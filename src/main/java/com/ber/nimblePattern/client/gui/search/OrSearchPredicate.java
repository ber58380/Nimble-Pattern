package com.ber.nimblePattern.client.gui.search;


import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Predicate;

final class OrSearchPredicate implements Predicate<ItemStack> {
    private final List<Predicate<ItemStack>> terms;

    private OrSearchPredicate(List<Predicate<ItemStack>> terms) {
        this.terms = terms;
    }

    public static Predicate<ItemStack> of(List<Predicate<ItemStack>> filters) {
        if (filters.isEmpty()) {
            return t -> false;
        }
        if (filters.size() == 1) {
            return filters.get(0);
        }
        return new OrSearchPredicate(filters);
    }

    @Override
    public boolean test(ItemStack entry) {
        for (var term : terms) {
            if (term.test(entry)) {
                return true;
            }
        }

        return false;
    }
}
