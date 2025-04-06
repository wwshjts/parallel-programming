package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.UserThread;
import org.nsu.syspro.parprog.external.*;

import java.util.*;

public class SolutionThread extends UserThread {
    private final Balancer balancer;

    public SolutionThread(int compilationThreadBound, ExecutionEngine exec, CompilationEngine compiler, Runnable r) {
        super(compilationThreadBound, exec, compiler, r);
        balancer = new Balancer(compilationThreadBound, compiler);
    }

    private final Map<Long, Long> hotness = new HashMap<>();
    private final Map<Long, CompiledMethod> compiledMethods = new HashMap<>();

    @Override
    public ExecutionResult executeMethod(MethodID methodID) {
        /* -- Start of critical fast-path
         * No global synchronisation used at this section
         */
        final long id = methodID.id();
        final long hotLevel = hotness.getOrDefault(id, 0L);
        hotness.put(id, hotLevel + 1);
        ExecutionResult result;

        if (compiledMethods.containsKey(id)) {
            result = exec.execute(compiledMethods.get(id));
        } else {
            result = exec.interpret(methodID);
        }

        // -- end of critical fast-path
        balancer.incrementHotness(methodID);

        if (hotLevel > Tuner.l1ExecutionLimit) {
            balancer.waitCompilation(methodID, CompilationUnit.JitLevel.L2);
        } else if (hotLevel > Tuner.interpretationLimit) {
            balancer.waitCompilation(methodID, CompilationUnit.JitLevel.L1);
        }

        // we can update compiledMethods every time because JitLevel monotonically increases
        // it is guranteed by CompilationUnit state-machine. Also, these units are unique
        // so every time we update map to the same method, or to method with
        // higher JitLevel
        if (balancer.isCompiled(methodID)) {
            compiledMethods.put(id, balancer.getCompiledMethod(methodID));
        }

        return result;
    }

}