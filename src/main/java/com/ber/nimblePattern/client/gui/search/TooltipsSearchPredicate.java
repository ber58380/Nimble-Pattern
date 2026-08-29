package com.ber.nimblePattern.client.gui.search;

import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.core.AEConfig;
import appeng.crafting.pattern.EncodedPatternItem;
import appeng.util.Platform;
import com.ber.nimblePattern.compat.jecharacters.PinInHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

final class TooltipsSearchPredicate implements Predicate<ItemStack> {
    private final String tooltip;
    private final Map<AEKey, String> tooltipCache;

    public TooltipsSearchPredicate(String tooltip, Map<AEKey, String> tooltipCache) {
        this.tooltip = normalize(tooltip.toLowerCase());
        this.tooltipCache = tooltipCache;
    }

    @Override
    public boolean test(ItemStack stack) {
        ItemStack target = stack.getItem() instanceof EncodedPatternItem iep && !iep.getOutput(stack).isEmpty() ? iep.getOutput(stack) : ItemStack.EMPTY;
        var key = AEItemKey.of(target);
        if (key == null) {
            return false;
        }
        var tooltipText = getTooltipText(key);
        return tooltipText.contains(tooltip) || PinInHelper.contains(tooltipText, tooltip);
    }

    /**
     * Gets the concatenated text of a keys tooltip for search purposes.
     */
    private String getTooltipText(AEKey what) {
        return tooltipCache.computeIfAbsent(what, key -> {
            var lines = AEKeyRendering.getTooltip(key);

            var tooltipText = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                var line = lines.get(i);

                // Process last line and skip mod name if our heuristic detects it
                if (i > 0 && i >= lines.size() - 1 && !AEConfig.instance().isSearchModNameInTooltips()) {
                    var text = line.getString();
                    boolean hadFormatting = false;
                    if (text.indexOf(ChatFormatting.PREFIX_CODE) != -1) {
                        text = ChatFormatting.stripFormatting(text);
                        hadFormatting = true;
                    } else {
                        hadFormatting = !line.getStyle().isEmpty();
                    }

                    if (!hadFormatting || !Objects.equals(text, Platform.getModName(what.getModId()))) {
                        tooltipText.append('\n').append(text);
                    }
                } else {
                    if (i > 0) {
                        tooltipText.append('\n');
                    }
                    line.visit(text -> {
                        if (text.indexOf(ChatFormatting.PREFIX_CODE) != -1) {
                            text = ChatFormatting.stripFormatting(text);
                        }
                        tooltipText.append(text);
                        return Optional.empty();
                    });
                }
            }

            return normalize(tooltipText.toString());
        });
    }

    private static String normalize(String input) {
        return input.toLowerCase().replace(" ", "");
    }
}
