package com.ber.nimblePattern.mixin.ae2;

import appeng.api.storage.MEStorage;
import appeng.me.storage.DelegatingMEInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = DelegatingMEInventory.class, remap = false)
public interface DelegatingMEInventoryMixin {
    @Accessor("delegate")
    MEStorage nimblePattern$getDelegate();
}
