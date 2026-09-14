package com.ber.nimblePattern.client.gui;

import net.minecraft.world.item.ItemStack;

import java.util.Locale;

import static com.ber.nimblePattern.client.gui.search.UnwrapHelper.getDisplayName;

public record PatternRecord(long serverId, int machineSlot, ItemStack stack, String sortKey) {
    public PatternRecord(long serverId, int machineSlot, ItemStack stack) {
        // use name as sorted keys
        this(serverId, machineSlot, stack, getDisplayName(stack).toLowerCase(Locale.ROOT));
    }
}
