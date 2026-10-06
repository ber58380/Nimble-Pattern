package com.ber.nimblePattern.item.storage;

import appeng.api.storage.MEStorage;
import appeng.api.storage.cells.StorageCell;
import com.ber.nimblePattern.mixin.ae2.DelegatingMEInventoryMixin;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class LoopStorageCellVisibility {
    private LoopStorageCellVisibility() {
    }

    public static StorageCell unwrapCell(MEStorage storage) {
        Set<MEStorage> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        while (storage != null && visited.add(storage)) {
            if (storage instanceof StorageCell cell) {
                return cell;
            }
            if (storage instanceof DelegatingMEInventoryMixin accessor) {
                storage = accessor.nimblePattern$getDelegate();
            } else {
                return null;
            }
        }
        return null;
    }

    /**
     * Determine whether it is loop storage cell
     */
    public static boolean isLoopStorageCell(MEStorage storage) {
        return unwrapCell(storage) instanceof LoopStorageCellInventory;
    }

    /**
     * Determine whether it is a storage cell but not a loop storage cell
     */
    public static boolean isNonLoopStorageCell(MEStorage storage) {
        var cell = unwrapCell(storage);
        return cell != null && !(cell instanceof LoopStorageCellInventory);
    }
}
