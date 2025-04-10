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
    private final Map<Long, Long> compiledMethods = new HashMap<>();

    @Override
    public ExecutionResult executeMethod(MethodID methodID) {
        /* -- Start of critical fast-path
         * No global synchronisation used at this section
         *
         */
        final long id = methodID.id();
        final long hotLevel = hotness.getOrDefault(id, 0L);
        hotness.put(id, hotLevel + 1);
        ExecutionResult result;

        if (balancer.isCompiled(methodID)) {
            result = exec.execute(balancer.getCompiledMethod(methodID));
        } else {
            result = exec.interpret(methodID);
        }

        balancer.incrementHotness(methodID);

        if (hotLevel > Tuner.l1ExecutionLimit) {
            balancer.waitCompilation(methodID, CompilationUnit.JitLevel.L2);
        } else if (hotLevel > Tuner.interpretationLimit) {
            balancer.waitCompilation(methodID, CompilationUnit.JitLevel.L1);
        }

        return result;
    }

}