package org.nsu.syspro.parprog.solution;

/**
 * Static collection of numerical parameters of my balancer
 */
public class Tuner {

    // ------------------ User thread settings ----------------

    /**
     * When local hotness reaches this limit,  user thread starts to wait L1 compilation of current method
     */
    public static final Integer interpretationLimit = 9_900;

    /**
     * When local hotness reaches this limit, user thread starts to wait L2 compilation of current method
     */
    public static final Integer l1ExecutionLimit = 99_900;

    // ------------------ User thread settings ----------------

    /**
     * When global hotness of method reaches this threshold, then it is scheduled to l1 compilation
     */
    public static final int l1CompilationThreshold = 6_000;

    /**
     * When global hotness of method reaches this threshold, then it is scheduled to l2 compilation
     */
    public static final int l2CompilationThreshold = 90_000;

    // ------------------ Check invariants ----------------
    public static boolean checkInvariants() {
        assert l1CompilationThreshold < interpretationLimit;
        assert l2CompilationThreshold < l1ExecutionLimit;
        return true;
    }

    private final static boolean _inv = checkInvariants();

    // this is a 'static' collection of constants, so no instances should be created
    private Tuner() {}
}
