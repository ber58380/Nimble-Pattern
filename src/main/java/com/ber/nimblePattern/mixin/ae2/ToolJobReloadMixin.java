package com.ber.nimblePattern.mixin.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.crafting.execution.ExecutingCraftingJob;
import appeng.crafting.pattern.AECraftingPattern;
import com.ber.nimblePattern.pattern.NimbleEncodedPattern;
import com.ber.nimblePattern.pattern.ToolPatternData;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ExecutingCraftingJob.class, remap = false)
public class ToolJobReloadMixin {
    @Redirect(method = "<init>(Lnet/minecraft/nbt/CompoundTag;Lappeng/crafting/execution/ExecutingCraftingJob$CraftingDifferenceListener;Lappeng/crafting/execution/CraftingCpuLogic;)V", at = @At(value = "INVOKE", target =
            "Lappeng/api/crafting/PatternDetailsHelper;decodePattern(Lappeng/api/stacks/AEItemKey;Lnet/minecraft/world/level/Level;)Lappeng/api/crafting/IPatternDetails;"))
    private IPatternDetails restoreToolPattern(AEItemKey key, Level level) {
        var stack = key.toStack();
        var root = stack.getTagElement("nimble_pattern");
        boolean fuzzy = root != null && root.getBoolean("execution_fuzzy");
        if (fuzzy) {
            root.remove("execution_fuzzy");
            if (root.isEmpty()) stack.removeTagKey("nimble_pattern");
        }
        var decoded = PatternDetailsHelper.decodePattern(stack, level);
        return decoded == null ? null : NimbleEncodedPattern.wrap(decoded, fuzzy);
    }
}
