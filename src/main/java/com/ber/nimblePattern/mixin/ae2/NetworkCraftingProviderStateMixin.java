package com.ber.nimblePattern.mixin.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.me.service.helpers.NetworkCraftingProviders;
import com.ber.nimblePattern.pattern.NimblePatternTag;
import com.ber.nimblePattern.pattern.PatternMapping;
import com.ber.nimblePattern.pattern.loop.LoopPatternData;
import com.ber.nimblePattern.pattern.wrapper.NimbleAssemblerPattern;
import com.ber.nimblePattern.pattern.wrapper.NimblePatternWrapper;
import com.ber.nimblePattern.pattern.wrapper.NimbleProcessingPattern;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(targets = "appeng.me.service.helpers.NetworkCraftingProviders$ProviderState", remap = false)
public class NetworkCraftingProviderStateMixin {
    @Shadow
    @Final
    private ICraftingProvider provider;
    @Unique
    private Object registration;

    @Inject(method = "unmount", at = @At("TAIL"))
    private void releaseProviderMapping(NetworkCraftingProviders methods, CallbackInfo ci) {
        PatternMapping.unregister(provider, registration);
    }

    /**
     * This function is used to ensure compatibility with the pattern buffers in GT series.
     * The pattern buffers will decode and re-encode patterns to remove circuit and byproducts,
     * leading to loop data missing. Use this function to collect all loop data in the provider.
     */
    @Unique
    private static List<LoopPatternData> collectLoopData(ICraftingProvider provider) {
        var result = new ArrayList<LoopPatternData>();
        if (!(provider instanceof PatternContainer container)) {
            return result;
        }

        var inventory = container.getTerminalPatternInventory();
        for (int slot = 0; slot < inventory.size(); slot++) {
            var data = NimblePatternTag.getLoop(inventory.getStackInSlot(slot));
            if (data != null && data.size() >= 2 && data.index() >= 0 && data.index() < data.outputs().size()) {
                result.add(data);
            }
        }
        return result;
    }

    private static boolean isSameStack(GenericStack left, GenericStack right) {
        return left != null && right != null && left.amount() == right.amount() && left.what().equals(right.what());
    }

    /**
     * Collaborate with the function collectLoopData. This is used to find the executed pattern matches
     * which pattern in the collection. After matching, remove it from the collections.
     * After preprocessing by pattern buffers, inputs might change.
     * Thus, only compare output and return null for uncertainty.
     */
    private static LoopPatternData recoverLoopData(GenericStack output, List<LoopPatternData> candidates) {
        LoopPatternData match = null;
        for (var candidate : candidates) {
            var loopOutput = candidate.outputs().get(candidate.index());
            if (isSameStack(output, loopOutput)) {
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

    @Redirect(
            method = "<init>",
            at = @At(value = "INVOKE",
                    target = "Lappeng/api/networking/crafting/ICraftingProvider;getAvailablePatterns()Ljava/util/List;"))
    private List<IPatternDetails> wrapPatterns(ICraftingProvider provider) {
        var original = provider.getAvailablePatterns();
        var wrapped = new ArrayList<IPatternDetails>(original.size());
        var recoveredLoopData = collectLoopData(provider);
        for (var pattern : original) {
            var definition = pattern.getDefinition();
            var stack = definition.toStack();
            // get loop data of the pattern
            var loopData = pattern instanceof NimbleProcessingPattern npp ? npp.getLoopData() : NimblePatternTag.getLoop(stack);
            if (loopData == null && pattern instanceof AEProcessingPattern) {
                loopData = recoverLoopData(pattern.getPrimaryOutput(), recoveredLoopData);
                if (loopData != null) {
                    NimblePatternTag.tagLoop(stack, loopData);
                }
            } else if (loopData != null) {
                recoveredLoopData.remove(loopData);
            }

            if (pattern instanceof NimblePatternWrapper<?>) {
                wrapped.add(pattern);
            } else {
                definition = AEItemKey.of(NimblePatternTag.filterCraftingTags(stack));
                if (pattern instanceof AEProcessingPattern aep) {
                    wrapped.add(new NimbleProcessingPattern(aep, definition));
                } else if (pattern instanceof IMolecularAssemblerSupportedPattern asp) {
                    wrapped.add(new NimbleAssemblerPattern(asp, definition));
                } else {
                    wrapped.add(pattern);
                }
            }
        }
        registration = PatternMapping.register(provider, original, wrapped);
        return wrapped;
    }
}
