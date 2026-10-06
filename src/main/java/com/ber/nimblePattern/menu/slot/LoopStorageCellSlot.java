package com.ber.nimblePattern.menu.slot;

import appeng.api.inventories.InternalInventory;
import appeng.menu.slot.RestrictedInputSlot;
import com.ber.nimblePattern.item.storage.LoopStorageCellItem;
import net.minecraft.world.item.ItemStack;

public class LoopStorageCellSlot extends RestrictedInputSlot {
    public LoopStorageCellSlot(InternalInventory inventory, int slot) {
        super(PlacableItemType.STORAGE_CELLS, inventory, slot);
        setStackLimit(1);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return super.mayPlace(stack) && stack.getItem() instanceof LoopStorageCellItem;
    }
}
