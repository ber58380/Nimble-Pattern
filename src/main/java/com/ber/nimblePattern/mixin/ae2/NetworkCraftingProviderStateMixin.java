package com.ber.nimblePattern.mixin.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.me.service.helpers.NetworkCraftingProviders;
import com.ber.nimblePattern.pattern.*;
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

    @Redirect(
            method = "<init>",
            at = @At(value = "INVOKE",
                    target = "Lappeng/api/networking/crafting/ICraftingProvider;getAvailablePatterns()Ljava/util/List;"))
    private List<IPatternDetails> wrapPatterns(ICraftingProvider provider) {
        var original = provider.getAvailablePatterns();
        var wrapped = new ArrayList<IPatternDetails>(original.size());
        for (var pattern : original) {
            if (pattern instanceof NimblePatternWrapper<?>) {
                wrapped.add(pattern);
            } else {
                var definition = pattern.getDefinition();
                var stack = NimblePatternTag.filterCraftingTags(definition.toStack());
                definition = AEItemKey.of(stack);
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
