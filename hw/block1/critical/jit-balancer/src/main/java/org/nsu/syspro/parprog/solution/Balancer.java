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

    public void scheduleCompilation(MethodID methodID) {
        var unit = new CompilationUnit(methodID, engine);
        units.putIfAbsent(methodID.id(), unit);
        unit = units.get(methodID.id());
        unit.startCompilation();
    }

    public CompiledMethod waitCompilation(MethodID methodID) {
        long id = methodID.id();
        assert units.containsKey(id);

        return units.get(id).waitCompilation();
    }

}
