package com.ber.nimblePattern.mixin.ae2;

import appeng.menu.me.items.PatternEncodingTermMenu;
import com.ber.nimblePattern.network.NimblePatternNetwork;
import com.ber.nimblePattern.network.RecipeProvenancePacket;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.integration.jei.recipe.GTRecipeWrapper;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "appeng.integration.modules.jei.transfer.EncodePatternTransferHandler", remap = false)
public abstract class EncodePatternTransferHandlerMixin {
    /**
     * Get the recipe ID if it's a GT recipe, and send it to the server.
     */
    @Inject(method = "transferRecipe(Lappeng/menu/me/items/PatternEncodingTermMenu;Ljava/lang/Object;Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;Lnet/minecraft/world/entity/player/Player;ZZ)Lmezz/jei/api/recipe/transfer/IRecipeTransferError;", at = @At("RETURN"))
    private void nimblePattern$rememberRecipe(PatternEncodingTermMenu menu, Object recipe,
                                              @Coerce Object slots, Player player, boolean maxTransfer, boolean doTransfer,
                                              CallbackInfoReturnable<Object> cir) {
        if (!doTransfer || !ModList.get().isLoaded("gtceu")) {
            return;
        }
        String id = "";
        if (cir.getReturnValue() == null) {
            if (recipe instanceof GTRecipe gt) {
                id = gt.id.toString();
            }
            if (recipe instanceof GTRecipeWrapper wrapper && wrapper.recipe != null) {
                id = wrapper.recipe.id.toString();
            }
        }
        // Sent after AE2's transfer packet, so the server captures the resulting terminal contents.
        NimblePatternNetwork.CHANNEL.sendToServer(new RecipeProvenancePacket(menu.containerId, id));
    }
}
