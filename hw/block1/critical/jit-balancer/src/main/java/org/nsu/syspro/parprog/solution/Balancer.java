package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.external.CompilationEngine;
import org.nsu.syspro.parprog.external.CompiledMethod;
import org.nsu.syspro.parprog.external.MethodID;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The class is the intermediary between user threads and compilation units
 */
public class Balancer {
    private final static ConcurrentHashMap<Long, CompilationUnit> units = new ConcurrentHashMap<>();
    private final CompilationEngine engine;
    private final int threadBound;


    public Balancer(int threadBound, CompilationEngine engine) {
        this.engine = engine;
        this.threadBound = threadBound;
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


    public void incrementHotness(MethodID methodID) {
        var unit = units.computeIfAbsent(methodID.id(), id -> new CompilationUnit(methodID, engine, threadBound));
        unit.incrementHotness();
        if (!CompilationUnit.isPoolInitialized()) {
            unit.initializePool();
        }
    }


    public CompiledMethod waitCompilation(MethodID methodID, CompilationUnit.JitLevel requiredLevel) {
        long id = methodID.id();
        assert units.containsKey(id);

        return units.get(id).waitCompilation(requiredLevel);
    }

}
