package com.ber.nimblePattern.mixin.ae2;

import appeng.menu.me.items.PatternEncodingTermMenu;
import com.ber.nimblePattern.network.*;
import com.ber.nimblePattern.probability.ProbabilityRecipes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "appeng.integration.modules.jei.transfer.EncodePatternTransferHandler", remap = false)
public class ProbabilityJeiTransferMixin {
    @Inject(method = "transferRecipe(Lappeng/menu/me/items/PatternEncodingTermMenu;Ljava/lang/Object;Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;Lnet/minecraft/world/entity/player/Player;ZZ)Lmezz/jei/api/recipe/transfer/IRecipeTransferError;", at = @At("RETURN"))
    private void transferred(PatternEncodingTermMenu menu, Object display, @Coerce Object slots, Player player,
                             boolean max, boolean transfer, CallbackInfoReturnable<Object> cir) {
        if (!transfer || cir.getReturnValue() != null) return;
        var recipe = ProbabilityRecipes.unwrap(display);
        // Ordered after AE's slot packets, including deterministic transfers which invalidate the previous recipe.
        NimblePatternNetwork.CHANNEL.sendToServer(new ProbabilityRecipePacket(menu.containerId,
                recipe == null ? net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("nimble_pattern", "unknown") : recipe.getId()));
    }
}
