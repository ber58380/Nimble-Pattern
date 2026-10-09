package com.ber.nimblePattern.client.gui.search;

import com.ber.nimblePattern.pattern.NimblePatternTag;
import com.ber.nimblePattern.pattern.UpgradeState;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.function.Predicate;

import static com.ber.nimblePattern.pattern.UpgradeState.*;

final class StatusSearchPredicate implements Predicate<ItemStack> {
    private final UpgradeState state;

    public StatusSearchPredicate(String term) {
        // TODO: 考虑更换一下搜索方式
        switch (term.toUpperCase(Locale.ROOT).trim()) {
            case "UNTRACKED", "0", "":
                this.state = UNTRACKED;
                break;
            case "LATEST", "1":
                this.state = LATEST;
                break;
            case "UPDATE", "2":
                this.state = UPGRADE;
                break;
            default:
                this.state = null;
        }
    }

    @Override
    public boolean test(ItemStack pattern) {
        UpgradeState state = NimblePatternTag.getStatus(pattern);
        return state == this.state;
    }
}
