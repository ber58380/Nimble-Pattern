package com.ber.nimblePattern.mixin.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.KeyCounter;
import appeng.core.AEConfig;
import appeng.crafting.inv.NetworkCraftingSimulationState;
import com.ber.nimblePattern.item.storage.LoopStorageCellAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NetworkCraftingSimulationState.class, remap = false)
public class NetworkCraftingSimulationStateMixin {
    @Shadow
    @Final
    private KeyCounter list;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void excludeLoopSeedsFromPlanning(IStorageService storage, @Nullable IActionSource source, CallbackInfo ci) {
        if (source == null) {
            return;
        }
        var available = new KeyCounter();
        if (!LoopStorageCellAccess.getAvailableWithoutLoopCells(storage.getInventory(), available)) {
            return;
        }

        list.clear();
        for (var stack : available) {
            long amount = AEConfig.instance().isCraftingSimulatedExtraction()
                    ? storage.getInventory().extract(
                    stack.getKey(), stack.getLongValue(), Actionable.SIMULATE, source)
                    : stack.getLongValue();
            if (amount > 0) {
                list.add(stack.getKey(), amount);
            }
        }
    }
}
