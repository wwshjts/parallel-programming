package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.external.CompilationEngine;
import org.nsu.syspro.parprog.external.CompiledMethod;
import org.nsu.syspro.parprog.external.MethodID;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Manges compilation resources
 */
public class Balancer {
    private final static ConcurrentHashMap<Long, CompilationUnit> units = new ConcurrentHashMap<>();
    private final CompilationEngine engine;

    public Balancer(CompilationEngine engine) {
        this.engine = engine;
    }

    public boolean isCompiled(MethodID methodID) {
        long id = methodID.id();
        if (units.containsKey(id)) {
            return units.get(id).isCompiled();
        }
        return false;
    }

    public CompiledMethod getCompiledMethod(MethodID methodID) {
        long id = methodID.id();
        assert units.containsKey(methodID.id());
        return units.get(id).getCode();
    }

    // might be blocking
    public void incrementHotness(MethodID methodID) {
        units.computeIfAbsent(methodID.id(), id -> new CompilationUnit(methodID, engine)).incrementHotness();
    }


    public CompiledMethod waitCompilation(MethodID methodID, CompilationUnit.JitLevel requiredLevel) {
        long id = methodID.id();
        assert units.containsKey(id);

        return units.get(id).waitCompilation(requiredLevel);
    }

}
