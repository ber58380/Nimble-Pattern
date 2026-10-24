package com.ber.nimblePattern.network;

import com.ber.nimblePattern.pattern.upgrade.RecipeProvenanceMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RecipeProvenancePacket(int menuId, String recipeId) {
    public static void encode(RecipeProvenancePacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.menuId);
        buf.writeUtf(packet.recipeId, 512);
    }

    public static RecipeProvenancePacket decode(FriendlyByteBuf buf) {
        return new RecipeProvenancePacket(buf.readVarInt(), buf.readUtf(512));
    }

    public static void handle(RecipeProvenancePacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null
                    && player.containerMenu.containerId == packet.menuId
                    && player.containerMenu.stillValid(player)
                    && player.containerMenu instanceof RecipeProvenanceMenu menu)
                menu.nimblePattern$captureRecipe(packet.recipeId);
        });
        context.setPacketHandled(true);
    }
}
