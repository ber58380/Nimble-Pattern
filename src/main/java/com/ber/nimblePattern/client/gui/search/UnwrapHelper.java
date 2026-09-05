package com.ber.nimblePattern.client.gui.search;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.EncodedPatternItem;
import net.minecraft.world.item.ItemStack;

public final class UnwrapHelper {
    // All the searching of the patterns should be targeted to the output of the pattern.
    // This class is used to get the AEKey and displayName of the output.
    // Because AE2 wraps the fluid, this class needs to do unwrapping operation.
    private UnwrapHelper() {
    }

    public static String getDisplayName(ItemStack pattern) {
        if (pattern.getItem() instanceof EncodedPatternItem iep) {
            var output = iep.getOutput(pattern);
            if (!output.isEmpty()) {
                var unwrapped = GenericStack.unwrapItemStack(output);
                if (unwrapped != null) {
                    return unwrapped.what().getDisplayName().getString();
                }
                var itemKey = AEItemKey.of(output);
                if (itemKey != null) {
                    return itemKey.getDisplayName().getString();
                }
                return output.getHoverName().getString();
            }
        }
        return pattern.getHoverName().getString();
    }

    public static AEKey getKey(ItemStack pattern) {
        if (pattern.getItem() instanceof EncodedPatternItem iep) {
            var output = iep.getOutput(pattern);
            if (!output.isEmpty()) {
                var unwrapped = GenericStack.unwrapItemStack(output);
                if (unwrapped != null) {
                    return unwrapped.what();
                }
                return AEItemKey.of(output);
            }
        }
        return null;
    }
}
