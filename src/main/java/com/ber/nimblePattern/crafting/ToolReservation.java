package com.ber.nimblePattern.crafting;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.KeyCounter;
import com.ber.nimblePattern.pattern.wrapper.NimbleCraftingPattern;

import java.util.Map;

public final class ToolReservation {
    private ToolReservation() {
    }

    /**
     * Calculate how many tools should be reserved for patterns. For several patterns using a same tool,
     * just reserve the maximum demand of tool in a single pattern is enough for all crafting.
     */
    public static KeyCounter getToolReservation(Map<IPatternDetails, Long> patterns) {
        var tools = new KeyCounter();
        patterns.forEach((pattern, count) -> {
            if (count > 0 && pattern instanceof NimbleCraftingPattern ncp) {
                for (var tool : ncp.getTools()) {
                    tools.set(tool.what(), Math.max(tools.get(tool.what()), tool.amount()));
                }
            }
        });
        return tools;
    }
}
