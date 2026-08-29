package com.ber.nimblePattern.client.gui.search;

import appeng.api.stacks.AEItemKey;
import appeng.crafting.pattern.EncodedPatternItem;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.function.Predicate;

final class ItemIdSearchPredicate implements Predicate<ItemStack> {
    private final String term;

    public ItemIdSearchPredicate(String term) {
        this.term = term.toLowerCase();
    }

    @Override
    public boolean test(ItemStack stack) {
        ItemStack target = stack.getItem() instanceof EncodedPatternItem iep && !iep.getOutput(stack).isEmpty() ? iep.getOutput(stack) : ItemStack.EMPTY;
        var key = AEItemKey.of(target);
        if (key == null) {
            return false;
        }
        var id = key.getId().toString();
        return id.toLowerCase(Locale.ROOT).contains(term);
    }
}
