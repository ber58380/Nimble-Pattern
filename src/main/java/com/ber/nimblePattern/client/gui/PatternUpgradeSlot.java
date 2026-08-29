package com.ber.nimblePattern.client.gui;

import appeng.crafting.pattern.EncodedPatternItem;
import appeng.menu.slot.AppEngSlot;
import appeng.util.inv.AppEngInternalInventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PatternUpgradeSlot extends AppEngSlot {
    private final long serverId;
    private final int machineSlot;

    public PatternUpgradeSlot(AppEngInternalInventory inventory, long serverId, int machineSlot, int x, int y) {
        super(inventory, machineSlot);
        this.serverId = serverId;
        this.machineSlot = machineSlot;
        this.x = x;
        this.y = y;
    }

    public long getServerId() {
        return serverId;
    }

    public int getMachineSlot() {
        return machineSlot;
    }

    @Override
    public ItemStack getDisplayStack() {
        if (isRemote()) {
            final ItemStack is = super.getDisplayStack();
            if (!is.isEmpty() && is.getItem() instanceof EncodedPatternItem iep) {
                final ItemStack out = iep.getOutput(is);
                if (!out.isEmpty()) {
                    return out;
                }
            }
        }
        return super.getDisplayStack();
    }

    @Override
    public boolean hasItem() {
        return !this.getItem().isEmpty();
    }

    @Override
    public final boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public final void set(ItemStack stack) {
    }

    @Override
    public void initialize(ItemStack stack) {
    }

    @Override
    public final int getMaxStackSize() {
        return 0;
    }

    @Override
    public final ItemStack remove(int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public final boolean mayPickup(Player player) {
        return false;
    }
}
