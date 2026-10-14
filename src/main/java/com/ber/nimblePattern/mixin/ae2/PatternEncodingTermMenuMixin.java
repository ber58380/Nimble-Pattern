package com.ber.nimblePattern.mixin.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AECraftingPattern;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.slot.RestrictedInputSlot;
import com.ber.nimblePattern.pattern.NimblePatternTag;
import com.ber.nimblePattern.pattern.PatternValidation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;
import java.util.Map;

@Mixin(value = PatternEncodingTermMenu.class, remap = false)
public abstract class PatternEncodingTermMenuMixin {
    @Shadow
    @Final
    private RestrictedInputSlot encodedPatternSlot;

    @Shadow
    @Nullable
    protected abstract ItemStack encodePattern();

    private static Map<AEKey, Long> calculateTotalAmounts(GenericStack[] stacks) {
        var amounts = new HashMap<AEKey, Long>();
        for (var stack : stacks) {
            if (stack != null) {
                amounts.merge(stack.what(), stack.amount(), Long::sum);
            }
        }
        return amounts;
    }

    @Unique
    private static boolean isSamePattern(IPatternDetails previousDetail, AECraftingPattern current) {
        if (!(previousDetail instanceof AECraftingPattern previous)) {
            return false;
        }
        return calculateTotalAmounts(previous.getSparseInputs()).equals(calculateTotalAmounts(current.getSparseInputs()))
                && calculateTotalAmounts(previous.getSparseOutputs()).equals(calculateTotalAmounts(current.getSparseOutputs()));
    }

    @Unique
    private static boolean isSamePattern(IPatternDetails previousDetail, AEProcessingPattern current) {
        if (!(previousDetail instanceof AEProcessingPattern previous)) {
            return false;
        }
        return calculateTotalAmounts(previous.getSparseInputs()).equals(calculateTotalAmounts(current.getSparseInputs()))
                && calculateTotalAmounts(previous.getSparseOutputs()).equals(calculateTotalAmounts(current.getSparseOutputs()));
    }

    /**
     * Detect the encoding pattern and determine whether it is a fake pattern, tool pattern.
     */
    @Redirect(method = "encode", at = @At(value = "INVOKE", target = "Lappeng/menu/me/items/PatternEncodingTermMenu;encodePattern()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack encodeWithTag(PatternEncodingTermMenu menu) {
        var encoded = encodePattern();
        if (encoded == null) {
            return null;
        }
        var level = menu.getPlayer().level();
        var patternDetails = PatternDetailsHelper.decodePattern(encoded, level);
        var previousPattern = encodedPatternSlot.getItem();
        var previousDecoded = PatternDetailsHelper.decodePattern(previousPattern, level);

        if (patternDetails instanceof AECraftingPattern current) {
            // Determine whether it needs to tag tool information
            var tools = PatternValidation.isToolPattern(current);
            if (!tools.isEmpty()) {
                if (NimblePatternTag.getTool(previousPattern).isEmpty() || !isSamePattern(previousDecoded, current)) {
                    NimblePatternTag.tagTool(encoded, tools);
                }
            }
        } else if (patternDetails instanceof AEProcessingPattern current) {
            // Determine whether it needs to tag fake information
            if (PatternValidation.isFakePattern(current)) {
                if (!NimblePatternTag.getFake(previousPattern) || !isSamePattern(previousDecoded, current)) {
                    NimblePatternTag.tagFake(encoded);
                }
            }
        }

        return encoded;
    }

}
