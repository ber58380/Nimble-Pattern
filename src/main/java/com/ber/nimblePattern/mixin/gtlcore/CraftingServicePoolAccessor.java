package com.ber.nimblePattern.mixin.gtlcore;

import appeng.me.service.CraftingService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.ExecutorService;

@Mixin(value = CraftingService.class, remap = false)
public interface CraftingServicePoolAccessor {
    @Accessor("CRAFTING_POOL")
    static ExecutorService nimblePattern$craftingPool() {
        throw new AssertionError("Mixin accessor not applied");
    }
}
