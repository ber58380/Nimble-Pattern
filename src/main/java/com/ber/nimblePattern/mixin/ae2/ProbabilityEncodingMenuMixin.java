package com.ber.nimblePattern.mixin.ae2;

import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.slot.RestrictedInputSlot;
import appeng.util.ConfigInventory;
import com.ber.nimblePattern.probability.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value = PatternEncodingTermMenu.class, remap = false)
public abstract class ProbabilityEncodingMenuMixin implements ProbabilityEncoding.Menu {
    @Shadow @Final private ConfigInventory encodedInputsInv;
    @Shadow @Final private ConfigInventory encodedOutputsInv;
    @Shadow @Final private RestrictedInputSlot encodedPatternSlot;
    @Shadow protected abstract ItemStack encodePattern();
    @Unique private ResourceLocation nimble$recipe;
    @Unique private ProbabilityEncoding.Normalized nimble$normalized;
    @Unique private java.util.List<ProbabilityRecipes.Output> nimble$recipeOutputs = java.util.List.of();

    @Override public void nimble$selectProbabilityRecipe(ResourceLocation id) {
        nimble$recipe = null;
        nimble$normalized = null;
        nimble$recipeOutputs = java.util.List.of();
        var menu = (PatternEncodingTermMenu) (Object) this;
        var recipe = menu.getPlayer().level().getRecipeManager().byKey(id).orElse(null);
        if (recipe == null || menu.getMode() != appeng.parts.encoding.EncodingMode.PROCESSING) return;
        try {
            var outputs = ProbabilityRecipes.outputs(recipe);
            var normalized = ProbabilityEncoding.normalize(ProbabilityEncoding.snapshot(encodedInputsInv),
                    ProbabilityEncoding.snapshot(encodedOutputsInv), outputs);
            if (normalized == null) return;
            encodedInputsInv.clear(); encodedOutputsInv.clear();
            for (int i = 0; i < normalized.inputs().size(); i++) encodedInputsInv.setStack(i, normalized.inputs().get(i));
            for (int i = 0; i < normalized.outputs().size(); i++) encodedOutputsInv.setStack(i, normalized.outputs().get(i));
            nimble$recipe = id;
            nimble$normalized = normalized;
            nimble$recipeOutputs = outputs;
            menu.broadcastChanges();
        } catch (ArithmeticException | IllegalArgumentException ignored) { }
    }

    @Redirect(method = "encode", at = @At(value = "INVOKE", target = "Lappeng/menu/me/items/PatternEncodingTermMenu;encodePattern()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack encodeProbability(PatternEncodingTermMenu menu) {
        var encoded = encodePattern();
        if (encoded == null || menu.getMode() != appeng.parts.encoding.EncodingMode.PROCESSING) return encoded;
        var previous = encodedPatternSlot.getItem();
        // encodePattern produces native AE NBT with the currently displayed (expected) amounts.
        if (ProbabilityEncoding.reencodingSamePattern(previous, encoded)) return encoded;
        if (nimble$normalized != null && nimble$recipe != null
                && ProbabilityEncoding.hasProbabilityOutput(ProbabilityEncoding.snapshot(encodedOutputsInv), nimble$recipeOutputs))
            ProbabilityEncoding.mark(encoded, nimble$recipe,
                    ProbabilityEncoding.editedCycles(nimble$normalized, ProbabilityEncoding.snapshot(encodedInputsInv)));
        return encoded;
    }
}
