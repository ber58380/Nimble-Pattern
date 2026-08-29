package com.ber.nimblePattern.client.gui.search;

import appeng.crafting.pattern.EncodedPatternItem;
import com.ber.nimblePattern.compat.jecharacters.PinInHelper;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.function.Predicate;

final class NameSearchPredicate implements Predicate<ItemStack> {
    private final String term;

    public NameSearchPredicate(String term) {
        this.term = term.toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean test(ItemStack stack) {
        var output = stack.getItem() instanceof EncodedPatternItem iep ? iep.getOutput(stack) : ItemStack.EMPTY;
        ItemStack target = !output.isEmpty() ? output : stack;
        var name = target.getHoverName().getString().toLowerCase(Locale.ROOT);
        return name.contains(term) || PinInHelper.contains(name, term);
    }
}
