package com.ber.nimblePattern.item.storage;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import com.ber.nimblePattern.mixin.ae2.NetworkStorageAccessorMixin;

import java.util.UUID;

public final class LoopStorageCellAccess {
    private LoopStorageCellAccess() {
    }

    public static LoopStorageCellInventory find(MEStorage networkStorage, UUID cellId, AEKey seed, long amount) {
        if (networkStorage instanceof NetworkStorageAccessorMixin accessor) {
            for (var inventories : accessor.nimblePattern$getPriorityInventory().values()) {
                for (var inventory : inventories) {
                    var cell = LoopStorageCellVisibility.unwrapCell(inventory);
                    if (cell instanceof LoopStorageCellInventory loopCell && cellId.equals(loopCell.getCellId())
                            && loopCell.extractForLoop(seed, amount, Actionable.SIMULATE) == amount) {
                        return loopCell;
                    }
                }
            }
        }
        return null;
    }

    public static boolean getAvailableWithoutLoopCells(MEStorage networkStorage, KeyCounter out) {
        if (networkStorage instanceof NetworkStorageAccessorMixin accessor) {
            for (var inventories : accessor.nimblePattern$getPriorityInventory().values()) {
                for (var inventory : inventories) {
                    if (!LoopStorageCellVisibility.isLoopStorageCell(inventory)) {
                        inventory.getAvailableStacks(out);
                    }
                }
            }
            return true;
        }
        return false;
    }
}
