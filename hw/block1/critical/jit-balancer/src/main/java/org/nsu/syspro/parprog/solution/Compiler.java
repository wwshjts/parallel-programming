package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.external.CompiledMethod;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Manges compilation resources
 */
public class Compiler {
    private final ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public static enum JitLevel {
        L1, L2
    }

    public Future<CompiledMethod> compile(CompilationRequest request) {
        return pool.submit(request);
    }

}
