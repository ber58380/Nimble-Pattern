package com.ber.nimblePattern.mixin.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.me.storage.NetworkStorage;
import com.ber.nimblePattern.pattern.PatternUpgradeTracker;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = NetworkStorage.class, remap = false)
public class NetworkStorageMixin {
    @Inject(method = "insert", at = @At("RETURN"))
    private void validateInsert(AEKey what, long amount, Actionable mode, IActionSource src, CallbackInfoReturnable<Long> cir) {
        if (mode != Actionable.MODULATE) {
            return;
        }
        long inserted = cir.getReturnValue();
        if (inserted <= 0) {
            return;
        }
        ResourceLocation id = what.getId();
        PatternUpgradeTracker.instance().enqueueIfTracked(id);
    }
}
