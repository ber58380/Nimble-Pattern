package com.ber.nimblePattern.client.gui.search;

import appeng.api.stacks.AEKey;
import appeng.util.Platform;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.function.Predicate;

import static com.ber.nimblePattern.client.gui.search.UnwrapHelper.getKey;

final class ModSearchPredicate implements Predicate<ItemStack> {
    private final String term;

    public ModSearchPredicate(String term) {
        this.term = term.toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean test(ItemStack stack) {
        AEKey key = getKey(stack);
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
