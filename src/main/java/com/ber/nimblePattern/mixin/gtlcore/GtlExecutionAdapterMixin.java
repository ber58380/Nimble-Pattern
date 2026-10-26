package com.ber.nimblePattern.mixin.gtlcore;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.KeyCounter;
import com.ber.nimblePattern.compat.gtlcore.GtlGraphDispatchAccess;
import com.ber.nimblePattern.compat.gtlcore.GtlGraphLoopAccess;
import com.ber.nimblePattern.crafting.LoopCpuAccess;
import com.ber.nimblePattern.pattern.PatternMapping;
import com.ber.nimblePattern.pattern.wrapper.NimbleProcessingPattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Restore provider-owned patterns at GTL's graph dispatch boundaries, while retaining wrappers in
 * the graph and crafting service. GTL normally pushes every planned pattern into a provider. A loop
 * composite is a virtual pattern: no machine can execute it. Instead, feed it to Nimble's loop
 * controller on the CPU that owns the graph job, exactly like AE's classic execution path does.
 */
@Pseudo
@Mixin(targets = "org.gtlcore.gtlcore.integration.ae2.graph.GtlExecutionAdapter", remap = false)
public class GtlExecutionAdapterMixin {
    @Shadow
    private long selectedBatch;

    // The provider call lives in the synthetic lambda used for GTL's dispatch preflight context,
    // not directly in push().
    @Redirect(method = {"push", "lambda$push$2"}, at = @At(value = "INVOKE",
            target = "Lappeng/api/networking/crafting/ICraftingProvider;pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;)Z"),
            require = 0)
    private boolean nimblePattern$pushProviderPattern(ICraftingProvider provider, IPatternDetails pattern, KeyCounter[] inputs) {
        if (!(pattern instanceof NimbleProcessingPattern nimble) || !nimble.isLoopComposite()) {
            return provider.pushPattern(PatternMapping.getOriginalPattern(provider, pattern), inputs);
        }
        LoopCpuAccess access = GtlGraphLoopAccess.find(this);
        return access != null && access.nimblePattern$startLoop(nimble, inputs, selectedBatch);
    }

    // Batch limits can also look up the pattern in the provider's own pattern map. Only unwrap at
    // this provider boundary: the crafting service indexes providers by the wrapped pattern.
    // ModifyArg avoids generated Args classes, which Forge's module class loader cannot resolve
    // when this optional mixin is applied to GTL's lazily loaded execution adapter.
    @ModifyArg(method = "lambda$capacity$1", at = @At(value = "INVOKE",
            target = "Lorg/gtlcore/gtlcore/integration/ae2/graph/GtlDispatchPolicy;capacity(Lappeng/api/crafting/IPatternDetails;Lappeng/api/networking/crafting/ICraftingProvider;JZ)J"),
            index = 0, require = 0)
    private IPatternDetails nimblePattern$originalPatternForBatchCapacity(IPatternDetails pattern,
            ICraftingProvider provider, long requested, boolean expanded) {
        if (!(pattern instanceof NimbleProcessingPattern nimble) || !nimble.isLoopComposite()) {
            return PatternMapping.getOriginalPattern(provider, pattern);
        }
        return pattern;
    }

    @Redirect(method = {"lambda$capacity$0", "lambda$push$2"}, at = @At(value = "INVOKE",
            target = "Lorg/gtlcore/gtlcore/integration/ae2/graph/GraphDispatchContext;allowed(Lappeng/api/networking/crafting/ICraftingProvider;Lappeng/api/crafting/IPatternDetails;)Z"),
            require = 0)
    private boolean nimblePattern$allowLoopComposite(ICraftingProvider provider, IPatternDetails pattern) {
        return GtlGraphDispatchAccess.allowed(provider, pattern);
    }

    @Redirect(method = {"lambda$capacity$1", "lambda$push$2"}, at = @At(value = "INVOKE",
            target = "Lorg/gtlcore/gtlcore/integration/ae2/graph/GraphDispatchContext;capacity(Lappeng/api/networking/crafting/ICraftingProvider;Lappeng/api/crafting/IPatternDetails;J)J"),
            require = 0)
    private long nimblePattern$capacityLoopComposite(ICraftingProvider provider, IPatternDetails pattern, long requested) {
        return GtlGraphDispatchAccess.capacity(provider, pattern, requested);
    }
}
