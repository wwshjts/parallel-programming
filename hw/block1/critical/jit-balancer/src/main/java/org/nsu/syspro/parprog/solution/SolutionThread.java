package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.UserThread;
import org.nsu.syspro.parprog.external.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SolutionThread extends UserThread {
    /**
     * Shared state of all user threads
     */
    //private final static HashMap<Long, CompiledMethod> compiledMethods = new HashMap<>();
    private final static Map<Long, CompiledMethod> compiledMethods = new ReadMostMap<>();
    private final static ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final static int l1Bound = 5_000;
    private final static int l2Bound = 50_000;

    // TODO: add fields here!
    public SolutionThread(int compilationThreadBound, ExecutionEngine exec, CompilationEngine compiler, Runnable r) {
        super(compilationThreadBound, exec, compiler, r);
    }

    private final Map<Long, Long> hotness = new HashMap<>();

    @Override
    public ExecutionResult executeMethod(MethodID methodID) {
        final long id = methodID.id();
        final long hotLevel = hotness.getOrDefault(id, 0L);
        hotness.put(id, hotLevel + 1);

        if (compiledMethods.containsKey(id)) {
            return exec.execute(compiledMethods.get(id));
        }

        if (hotLevel > l1Bound) {
            pool.submit(makeRequest(methodID));
        }

        return exec.interpret(methodID);
    }

    private static enum JitLevel {
        L1, L2
    }

    private CompilationRequest makeRequest(MethodID methodID) {
        return new CompilationRequest(methodID,JitLevel.L1);
    }

    private class CompilationRequest implements Runnable {
        private final MethodID methodID;
        private final JitLevel level;

        private CompilationRequest(MethodID methodID, JitLevel level) {
            this.methodID = methodID;
            this.level = level;
        }

        @Override
        public void run() {
            CompiledMethod code = compiler.compile_l1(methodID); // <- quants lost
            compiledMethods.putIfAbsent(methodID.id(), code);
        }
    }
}