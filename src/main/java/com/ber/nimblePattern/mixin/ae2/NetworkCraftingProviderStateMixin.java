package com.ber.nimblePattern.mixin.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.helpers.patternprovider.PatternContainer;
import com.ber.nimblePattern.pattern.LoopPatternData;
import com.ber.nimblePattern.pattern.NimblePatternTag;
import com.ber.nimblePattern.pattern.NimbleEncodedPattern;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

/** Ensures loop patterns are collapsed even when they come from a non-vanilla AE2 crafting provider. */
@Mixin(targets = "appeng.me.service.helpers.NetworkCraftingProviders$ProviderState", remap = false)
public class NetworkCraftingProviderStateMixin {
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private ICraftingProvider provider;
    @org.spongepowered.asm.mixin.Unique private Object nimble$registration;

    @org.spongepowered.asm.mixin.injection.Inject(method = "unmount", at = @At("TAIL"))
    private void releaseProviderIndex(appeng.me.service.helpers.NetworkCraftingProviders methods,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        com.ber.nimblePattern.pattern.ProviderPatternIndex.unregister(provider, nimble$registration);
    }
    @Redirect(
            method = "<init>",
            at = @At(value = "INVOKE",
                    target = "Lappeng/api/networking/crafting/ICraftingProvider;getAvailablePatterns()Ljava/util/List;"))
    private List<IPatternDetails> wrapLoopPatterns(ICraftingProvider provider) {
        var original = provider.getAvailablePatterns();
        var wrapped = new ArrayList<IPatternDetails>(original.size());
        var recoveredLoopData = collectPhysicalLoopData(provider);
        var probabilityIndex = new com.ber.nimblePattern.probability.ProbabilityPattern.Index(provider);
        for (var pattern : original) {
            pattern = probabilityIndex.recover(pattern);
            var loopData = pattern instanceof NimbleEncodedPattern n ? n.getLoopData()
                    : NimblePatternTag.getLoopData(pattern.getDefinition().toStack());
            if (loopData == null && pattern instanceof AEProcessingPattern) {
                loopData = recoverLoopData(pattern.getPrimaryOutput(), recoveredLoopData);
            } else if (loopData != null) {
                recoveredLoopData.remove(loopData);
            }
            if (pattern instanceof AEProcessingPattern processingPattern && loopData != null && loopData.size() >= 2) {
                wrapped.add(new NimbleEncodedPattern(processingPattern, false, loopData));
            } else {
                wrapped.add(pattern instanceof NimbleEncodedPattern ? pattern : NimbleEncodedPattern.wrap(pattern, false));
            }
        }
        nimble$registration = com.ber.nimblePattern.pattern.ProviderPatternIndex.register(provider, original, wrapped);
        return wrapped;
    }

    /**
     * GTL pattern buffers may decode and then re-encode their patterns to remove an integrated circuit or trim
     * byproducts. That reconstructed definition no longer contains our loop tag, while the physical encoded pattern
     * in the provider inventory still does. PatternContainer gives us a provider-neutral way to recover it.
     */
    private static List<LoopPatternData> collectPhysicalLoopData(ICraftingProvider provider) {
        var result = new ArrayList<LoopPatternData>();
        if (!(provider instanceof PatternContainer container)) {
            return result;
        }

        var inventory = container.getTerminalPatternInventory();
        for (int slot = 0; slot < inventory.size(); slot++) {
            var data = NimblePatternTag.getLoopData(inventory.getStackInSlot(slot));
            if (data != null && data.size() >= 2 && data.index() >= 0 && data.index() < data.mainOutputs().size()) {
                result.add(data);
            }
        }
        return result;
    }

    @Nullable
    private static LoopPatternData recoverLoopData(GenericStack exposedPrimaryOutput,
            List<LoopPatternData> candidates) {
        LoopPatternData match = null;
        for (var candidate : candidates) {
            var taggedPrimaryOutput = candidate.mainOutputs().get(candidate.index());
            if (sameStack(exposedPrimaryOutput, taggedPrimaryOutput)) {
                // Do not guess if two independently tagged loops expose the same primary output through one provider.
                if (match != null && !match.equals(candidate)) {
                    return null;
                }
                match = candidate;
            }
        }
        if (match != null) {
            candidates.remove(match);
        }
        return match;
    }

    private static boolean sameStack(GenericStack left, GenericStack right) {
        return left != null && right != null
                && left.amount() == right.amount()
                && left.what().equals(right.what());
    }
}
