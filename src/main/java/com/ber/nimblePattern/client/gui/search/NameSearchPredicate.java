package com.ber.nimblePattern.client.gui.search;

import com.ber.nimblePattern.compat.jecharacters.PinInHelper;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.function.Predicate;

import static com.ber.nimblePattern.client.gui.search.UnwrapHelper.getDisplayName;

final class NameSearchPredicate implements Predicate<ItemStack> {
    private final String term;

    public NameSearchPredicate(String term) {
        this.term = term.toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean test(ItemStack stack) {
        String name = getDisplayName(stack).toLowerCase(Locale.ROOT);
        return name.contains(term) || PinInHelper.contains(name, term);
    }
}
