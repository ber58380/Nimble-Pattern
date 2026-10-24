package com.ber.nimblePattern.client.gui.search;

import appeng.api.stacks.AEKey;
import com.ber.nimblePattern.compat.jecharacters.PinInHelper;
import com.ber.nimblePattern.pattern.NimblePatternTag;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.function.Predicate;

final class ConditionSearchPredicate implements Predicate<ItemStack> {
    private final String term;

    ConditionSearchPredicate(String term) {
        this.term = term.toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean test(ItemStack pattern) {
        if (term.isEmpty()) {
            return true;
        }
        AEKey condition = NimblePatternTag.getCondition(pattern);
        if (condition == null) {
            return false;
        }
        String lowerName = condition.getDisplayName().getString().toLowerCase(Locale.ROOT);
        return lowerName.contains(term) || PinInHelper.contains(lowerName, term);
    }
}
