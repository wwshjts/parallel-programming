package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.external.CompilationEngine;
import org.nsu.syspro.parprog.external.CompiledMethod;
import org.nsu.syspro.parprog.external.MethodID;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages compilation requests
 * Keeps invariant - there is no two compilation request with the same method ID that compiles
 */
public class Balancer {
    private final static Compiler compiler = new Compiler();

    private final static Map<Long, Future<CompiledMethod>> scheduled = new HashMap<>();
    private final static Map<Long,  CompilationRequest>  requests = new HashMap<>();
    private final static ExecutorService pool = Executors.newCachedThreadPool();

    public CompiledMethod getCompiled(MethodID methodID) {
        synchronized (scheduled) {
            long id = methodID.id();
            if (scheduled.containsKey(id)) {
               var future = scheduled.get(id);
               if (future.isDone()) {
                   try {
                       return future.get();
                   } catch (ExecutionException | InterruptedException e) {
                       throw new RuntimeException(e);
                   }
               }
            }
            return null;
        }
    }

    public Future<CompiledMethod> getFuture(MethodID methodID) {
        synchronized (scheduled) {
            assert scheduled.containsKey(methodID.id());

            return scheduled.get(methodID.id());
        }
    }

    public void makeRequest(CompilationRequest request) {
        pool.submit(Request.of(request));
    }

    private static class Request implements Runnable {
        private final CompilationRequest request;

        private Request(CompilationRequest request) {
            this.request = request;
        }

        public static Request of(CompilationRequest request) {
            return new Request(request);
        }

        public long getId() {
            return request.getID();
        }

        @Override
        public void run() {
            synchronized (scheduled) { // <- so those things runs close to sequence evaluation
                if (!scheduled.containsKey(request.getID())) {
                    scheduled.put(request.getID(), compiler.compile(request));
                }
            }
        }
    }
}