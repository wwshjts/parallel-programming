package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.UserThread;
import org.nsu.syspro.parprog.external.*;

import java.util.*;

public class SolutionThread extends UserThread {
    private final Balancer balancer;

    private final static int l1Bound = 6_000;
    private final static int hardl1Bound = 9_000;
    private final static int l2Bound = 50_000;

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

        if (balancer.isCompiled(methodID)) {
            return exec.execute(balancer.getCompiledMethod(methodID));
        }

        if (hotLevel > hardl1Bound) {
            return exec.execute(balancer.waitCompilation(methodID));
        }

        if (hotLevel > l1Bound) {
            balancer.scheduleCompilation(methodID);
        }




        return exec.interpret(methodID);
    }

}