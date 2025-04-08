package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.external.CompilationEngine;
import org.nsu.syspro.parprog.external.CompiledMethod;
import org.nsu.syspro.parprog.external.MethodID;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.*;

public class CompilationUnit {
    private static final ExecutorService compiler = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final MethodID methodID;
    private final CompilationEngine engine;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Condition isCompiled = lock.writeLock().newCondition();

    private JitLevel level = JitLevel.INTERPRETED;
    private CompiledMethod code     = null;
    private State state             = State.CREATED;

    private enum State {
        CREATED, ON_COMPILATION, COMPILED
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
    public void startCompilation() {
        lock.writeLock().lock();
        try {
            if (state == State.CREATED) {
                // System.out.println("Starting compile" + methodID.id());
                state = State.ON_COMPILATION;
                compiler.submit(new CompileTask(level)); // start compile asynchronously
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
        private JitLevel localLevel;

        public CompileTask(JitLevel localLevel) {
            this.localLevel = localLevel;
        }

        @Override
        public void run() {
            // No switch expressions :(
            CompiledMethod newCode;
            switch (localLevel)  {
                case INTERPRETED:
                    newCode = engine.compile_l1(methodID);
                    localLevel = JitLevel.L1;
                    break;
                case L1:
                    newCode = engine.compile_l2(methodID);
                    localLevel = JitLevel.L2;
                    break;
                default:
                    assert false : "Wrong state of state machine: Can't promote method from L2 level";
                    newCode = null; localLevel = null;
            }

            // Transition ON_COMPILATION -> COMPILED begins
            lock.writeLock().lock();
            try {
                assert state == State.ON_COMPILATION;
                code = newCode;
                level = localLevel;
                state = State.COMPILED;

                // System.out.println("Compiled: " + methodID.id());

                isCompiled.signalAll();
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

}
