package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.external.CompilationEngine;
import org.nsu.syspro.parprog.external.CompiledMethod;
import org.nsu.syspro.parprog.external.MethodID;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.*;

/**
 * Compilation unit encapsulates the state of method in VM
 * It is guaranteed that level of optimised compilation increases while hotness is increases
 * After highest level of optimised compilation no observable changes happens in this class
 */
public class CompilationUnit {
    private static ExecutorService compilerWorkers;
    private static final ReadWriteLock wierdWorkersLock = new ReentrantReadWriteLock();

    private final MethodID methodID;
    private final CompilationEngine engine;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Condition isCompiled = lock.writeLock().newCondition();
    private long hotness = 0;
    private final int workersBound;

    private JitLevel level          = JitLevel.INTERPRETED;
    private CompiledMethod code     = null;
    private State state             = State.CREATED;

    private enum State {
        CREATED,        // Start state of state machine, visited exactly once
        ON_COMPILATION, // Method is in this state if and only if it scheduled on compilation
                        // Every scheduled compilation succeeds
                        // this state can't be visited more than twice
        COMPILED        // After visited this state predicate `isCompiled` always true
                        // this state can't be visited more than twice
    }

    /**
     * Increments hotness of method
     * Always write blocking :(
     */
    public void incrementHotness() {
        lock.writeLock().lock();
        try {
            JitLevel requiredLevel = hotnessToLevel(++hotness);
            if (requiredLevel.ordinal() > level.ordinal()) {
                startCompilation(requiredLevel);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }


    /**
     * If this method once returns true, then every call `getCode` method
     * succeeds and returns compiled version of method
     *
     * This method only reads data from CompilationUnit
     * @see #getCode
     * @return true if this unit can provide compiled version of method,
     * false otherwise
     */
    public boolean isCompiled() {
        lock.readLock().lock();
        try {
            if (level.ordinal() > JitLevel.INTERPRETED.ordinal()) {
                assert code != null;
                return true;
            }
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Subscribes current thread for waiting until compilation succeeds
     * @param requiredLevel Thread will be signalled when this compilation will be reached
     * @return code of compiled methods, which optimisation corresponds to required level
     */
    public CompiledMethod waitCompilation(JitLevel requiredLevel) {
        lock.writeLock().lock();
        try {
            if (requiredLevel.ordinal() > level.ordinal()) {
                assert state == State.ON_COMPILATION;

                while (state != State.COMPILED) isCompiled.await();

                assert requiredLevel.ordinal() == level.ordinal();
            }
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
            assert level.ordinal() > JitLevel.INTERPRETED.ordinal() : "Wrong state in getCode: " + state;
            assert code != null;

            return code;
        } finally {
            lock.readLock().unlock();
        }

    }

    public CompilationUnit(MethodID methodID, CompilationEngine engine, int workersBound) {
        this.methodID = methodID;
        this.engine = engine;
        this.workersBound = workersBound;
    }

    // Transition to CREATED -> ON_COMPILATION | COMPILED -> ON_COMPILATION
    // Happens under the lock but compilation is async
    private void startCompilation(JitLevel required) {
        if (state == State.CREATED || state == State.COMPILED) {
            assert required.ordinal() > level.ordinal();
            // System.out.println("Starting compile" + methodID.id());
            state = State.ON_COMPILATION;
            compilerWorkers.submit(new CompileTask(required)); // start compile asynchronously
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

                // signAll all is necessary here
                isCompiled.signalAll();
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    private static JitLevel hotnessToLevel(long hotness) {
        if (hotness < Tuner.l1CompilationThreshold) {
            return JitLevel.INTERPRETED;
        } else if (hotness < Tuner.l2CompilationThreshold) {
            return JitLevel.L1;
        }
        return JitLevel.L2;
    }

    // just ugly thing that needed because the number of workers isn't provided as static
    public static boolean isPoolInitialized() {
        wierdWorkersLock.readLock().lock();
        try {
            return compilerWorkers != null;
        } finally {
            wierdWorkersLock.readLock().unlock();
        }
    }

    public void initializePool() {
        wierdWorkersLock.writeLock().lock();
        try {
            if (compilerWorkers == null) {
                compilerWorkers = Executors.newFixedThreadPool(workersBound);
            }
        } finally {
            wierdWorkersLock.writeLock().unlock();
        }
    }
}
