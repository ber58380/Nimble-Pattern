package com.ber.nimblePattern.item.storage;

import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ISaveProvider;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.item.ItemStack;

public final class LoopStorageCellHandler implements ICellHandler {
    public static final LoopStorageCellHandler INSTANCE = new LoopStorageCellHandler();

    private LoopStorageCellHandler() {
    }

    @Override
    public boolean isCell(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof LoopStorageCellItem;
    }

    @Override
    public @Nullable LoopStorageCellInventory getCellInventory(ItemStack stack, @Nullable ISaveProvider host) {
        if (!(stack.getItem() instanceof LoopStorageCellItem item)) {
            return null;
        }
        return new LoopStorageCellInventory(stack, item, host);
    }
}
