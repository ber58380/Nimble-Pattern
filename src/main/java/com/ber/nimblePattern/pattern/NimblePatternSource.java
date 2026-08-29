package com.ber.nimblePattern.pattern;

import appeng.api.inventories.InternalInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

public record NimblePatternSource(ServerLevel level, BlockPos pos, Direction side, int slot,
                                  InternalInventory container) {
}
