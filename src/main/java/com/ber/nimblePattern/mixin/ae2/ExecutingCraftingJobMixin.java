package com.ber.nimblePattern.mixin.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.crafting.execution.ExecutingCraftingJob;
import appeng.crafting.pattern.AECraftingPattern;
import appeng.crafting.pattern.AEProcessingPattern;
import com.ber.nimblePattern.pattern.NimblePatternTag;
import com.ber.nimblePattern.pattern.wrapper.NimbleCraftingPattern;
import com.ber.nimblePattern.pattern.wrapper.NimbleProcessingPattern;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ExecutingCraftingJob.class, remap = false)
public class ExecutingCraftingJobMixin {
    @Redirect(method = "<init>(Lnet/minecraft/nbt/CompoundTag;Lappeng/crafting/execution/ExecutingCraftingJob$CraftingDifferenceListener;Lappeng/crafting/execution/CraftingCpuLogic;)V", at = @At(value = "INVOKE", target =
            "Lappeng/api/crafting/PatternDetailsHelper;decodePattern(Lappeng/api/stacks/AEItemKey;Lnet/minecraft/world/level/Level;)Lappeng/api/crafting/IPatternDetails;"))
    private IPatternDetails reloadPattern(AEItemKey key, Level level) {
        var stack = NimblePatternTag.filterCraftingTags(key.toStack());
        var definition = AEItemKey.of(stack);
        var decoded = PatternDetailsHelper.decodePattern(stack, level);
        if (decoded == null) {
            return null;
        }
        if (definition != null) {
            if (decoded instanceof AEProcessingPattern app) {
                return new NimbleProcessingPattern(app, definition);
            } else if (decoded instanceof AECraftingPattern acp) {
                return new NimbleCraftingPattern(acp, definition);
            }
        }
        return decoded;
    }
}
