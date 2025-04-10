package org.nsu.syspro.parprog.solution;

/**
 * Static collection of numerical parameters of my balancer
 */
public class Tuner {

    // ------------------ User thread settings ----------------
    /**
     * When local hotness reaches this limit, user thread tries to commit current method to global cache
     */
    public static final Integer localCommitThreshold = 1_000;

    /**
     * When local hotness reaches this limit,  user thread starts to wait L1 compilation of current method
     */
    public static final Integer interpretationLimit = 9_900;

    public static final Integer l1ExecutionLimit = 99_900;

    // ------------------ User thread settings ----------------

    public static final int l1CompilationThreshold = 6_000;


    private Tuner() {}
}
