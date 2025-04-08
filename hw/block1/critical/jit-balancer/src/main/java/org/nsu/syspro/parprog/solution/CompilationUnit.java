package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.external.CompilationEngine;
import org.nsu.syspro.parprog.external.CompiledMethod;
import org.nsu.syspro.parprog.external.MethodID;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CompilationUnit {
    private static final ExecutorService compiler = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final MethodID methodID;
    private final CompilationEngine engine;

    private final Lock lock = new ReentrantLock();
    private final Condition isCompiled = lock.newCondition();

    private JitLevel level = JitLevel.INTERPRETED;
    private CompiledMethod code     = null;
    private State state             = State.CREATED;

    private CompilationResult concurrentState;

    private static enum State {
        CREATED, ON_COMPILATION, COMPILED
    }

    public CompilationUnit(MethodID methodID, JitLevel level, CompilationEngine engine) {
        this.methodID = methodID;
        this.level = level;
        this.engine = engine;
    }

    public long getID() {
        return methodID.id();
    }

    // Transition to CREATED -> ON_COMPILATION
    // Happens under the lock but compilation is async
    public void startCompilation() {
        lock.lock();
        try {
            if (state == State.CREATED) {
                compiler.submit(new CompileTask()); // start compile asynchronously
                state = State.ON_COMPILATION;
            }
        } finally {
            lock.unlock();
        }
    }

    public static enum JitLevel {
        INTERPRETED, L1, L2
    }


    public static class CompilationResult {
        private final CompiledMethod code;
        private final JitLevel level;

        private CompilationResult(CompiledMethod code, JitLevel level) {
            this.code = code;
            this.level = level;
        }

        public static CompilationResult of(CompiledMethod code, JitLevel level) {
           return new CompilationResult(code, level);
        }

        public JitLevel getLevel() {
            return level;
        }

        public CompiledMethod getCode() {
            return code;
        }
    }

    // The 'function' that async compiles method, then
    // makes transition of state machine
    private class CompileTask implements Runnable {
        @Override
        public void run() {
            // No switch expressions :(
            CompiledMethod newCode;
            JitLevel newLevel;
            switch (level)  {
                case INTERPRETED:
                    newCode = engine.compile_l1(methodID);
                    newLevel = JitLevel.L1;
                    break;
                case L1:
                    newCode = engine.compile_l2(methodID);
                    newLevel = JitLevel.L2;
                    break;
                default:
                    assert false : "Wrong state of state machine: Can't promote method from L2 level";
                    newCode = null; newLevel = null;
            }

            // Transition ON_COMPILATION -> COMPILED begins
            lock.lock();
            try {
                assert state == State.ON_COMPILATION;
                code = newCode;
                level = newLevel;
                state = State.COMPILED;
            } finally {
                lock.unlock();
            }
        }
    }

}
