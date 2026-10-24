package com.ber.nimblePattern.client.gui.widgets;

import appeng.api.stacks.GenericStack;
import appeng.menu.guisync.PacketWritable;
import com.ber.nimblePattern.pattern.loop.LoopPatternData;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record LoopDiagramData(List<GenericStack> nodes) implements PacketWritable {
    public static final LoopDiagramData EMPTY = new LoopDiagramData(List.of());
    private static final int MAX_NODES = 256;

    public LoopDiagramData {
        nodes = List.copyOf(nodes);
        if (nodes.size() == 1 || nodes.size() > MAX_NODES
                || nodes.stream().anyMatch(node -> node.amount() <= 0)) {
            throw new IllegalArgumentException("Invalid loop diagram");
        }
    }

    /**
     * If a loop is A->B->C->A, the information stored in LoopPatternData is B->C->A.
     * This function is used to justify the order to A->B->C for following drawing.
     */
    public static LoopDiagramData transform(LoopPatternData loop) {
        var nodes = new ArrayList<GenericStack>();
        nodes.add(loop.entry());
        for (int i = 0; i < loop.outputs().size() - 1; i++) {
            var output = loop.outputs().get(i);
            nodes.add(new GenericStack(output.what(), Math.multiplyExact(output.amount(), loop.operations().get(i))));
        }
        return new LoopDiagramData(nodes);
    }

    public LoopDiagramData(FriendlyByteBuf buffer) {
        this(readNodes(buffer));
    }

    private static List<GenericStack> readNodes(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_NODES) {
            throw new IllegalArgumentException("Invalid loop diagram size");
        }
        var nodes = new ArrayList<GenericStack>(size);
        for (int i = 0; i < size; i++) {
            nodes.add(Objects.requireNonNull(GenericStack.readBuffer(buffer)));
        }
        return nodes;
    }

    @Override
    public void writeToPacket(FriendlyByteBuf buffer) {
        buffer.writeVarInt(nodes.size());
        for (var node : nodes) {
            GenericStack.writeBuffer(node, buffer);
        }
    }
}
