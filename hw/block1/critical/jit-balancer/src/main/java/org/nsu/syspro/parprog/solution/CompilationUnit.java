package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.external.CompilationEngine;
import org.nsu.syspro.parprog.external.CompiledMethod;
import org.nsu.syspro.parprog.external.MethodID;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.*;

public class CompilationUnit {
    private static final ExecutorService compiler = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final MethodID methodID;
    private final CompilationEngine engine;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Condition isCompiled = lock.writeLock().newCondition();
    private AtomicLong hotness = new AtomicLong(0);

    private JitLevel level = JitLevel.INTERPRETED;
    private CompiledMethod code     = null;
    private State state             = State.CREATED;

    private enum State {
        CREATED, ON_COMPILATION, COMPILED
    }

    // Potentially blocking
    public void incrementHotness() {
        JitLevel hotLevel = hotnessToLevel(hotness.addAndGet(1));

        if (hotLevel.ordinal() > level.ordinal()) {
            startCompilation(hotLevel);
        }

    }


    public boolean isCompiled() {
        lock.readLock().lock();
        try {
            return level.ordinal() > JitLevel.INTERPRETED.ordinal();
        } finally {
            lock.readLock().unlock();
        }
    }

    public CompiledMethod waitCompilation() {
        lock.writeLock().lock();
        try {
            assert state == State.ON_COMPILATION : "Expected state 'ON_COMPILATION', current:  " + state;

            while (state != State.COMPILED) isCompiled.await();

            return code;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public CompiledMethod getCode() {
        lock.readLock().lock();
        try {
            assert state == State.COMPILED : "Wrong state in getCode: " + state;
            assert code != null;

            return code;
        } finally {
            lock.readLock().unlock();
        }

    }

    public CompilationUnit(MethodID methodID, CompilationEngine engine) {
        this.methodID = methodID;
        this.engine = engine;
    }

    // Transition to CREATED -> ON_COMPILATION
    // Happens under the lock but compilation is async
    public void startCompilation(JitLevel required) {
        lock.writeLock().lock();
        try {
            if (state == State.CREATED) {
                assert required.ordinal() > level.ordinal();
                // System.out.println("Starting compile" + methodID.id());
                state = State.ON_COMPILATION;
                compiler.submit(new CompileTask(required)); // start compile asynchronously
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public enum JitLevel {
        INTERPRETED, L1, L2
    }


    // The 'function' that async compiles method, then
    // makes transition of state machine
    private class CompileTask implements Runnable {
        private JitLevel requiredLevel;

        public CompileTask(JitLevel localLevel) {
            this.requiredLevel = localLevel;
        }

        @Override
        public void run() {
            // No switch expressions :(
            CompiledMethod newCode;
            switch (requiredLevel)  {
                case L1:
                    newCode = engine.compile_l1(methodID);
                    break;
                case L2:
                    newCode = engine.compile_l2(methodID);
                    break;
                default:
                    assert false : "Wrong state of state machine: wrong required Jit: " + requiredLevel;
                    newCode = null; requiredLevel = null;
            }

            // Transition ON_COMPILATION -> COMPILED begins
            lock.writeLock().lock();
            try {
                assert state == State.ON_COMPILATION;
                code = newCode;
                level = requiredLevel;
                state = State.COMPILED;

                // System.out.println("Compiled: " + methodID.id());

                isCompiled.signalAll();
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    private static JitLevel hotnessToLevel(long hotness) {
        if (hotness < Tuner.l1CompilationThreshold) {
            return JitLevel.INTERPRETED;
        }
        return JitLevel.L1;
    }
}
