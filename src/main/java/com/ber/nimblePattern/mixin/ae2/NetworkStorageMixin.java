package com.ber.nimblePattern.mixin.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.me.storage.NetworkStorage;
import com.ber.nimblePattern.item.storage.LoopStorageCellVisibility;
import com.ber.nimblePattern.pattern.PatternUpgradeTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.NavigableMap;

@Mixin(value = NetworkStorage.class, remap = false)
public class NetworkStorageMixin {
    @Shadow
    @Final
    private NavigableMap<Integer, List<MEStorage>> priorityInventory;

    @Shadow
    private boolean mountsInUse;

    @Inject(method = "insert", at = @At("RETURN"))
    private void validateInsert(AEKey what, long amount, Actionable mode, IActionSource src, CallbackInfoReturnable<Long> cir) {
        if (mode != Actionable.MODULATE) {
            return;
        }
        long inserted = cir.getReturnValue();
        if (inserted <= 0) {
            return;
        }
        PatternUpgradeTracker.instance().enqueueIfTracked(what.getId());
    }

    /**
     * A loop cell contributes to the network listing only while it is the sole kind of mounted storage cell.
     * Non-cell inventories, such as interfaces, do not suppress it.
     */
    @Inject(method = "getAvailableStacks", at = @At("HEAD"), cancellable = true)
    private void hideLoopCellContentsWhenNormalCellsArePresent(KeyCounter out, CallbackInfo ci) {
        boolean hasLoopCell = false;
        boolean hasNormalCell = false;

        for (var inventories : priorityInventory.values()) {
            for (var inventory : inventories) {
                hasLoopCell |= LoopStorageCellVisibility.isLoopCellStorage(inventory);
                hasNormalCell |= LoopStorageCellVisibility.isNonLoopCellStorage(inventory);
                if (hasLoopCell && hasNormalCell) {
                    break;
                }
            }
            if (hasLoopCell && hasNormalCell) {
                break;
            }
        }

        if (!hasLoopCell || !hasNormalCell) {
            return;
        }

        // Match AE2's re-entry guard while rebuilding the listing without loop cells.
        if (mountsInUse) {
            ci.cancel();
            return;
        }

        mountsInUse = true;
        try {
            for (var inventories : priorityInventory.values()) {
                for (var inventory : inventories) {
                    if (!LoopStorageCellVisibility.isLoopCellStorage(inventory)) {
                        inventory.getAvailableStacks(out);
                    }
                }
            }
        } finally {
            mountsInUse = false;
        }
        ci.cancel();
    }
}
