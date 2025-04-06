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
    private final static HashMap<Long, CompiledMethod> compiledMethods = new HashMap<>();

    private final static Lock setLock = new ReentrantLock();

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

        setLock.lock();
        try {
            if (compiledMethods.containsKey(methodID)) {
                return exec.execute(compiledMethods.get(methodID));
            }
        } finally {
            setLock.unlock();
        }

        if (hotLevel > 9_000) {
            final CompiledMethod code = compiler.compile_l1(id);
            setLock.lock();
            try {
                compiledMethods.putIfAbsent(methodID, code);
            } finally {
                setLock.unlock();
            }
        }


        return exec.interpret(id);
    }

    // TODO: add methods
    // TODO: add inner classes
    // TODO: add utility classes in the same package
}