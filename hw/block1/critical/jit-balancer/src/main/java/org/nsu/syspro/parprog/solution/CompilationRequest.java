package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.external.CompilationEngine;
import org.nsu.syspro.parprog.external.CompiledMethod;
import org.nsu.syspro.parprog.external.MethodID;

import java.util.concurrent.Callable;

public class CompilationRequest implements Callable<CompiledMethod> {
    private final MethodID methodID;
    private final Compiler.JitLevel level;
    private final CompilationEngine engine;

    public CompilationRequest(MethodID methodID, Compiler.JitLevel level, CompilationEngine engine) {
        this.methodID = methodID;
        this.level = level;
        this.engine = engine;
    }

    public long getID() {
        return methodID.id();
    }

    @Override
    public CompiledMethod call() {
        return engine.compile_l1(methodID);
    }
}
