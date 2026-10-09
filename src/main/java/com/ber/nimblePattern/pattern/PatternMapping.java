package com.ber.nimblePattern.pattern;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Record all pattern providers and all mappings of original patterns with wrapped patterns.
 */
public class PatternMapping {
    private static final Map<ICraftingProvider, Map<IPatternDetails, IPatternDetails>> PROVIDERS = new WeakHashMap<>();

    private PatternMapping() {
    }

    public static Object register(ICraftingProvider provider, List<IPatternDetails> original, List<IPatternDetails> wrapped) {
        var mapping = new HashMap<IPatternDetails, IPatternDetails>();
        for (int i = 0; i < wrapped.size(); i++) {
            var raw = original.get(i);
            mapping.put(wrapped.get(i), raw instanceof NimblePatternWrapper<?> npw ? npw.getPattern() : raw);
        }
        PROVIDERS.put(provider, mapping);
        return mapping;
    }

    public static void unregister(ICraftingProvider provider, Object registration) {
        if (PROVIDERS.get(provider) == registration) {
            PROVIDERS.remove(provider);
        }
    }

    public static IPatternDetails getOriginalPattern(ICraftingProvider provider, IPatternDetails pattern) {
        var patterns = PROVIDERS.get(provider);
        var originalPattern = patterns == null ? null : patterns.get(pattern);
        return originalPattern != null ? originalPattern : pattern instanceof NimblePatternWrapper<?> npw ? npw.getPattern() : pattern;
    }

    public static void clear() {
        PROVIDERS.clear();
    }
}
