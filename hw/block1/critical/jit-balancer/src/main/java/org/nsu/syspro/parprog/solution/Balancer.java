package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.external.CompilationEngine;
import org.nsu.syspro.parprog.external.MethodID;
import org.nsu.syspro.parprog.solution.CompilationUnit.CompilationResult;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Manges compilation resources
 */
public class Compiler {
    private final ConcurrentHashMap<Long, CompilationUnit> units = new ConcurrentHashMap<>();
    private final CompilationEngine engine;

    public Compiler(CompilationEngine engine) {
        this.engine = engine;
    }

    public boolean isCompiled() {

    }

    public void scheduleCompilation(MethodID methodID, CompilationEngine engine) {
       units.putIfAbsent(methodID.id(), new CompilationUnit(methodID, engine));
    }

}
