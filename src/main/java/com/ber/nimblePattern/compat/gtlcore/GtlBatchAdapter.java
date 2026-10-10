package com.ber.nimblePattern.compat.gtlcore;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import com.ber.nimblePattern.pattern.PatternMapping;

import java.lang.reflect.Method;
import java.util.Optional;

public final class GtlBatchAdapter {
    private GtlBatchAdapter() {
    }

    private static final ClassValue<Optional<Method>> MAXIMUM = new ClassValue<>() {
        @Override
        protected Optional<Method> computeValue(Class<?> type) {
            try {
                return Optional.of(type.getMethod("gtlcore$getMaxPatternOperations", IPatternDetails.class, long.class));
            } catch (NoSuchMethodException ignored) {
                return Optional.empty();
            }
        }
    };

    public static long maximum(ICraftingProvider provider, IPatternDetails pattern, long remaining) {
        var method = MAXIMUM.get(provider.getClass());
        if (method.isEmpty()) return 1;
        try {
            return ((Number) method.get().invoke(provider, PatternMapping.getOriginalPattern(provider, pattern), remaining)).longValue();
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return 1;
        }
    }
}
