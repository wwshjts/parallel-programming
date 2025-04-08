package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.UserThread;
import org.nsu.syspro.parprog.external.*;

import java.util.*;
import java.util.concurrent.*;

public class SolutionThread extends UserThread {
    private final static Balancer balancer = new Balancer();


    private final static int l1Bound = 8_000;
    private final static int hardl1Bound = 9_000;
    private final static int l2Bound = 50_000;

    public SolutionThread(int compilationThreadBound, ExecutionEngine exec, CompilationEngine compiler, Runnable r) {
        super(compilationThreadBound, exec, compiler, r);
    }

    private final Map<Long, Long> hotness = new HashMap<>();

    @Override
    public ExecutionResult executeMethod(MethodID methodID) {
        final long id = methodID.id();
        final long hotLevel = hotness.getOrDefault(id, 0L);
        hotness.put(id, hotLevel + 1);

        CompiledMethod code = balancer.getCompiled(methodID);
        if (code != null) {
            return exec.execute(code);
        }

        if (hotLevel > hardl1Bound) {
            try {
                return exec.execute(balancer.getFuture(methodID).get());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        if (hotLevel > l1Bound) {
            balancer.makeRequest(new CompilationUnit(methodID, CompilationUnit.JitLevel.L1, compiler));
        }

        return exec.interpret(methodID);
    }

}