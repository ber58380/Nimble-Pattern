package com.ber.nimblePattern.client.gui.search;

import com.ber.nimblePattern.pattern.NimblePatternTag;
import com.ber.nimblePattern.pattern.UpdateState;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.function.Predicate;

import static com.ber.nimblePattern.pattern.UpdateState.*;

final class StatusSearchPredicate implements Predicate<ItemStack> {
    private final UpdateState state;

    public StatusSearchPredicate(String term) {
        switch (term.toUpperCase(Locale.ROOT).trim()) {
            case "UNTRACKED", "0", "":
                this.state = UNTRACKED;
                break;
            case "LATEST", "1":
                this.state = LATEST;
                break;
            case "UPDATE", "2":
                this.state = UPDATE;
                break;
            default:
                this.state = null;
        }
    }

    @Override
    public boolean test(ItemStack pattern) {
        UpdateState state = NimblePatternTag.getStatus(pattern);
        return state == this.state;
    }
}
