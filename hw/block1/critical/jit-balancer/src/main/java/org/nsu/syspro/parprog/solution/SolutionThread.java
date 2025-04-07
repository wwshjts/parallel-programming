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

    private final static int l1Bound = 5_000;
    private final static int l2Bound = 50_000;

    // TODO: add fields here!
    public SolutionThread(int compilationThreadBound, ExecutionEngine exec, CompilationEngine compiler, Runnable r) {
        super(compilationThreadBound, exec, compiler, r);
    }

    private final Map<Long, Long> hotness = new HashMap<>();

    @Override
    public ExecutionResult executeMethod(MethodID id) {
        final long methodID = id.id();
        final long hotLevel = hotness.getOrDefault(methodID, 0L);
        hotness.put(methodID, hotLevel + 1);

        if (hotLevel > l1Bound) {
            final CompiledMethod code = compiler.compile_l1(id);
            Thread compilationThread = new Thread(() -> {
                compiler.compile_l1(id);
                compiledMethods.putIfAbsent(methodID, code);
            });
            compilationThread.start();
        }

        if (compiledMethods.containsKey(methodID)) {
            return exec.execute(compiledMethods.get(methodID));
        }

        return exec.interpret(id);
    }

    // TODO: add methods
    // TODO: add inner classes
    // TODO: add utility classes in the same package
}