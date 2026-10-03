package com.ber.nimblePattern.mixin.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import com.ber.nimblePattern.crafting.LoopBatchDispatch;
import com.ber.nimblePattern.pattern.NimbleEncodedPattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional compatibility with GTLCore's provider-side operation batching. */
@Pseudo
@Mixin(targets = "org.gtlcore.gtlcore.integration.ae2.crafting.CraftingPatternAutoExpand", remap = false)
public class GtlLoopBatchMixin {
    @org.spongepowered.asm.mixin.injection.Redirect(method = "getOperations", at = @At(value = "INVOKE",
            target = "Lorg/gtlcore/gtlcore/integration/ae2/crafting/IPatternProviderAutoExpand;gtlcore$getMaxPatternOperations(Lappeng/api/crafting/IPatternDetails;J)J"), require = 0)
    private static long nativeProviderLimit(@org.spongepowered.asm.mixin.injection.Coerce Object provider,
            IPatternDetails pattern, long remaining) {
        return com.ber.nimblePattern.compat.gtl.GtlBatchAdapter.maximum((ICraftingProvider) provider, pattern, remaining);
    }
    @Inject(method = "getOperations", at = @At("HEAD"), cancellable = true, require = 0)
    private static void serializeToolUse(boolean processing, ICraftingProvider provider, IPatternDetails pattern,
            long remaining, CallbackInfoReturnable<Long> cir) {
        if (pattern instanceof com.ber.nimblePattern.pattern.NimbleAssemblerPattern tool && tool.isToolPattern()) {
            cir.setReturnValue(Math.min(remaining, 1));
        }
    }
    @Inject(method = "getOperations", at = @At("RETURN"), cancellable = true, require = 0)
    private static void captureOperations(boolean processing, ICraftingProvider provider, IPatternDetails pattern,
            long remaining, CallbackInfoReturnable<Long> cir) {
        if (pattern instanceof NimbleEncodedPattern nimble && nimble.metadata().probability()) {
            long count = cir.getReturnValue();
            long cycles = nimble.metadata().cycles();
            if (cycles > 0) count = Math.min(count, Long.MAX_VALUE / cycles);
            for (var output : pattern.getOutputs()) {
                if (output.amount() > 0) count = Math.min(count, Long.MAX_VALUE / output.amount());
            }
            cir.setReturnValue(count);
            LoopBatchDispatch.record(pattern, count);
        } else if (pattern instanceof NimbleEncodedPattern nimble && nimble.isLoopComposite()) {
            LoopBatchDispatch.record(pattern, cir.getReturnValue());
        } else {
            LoopBatchDispatch.clear();
        }
    }
}
