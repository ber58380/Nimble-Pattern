package com.ber.nimblePattern.network;

import com.ber.nimblePattern.probability.ProbabilityEncoding;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Carries identity only. Probabilities and quantities are validated from the server's recipes. */
public record ProbabilityRecipePacket(int container, ResourceLocation recipe) {
    public static void encode(ProbabilityRecipePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.container); buf.writeResourceLocation(msg.recipe);
    }
    public static ProbabilityRecipePacket decode(FriendlyByteBuf buf) {
        return new ProbabilityRecipePacket(buf.readVarInt(), buf.readResourceLocation());
    }
    public static void handle(ProbabilityRecipePacket msg, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            var player = context.get().getSender();
            if (player != null && player.containerMenu.containerId == msg.container
                    && player.containerMenu instanceof ProbabilityEncoding.Menu menu)
                menu.nimble$selectProbabilityRecipe(msg.recipe);
        });
        context.get().setPacketHandled(true);
    }
}
