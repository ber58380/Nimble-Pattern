package com.ber.nimblePattern.pattern;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import java.util.*;

/** Explicit provider matching, separate from pattern equality. Replaced on every AE provider rebuild. */
public final class ProviderPatternIndex {
    private static final Map<ICraftingProvider, Map<IPatternDetails, IPatternDetails>> PROVIDERS = new WeakHashMap<>();
    public static Object register(ICraftingProvider provider, List<IPatternDetails> exposed, List<IPatternDetails> compiled) {
        var mapping = new HashMap<IPatternDetails, IPatternDetails>();
        for (int i = 0; i < compiled.size(); i++) {
            var raw = exposed.get(i);
            mapping.put(compiled.get(i), raw instanceof NimbleEncodedPattern n ? n.getPattern() : raw);
        }
        PROVIDERS.put(provider, mapping);
        return mapping;
    }
    public static void unregister(ICraftingProvider provider, Object registration) {
        // A replacement state may already be mounted when an older state leaves the network.
        if (PROVIDERS.get(provider) == registration) PROVIDERS.remove(provider);
    }
    public static IPatternDetails nativePattern(ICraftingProvider provider, IPatternDetails pattern) {
        var patterns = PROVIDERS.get(provider);
        var nativePattern = patterns == null ? null : patterns.get(pattern);
        return nativePattern != null ? nativePattern : pattern instanceof NimbleEncodedPattern n ? n.getPattern() : pattern;
    }
    public static void clear() { PROVIDERS.clear(); }
    private ProviderPatternIndex() {}
}
