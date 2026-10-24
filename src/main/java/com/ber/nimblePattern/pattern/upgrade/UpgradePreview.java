package com.ber.nimblePattern.pattern.upgrade;

import appeng.api.stacks.GenericStack;
import appeng.menu.guisync.PacketWritable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public record UpgradePreview(long revision, int index, int total, @Nullable ResourceLocation recipeTypeId,
                             @Nullable ResourceLocation recipeId,
                             GenericStack primaryOutput, long oldAmount, List<GenericStack> encodedInputs)
        implements PacketWritable {
    public static final UpgradePreview EMPTY = new UpgradePreview(0, 0, 0, null, null, null, 0, List.of());

    public UpgradePreview {
        encodedInputs = List.copyOf(encodedInputs);
    }

    public UpgradePreview forward(long revision, int index, int total) {
        return new UpgradePreview(revision, index, total, recipeTypeId, recipeId, primaryOutput, oldAmount, encodedInputs);
    }

    public UpgradePreview(FriendlyByteBuf buf) {
        this(readFromPacket(buf));
    }

    private UpgradePreview(UpgradePreview preview) {
        this(preview.revision, preview.index, preview.total, preview.recipeTypeId, preview.recipeId,
                preview.primaryOutput, preview.oldAmount, preview.encodedInputs);
    }

    private static UpgradePreview readFromPacket(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) return EMPTY;
        return new UpgradePreview(buf.readLong(), buf.readVarInt(), buf.readVarInt(),
                buf.readResourceLocation(), buf.readResourceLocation(), GenericStack.readBuffer(buf),
                buf.readVarLong(), buf.readList(b -> Objects.requireNonNull(GenericStack.readBuffer(b))));
    }

    @Override
    public void writeToPacket(FriendlyByteBuf buf) {
        // Unpublished previews also have revision 0; presence is determined by the recipe ID.
        buf.writeBoolean(recipeId != null);
        if (recipeId == null) {
            return;
        }
        buf.writeLong(revision);
        buf.writeVarInt(index);
        buf.writeVarInt(total);
        buf.writeResourceLocation(recipeTypeId);
        buf.writeResourceLocation(recipeId);
        GenericStack.writeBuffer(primaryOutput, buf);
        buf.writeVarLong(oldAmount);
        buf.writeCollection(encodedInputs, (b, stack) -> GenericStack.writeBuffer(stack, b));
    }
}
