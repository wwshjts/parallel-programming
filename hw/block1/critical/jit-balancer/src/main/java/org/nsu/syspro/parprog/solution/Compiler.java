package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.solution.CompilationUnit.CompilationResult;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Manges compilation resources
 */
public class Compiler {
    private final ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

}
