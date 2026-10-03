package com.ber.nimblePattern.crafting;

/** Dependency-free regression checks; run this main with assertions enabled or disabled. */
public final class LoopBatchSizingTest {
    public static void main(String[] args) {
        verify(10, 1, 1, 1, new long[] {1, 2, 4, 3});
        verify(10, 10, 1, 1, new long[] {10});
        verify(2, 1, 1, 8, new long[] {1, 1});
        verify(10, 4, 4, 4, new long[] {1, 2, 4, 3});
        if (LoopBatchSizing.next(10, 0, 1) != 0
                || LoopBatchSizing.next(10, 3, 4) != 0
                || LoopBatchSizing.next(0, 100, 1) != 0
                || LoopBatchSizing.next(Long.MAX_VALUE, Long.MAX_VALUE, 1) != Long.MAX_VALUE) {
            throw new AssertionError("Boundary batch size");
        }
        System.out.println("Loop batch regression checks passed");
    }

    private static void verify(long requested, long initial, long seed, long gain, long[] expected) {
        long remaining = requested;
        long available = initial;
        long consumedExternal = 0;
        for (long batch : expected) {
            if (LoopBatchSizing.next(remaining, available, seed) != batch) {
                throw new AssertionError("Unexpected physical batch size");
            }
            remaining -= batch;
            available += batch * gain;
            consumedExternal += batch;
        }
        if (remaining != 0 || available - initial != requested * gain || consumedExternal != requested) {
            throw new AssertionError("Bootstrap duplicated output or consumed extra inputs");
        }
    }
}
