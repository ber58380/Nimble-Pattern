package com.ber.nimblePattern.crafting;

/** Counts whole operations without spending seeds that have not physically returned yet. */
public final class LoopBatchSizing {
    private LoopBatchSizing() {
    }

    public static long next(long remaining, long availableSeed, long seedPerOperation) {
        if (remaining < 0 || availableSeed < 0 || seedPerOperation <= 0) {
            throw new IllegalArgumentException("Invalid loop batch amounts");
        }
        return Math.min(remaining, availableSeed / seedPerOperation);
    }
}
