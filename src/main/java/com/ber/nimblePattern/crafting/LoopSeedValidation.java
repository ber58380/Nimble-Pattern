package com.ber.nimblePattern.crafting;

import appeng.api.config.Actionable;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.crafting.CraftingPlan;
import com.ber.nimblePattern.item.storage.LoopStorageCellAccess;
import com.ber.nimblePattern.pattern.wrapper.NimbleProcessingPattern;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

/**
 * Startup seeds are borrowed from a specific cell, not ordinary recipe inputs.
 */
public final class LoopSeedValidation {
    private LoopSeedValidation() {
    }

    private record Seed(UUID cell, AEKey key) {
    }

    public static KeyCounter missing(ICraftingPlan plan, MEStorage storage) {
        var loops = new HashSet<UUID>();
        var required = new HashMap<Seed, Long>();
        plan.patternTimes().forEach((details, times) -> {
            if (times > 0 && details instanceof NimbleProcessingPattern pattern && pattern.isLoopComposite()) {
                var data = pattern.getLoopData();
                // Repetitions reuse a seed; different loops retain separate reservations until job completion.
                if (loops.add(data.loopId())) {
                    required.merge(new Seed(data.storageCellId(), data.entry().what()), data.seedAmount(),
                            (a, b) -> a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b);
                }
            }
        });
        var missing = new KeyCounter();
        required.forEach((seed, amount) -> {
            var cell = storage == null ? null : LoopStorageCellAccess.findMounted(storage, seed.cell());
            long available = cell == null ? 0 : cell.extractForLoop(seed.key(), amount, Actionable.SIMULATE);
            if (available < amount) {
                missing.add(seed.key(), amount - available);
            }
        });
        return missing;
    }

    public static ICraftingPlan validate(ICraftingPlan plan, MEStorage storage) {
        var seeds = missing(plan, storage);
        if (seeds.isEmpty()) {
            return plan;
        }
        var missing = new KeyCounter();
        missing.addAll(plan.missingItems());
        missing.addAll(seeds);
        return new CraftingPlan(plan.finalOutput(), plan.bytes(), true, plan.multiplePaths(),
                plan.usedItems(), plan.emittedItems(), missing, plan.patternTimes());
    }
}
