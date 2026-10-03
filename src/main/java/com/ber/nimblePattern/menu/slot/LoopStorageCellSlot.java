package com.ber.nimblePattern.menu.slot;

import appeng.api.inventories.InternalInventory;
import appeng.menu.slot.RestrictedInputSlot;
import com.ber.nimblePattern.item.storage.LoopStorageCellItem;
import net.minecraft.world.item.ItemStack;

public final class LoopStorageCellSlot extends RestrictedInputSlot {
    public LoopStorageCellSlot(InternalInventory inventory, int slot) {
        super(PlacableItemType.STORAGE_CELLS, inventory, slot);
        setStackLimit(1);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.getItem() instanceof LoopStorageCellItem && super.mayPlace(stack);
    }
}
