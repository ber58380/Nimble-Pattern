package com.ber.nimblePattern.item.storage;

import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class LoopStorageCellHandler implements ICellHandler {
    public static final LoopStorageCellHandler INSTANCE = new LoopStorageCellHandler();

    private LoopStorageCellHandler() {

    }

    @Override
    public boolean isCell(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof LoopStorageCellItem;
    }

    @Override
    public @Nullable StorageCell getCellInventory(ItemStack stack, @Nullable ISaveProvider host) {
        if (stack.getItem() instanceof LoopStorageCellItem item) {
            return new LoopStorageCellInventory(stack, item, host);
        }
        return null;
    }
}
