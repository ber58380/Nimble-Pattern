package com.ber.nimblePattern.compat.gtlcore;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import com.ber.nimblePattern.pattern.PatternMapping;
import com.ber.nimblePattern.pattern.wrapper.NimbleProcessingPattern;

import java.lang.reflect.Method;

/**
 * GTL's graph dispatch context must see the provider's original pattern, not Nimble's planning
 * wrapper (providers may use identity or concrete pattern types when checking acceptance). A loop
 * composite is not a real provider pattern, so a GT pattern buffer correctly rejects it even though
 * the virtual recipe is valid. Let the composite through that preflight and execute it through
 * Nimble's loop controller instead.
 */
public final class GtlGraphDispatchAccess {
    private static Method allowedMethod;
    private static Method capacityMethod;
    private static boolean methodsResolved;
    private static boolean failureReported;

    private GtlGraphDispatchAccess() {
    }

    public static boolean allowed(ICraftingProvider provider, IPatternDetails pattern) {
        if (isLoopComposite(pattern)) {
            return true;
        }
        Method method = allowedMethod();
        if (method == null) {
            return false;
        }
        try {
            return (Boolean) method.invoke(null, provider, PatternMapping.getOriginalPattern(provider, pattern));
        } catch (ReflectiveOperationException e) {
            reportFailure(e);
            return false;
        }
    }

    public static long capacity(ICraftingProvider provider, IPatternDetails pattern, long requested) {
        if (isLoopComposite(pattern)) {
            return requested;
        }
        Method method = capacityMethod();
        if (method == null) {
            return 0;
        }
        try {
            return (Long) method.invoke(null, provider, PatternMapping.getOriginalPattern(provider, pattern), requested);
        } catch (ReflectiveOperationException e) {
            reportFailure(e);
            return 0;
        }
    }

    private static boolean isLoopComposite(IPatternDetails pattern) {
        return pattern instanceof NimbleProcessingPattern nimble && nimble.isLoopComposite();
    }

    private static synchronized Method allowedMethod() {
        resolveMethods();
        return allowedMethod;
    }

    private static synchronized Method capacityMethod() {
        resolveMethods();
        return capacityMethod;
    }

    private static void resolveMethods() {
        if (methodsResolved) {
            return;
        }
        methodsResolved = true;
        try {
            Class<?> context = Class.forName("org.gtlcore.gtlcore.integration.ae2.graph.GraphDispatchContext");
            allowedMethod = context.getMethod("allowed", ICraftingProvider.class, IPatternDetails.class);
            capacityMethod = context.getMethod("capacity", ICraftingProvider.class, IPatternDetails.class, long.class);
        } catch (ReflectiveOperationException | LinkageError e) {
            reportFailure(e);
        }
    }

    private static synchronized void reportFailure(Throwable failure) {
        if (!failureReported) {
            failureReported = true;
            System.getLogger("nimble_pattern").log(System.Logger.Level.WARNING,
                    "GTL graph provider preflight failed; dispatch is blocked before transferring inputs", failure);
        }
    }
}
