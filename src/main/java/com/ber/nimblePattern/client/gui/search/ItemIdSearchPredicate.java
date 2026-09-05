package com.ber.nimblePattern.client.gui.search;

import appeng.api.stacks.AEKey;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.function.Predicate;

import static com.ber.nimblePattern.client.gui.search.UnwrapHelper.getKey;

final class ItemIdSearchPredicate implements Predicate<ItemStack> {
    private final String term;

    public ItemIdSearchPredicate(String term) {
        this.term = term.toLowerCase();
    }

    @Override
    public boolean test(ItemStack stack) {
        AEKey key = getKey(stack);
        if (key == null) {
            return false;
        }
        var id = key.getId().toString();
        return id.toLowerCase(Locale.ROOT).contains(term);
    }
}
