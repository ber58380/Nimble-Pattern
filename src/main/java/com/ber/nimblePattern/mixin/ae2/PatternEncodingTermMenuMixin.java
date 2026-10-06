package com.ber.nimblePattern.mixin.ae2;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.slot.RestrictedInputSlot;
import com.ber.nimblePattern.pattern.NimblePatternTag;
import net.minecraft.world.item.BookItem;
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

    /**
     * Determine whether the pattern is a fake pattern.
     * The fake pattern should have only one output and the output is a renamed book.
     */
    @Unique
    private static boolean isFakePattern(AEProcessingPattern pattern) {
        var outputs = pattern.getOutputs();
        if (outputs.length != 1) {
            return false;
        }
        if (!(outputs[0].what() instanceof AEItemKey key)) {
            return false;
        }
        var stack = key.toStack();
        if (!stack.hasCustomHoverName()) {
            return false;
        }
        var item = stack.getItem();
        return item instanceof BookItem;
    }

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
    private static boolean isSamePattern(AEProcessingPattern previous, AEProcessingPattern current) {
        return calculateTotalAmounts(previous.getSparseInputs()).equals(calculateTotalAmounts(current.getSparseInputs()))
                && calculateTotalAmounts(previous.getSparseOutputs()).equals(calculateTotalAmounts(current.getSparseOutputs()));
    }

    /**
     * If new encoded pattern is fake pattern and is not same as previous one, tag fake information on pattern.
     * Else encoded without tag.
     */
    @Redirect(method = "encode", at = @At(value = "INVOKE", target = "Lappeng/menu/me/items/PatternEncodingTermMenu;encodePattern()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack encodeWithTag(PatternEncodingTermMenu menu) {
        var encoded = encodePattern();
        if (encoded == null) {
            return null;
        }
        var level = menu.getPlayer().level();
        var decoded = PatternDetailsHelper.decodePattern(encoded, level);
        // Fake pattern must be a processing pattern
        if (!(decoded instanceof AEProcessingPattern current)) {
            return encoded;
        }
        // Determine whether it needs to tag fake information
        if (isFakePattern(current)) {
            var previousPattern = encodedPatternSlot.getItem();
            if (NimblePatternTag.getFake(previousPattern)) {
                var previousDecoded = PatternDetailsHelper.decodePattern(previousPattern, level);
                if (previousDecoded instanceof AEProcessingPattern previous && isSamePattern(previous, current)) {
                    return encoded;
                }
            }
            NimblePatternTag.tagFake(encoded);
        }
        return encoded;
    }

}
