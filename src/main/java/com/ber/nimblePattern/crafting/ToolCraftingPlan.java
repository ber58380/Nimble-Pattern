package com.ber.nimblePattern.crafting;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.CraftingPlan;
import com.ber.nimblePattern.pattern.NimbleAssemblerPattern;
import java.util.Map;

/** A tool is reserved once per job, never multiplied by the number of crafts. */
public final class ToolCraftingPlan {
    private ToolCraftingPlan() {}

    public static KeyCounter reservations(Map<IPatternDetails, Long> patterns) {
        var tools = new KeyCounter();
        patterns.forEach((pattern, count) -> {
            if (count > 0 && pattern instanceof NimbleAssemblerPattern toolPattern) {
                for (var tool : toolPattern.getTools()) {
                    tools.add(tool.what(), Math.max(0, tool.amount() - tools.get(tool.what())));
                }
            }
        });
        return tools;
    }

    public static CraftingPlan reserve(CraftingPlan plan, KeyCounter snapshot) {
        var tools = reservations(plan.patternTimes());
        if (tools.isEmpty()) return plan;
        var used = new KeyCounter();
        used.addAll(plan.usedItems());
        var missing = new KeyCounter();
        missing.addAll(plan.missingItems());
        for (var tool : tools) {
            long total = Math.addExact(used.get(tool.getKey()), tool.getLongValue());
            used.add(tool.getKey(), tool.getLongValue());
            long deficit = Math.max(0, total - snapshot.get(tool.getKey()));
            // The ordinary plan may already contain a deficit for this key.
            missing.add(tool.getKey(), Math.max(0, deficit - missing.get(tool.getKey())));
        }
        missing.removeZeros();
        return new CraftingPlan(plan.finalOutput(), Math.addExact(plan.bytes(), tools.size() * 8L),
                plan.simulation() || !missing.isEmpty(), plan.multiplePaths(),
                used, plan.emittedItems(), missing, plan.patternTimes());
    }
}
