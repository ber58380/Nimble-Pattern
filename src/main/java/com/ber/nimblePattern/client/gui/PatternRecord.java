package com.ber.nimblePattern.client.gui;

import net.minecraft.world.item.ItemStack;

public record PatternRecord(long serverId, int machineSlot, ItemStack stack) {
}
