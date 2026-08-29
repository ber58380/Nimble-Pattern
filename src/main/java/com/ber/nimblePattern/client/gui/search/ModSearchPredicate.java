package com.ber.nimblePattern.client.gui.search;

import appeng.api.stacks.AEItemKey;
import appeng.crafting.pattern.EncodedPatternItem;
import appeng.util.Platform;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.function.Predicate;

final class ModSearchPredicate implements Predicate<ItemStack> {
    private final String term;

    public ModSearchPredicate(String term) {
        this.term = term.toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean test(ItemStack stack) {
        ItemStack target = stack.getItem() instanceof EncodedPatternItem iep && !iep.getOutput(stack).isEmpty() ? iep.getOutput(stack) : ItemStack.EMPTY;
        var key = AEItemKey.of(target);
        if (key == null) {
            return false;
        }
        var modId = key.getModId();
        if (modId != null) {
            if (modId.contains(term)) {
                return true;
            }
            String modName = Platform.getModName(modId);
            return modName != null && modName.toLowerCase(Locale.ROOT).contains(term);
        }
        return false;
    }

}
