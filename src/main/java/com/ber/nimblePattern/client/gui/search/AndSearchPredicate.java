package com.ber.nimblePattern.client.gui.search;


import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Predicate;

final class AndSearchPredicate implements Predicate<ItemStack> {
    private final List<Predicate<ItemStack>> terms;

    private AndSearchPredicate(List<Predicate<ItemStack>> terms) {
        this.terms = terms;
    }

    public static Predicate<ItemStack> of(List<Predicate<ItemStack>> predicates) {
        if (predicates.isEmpty()) {
            return t -> true;
        }
        if (predicates.size() == 1) {
            return predicates.get(0);
        }
        return new AndSearchPredicate(predicates);
    }

    @Override
    public boolean test(ItemStack entry) {
        for (var term : terms) {
            if (!term.test(entry)) {
                return false;
            }
        }

        return true;
    }
}