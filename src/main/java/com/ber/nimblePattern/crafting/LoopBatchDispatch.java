package com.ber.nimblePattern.crafting;

import appeng.api.crafting.IPatternDetails;

/** Transfers GTL's actual dispatch count to the CPU's subsequent push, including zero-input loops. */
public final class LoopBatchDispatch {
    private static final ThreadLocal<Dispatch> CURRENT = new ThreadLocal<>();

    private LoopBatchDispatch() {
    }

    public static void record(IPatternDetails pattern, long operations) {
        CURRENT.set(new Dispatch(pattern, operations));
    }

    public static long take(IPatternDetails pattern) {
        var dispatch = CURRENT.get();
        CURRENT.remove();
        return dispatch != null && dispatch.pattern == pattern ? dispatch.operations : 1;
    }

    public static void clear() {
        CURRENT.remove();
    }

    private record Dispatch(IPatternDetails pattern, long operations) {
    }
}
