package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.UserThread;
import org.nsu.syspro.parprog.external.*;

import java.util.*;

public class SolutionThread extends UserThread {
    private final Balancer balancer;


    public SolutionThread(int compilationThreadBound, ExecutionEngine exec, CompilationEngine compiler, Runnable r) {
        super(compilationThreadBound, exec, compiler, r);
        balancer = new Balancer(compiler);
    }

    private final Map<Long, Long> hotness = new HashMap<>();

    @Override
    public ExecutionResult executeMethod(MethodID methodID) {
        final long id = methodID.id();
        final long hotLevel = hotness.getOrDefault(id, 0L);
        hotness.put(id, hotLevel + 1);
        ExecutionResult result;

        if (balancer.isCompiled(methodID)) {
            result = exec.execute(balancer.getCompiledMethod(methodID));
        } else {
            result = exec.interpret(methodID);

            if (hotLevel > Tuner.interpretationLimit) {
                balancer.waitCompilation(methodID);
            }
        }

        balancer.incrementHotness(methodID);

        return result;
    }

}