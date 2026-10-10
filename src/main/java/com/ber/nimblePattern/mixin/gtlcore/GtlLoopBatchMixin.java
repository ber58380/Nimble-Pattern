package com.ber.nimblePattern.mixin.gtlcore;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import com.ber.nimblePattern.compat.gtlcore.GtlBatchAdapter;
import com.ber.nimblePattern.crafting.LoopBatchDispatch;
import com.ber.nimblePattern.pattern.wrapper.NimbleProcessingPattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "org.gtlcore.gtlcore.integration.ae2.crafting.CraftingPatternAutoExpand", remap = false)
public class GtlLoopBatchMixin {
    @Redirect(method = "getOperations", at = @At(value = "INVOKE",
            target = "Lorg/gtlcore/gtlcore/integration/ae2/crafting/IPatternProviderAutoExpand;gtlcore$getMaxPatternOperations(Lappeng/api/crafting/IPatternDetails;J)J"), require = 0)
    private static long nativeProviderLimit(@Coerce Object provider, IPatternDetails pattern, long remaining) {
        return GtlBatchAdapter.maximum((ICraftingProvider) provider, pattern, remaining);
    }

    @Inject(method = "getOperations", at = @At("RETURN"), require = 0)
    private static void captureOperations(boolean processing, ICraftingProvider provider, IPatternDetails pattern,
                                          long remaining, CallbackInfoReturnable<Long> cir) {
        if (pattern instanceof NimbleProcessingPattern nimble && nimble.isLoopComposite()) {
            LoopBatchDispatch.record(pattern, cir.getReturnValue());
        } else {
            LoopBatchDispatch.clear();
        }
    }
}
