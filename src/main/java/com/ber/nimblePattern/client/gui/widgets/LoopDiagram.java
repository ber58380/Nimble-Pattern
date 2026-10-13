package com.ber.nimblePattern.client.gui.widgets;

import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AmountFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

public final class LoopDiagram {
    private static final int STEP = 32;
    private static final int MARGIN = 16;
    private final LoopDiagramData data;
    private double scroll;

    public LoopDiagram(LoopDiagramData data) {
        this.data = data;
    }

    private int contentWidth() {
        return data.nodes().size() * STEP + MARGIN;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta,
                                 int left, int top, int width) {
        if (mouseX < left || mouseX >= left + width || mouseY < top || mouseY >= top + 33
                || contentWidth() <= width) {
            return false;
        }
        scroll = Math.max(0, Math.min(contentWidth() - width, scroll - delta * STEP));
        return true;
    }

    public void render(GuiGraphics graphics, int left, int top, int width,
                       int mouseX, int mouseY, int color) {
        if (data.nodes().isEmpty()) return;
        var minecraft = Minecraft.getInstance();
        int origin = left + Math.max(0, (width - contentWidth()) / 2) - (int) scroll;
        int first = origin + MARGIN;
        int last = first + (data.nodes().size() - 1) * STEP;
        int hovered = -1;
        graphics.enableScissor(left, top, left + width, top + 33);
        try {
            arrowRight(graphics, origin, first - 3, top + 8, color);
            for (int i = 0; i < data.nodes().size(); i++) {
                int x = first + i * STEP;
                arrowRight(graphics, x + 18, x + STEP - 3, top + 8, color);
                if (x + 16 > left && x < left + width) {
                    AEKeyRendering.drawInGui(minecraft, graphics, x, top, data.nodes().get(i).what());
                    if (mouseX >= Math.max(left, x) && mouseX < Math.min(left + width, x + 16)
                            && mouseY >= top && mouseY < top + 16) {
                        hovered = i;
                    }
                }
            }
            // draw the loop return arrow
            graphics.vLine(last + 8, top + 17, top + 25, color);
            graphics.hLine(first + 8, last + 8, top + 25, color);
            graphics.vLine(first + 8, top + 17, top + 25, color);
            for (int i = 1; i <= 3; i++) {
                graphics.fill(first + 8 - i, top + 17 + i, first + 9 - i, top + 18 + i, color);
                graphics.fill(first + 8 + i, top + 17 + i, first + 9 + i, top + 18 + i, color);
            }
            if (contentWidth() > width) {
                graphics.hLine(left, left + width - 1, top + 32, 0xFFAAAAAA);
                int thumb = Math.max(12, width * width / contentWidth());
                int x = left + (int) ((width - thumb) * scroll / (contentWidth() - width));
                graphics.hLine(x, x + thumb - 1, top + 32, color);
            }
        } finally {
            graphics.disableScissor();
        }
        if (hovered >= 0) {
            var node = data.nodes().get(hovered);
            var tooltip = new ArrayList<>(AEKeyRendering.getTooltip(node.what()));
            tooltip.add(Component.literal(node.what().formatAmount(node.amount(), AmountFormat.FULL)));
            graphics.renderComponentTooltip(minecraft.font, tooltip, mouseX, mouseY);
        }
    }

    private static void arrowRight(GuiGraphics graphics, int from, int to, int y, int color) {
        graphics.hLine(from, to, y, color);
        for (int i = 1; i <= 3; i++) {
            graphics.fill(to - i, y - i, to - i + 1, y - i + 1, color);
            graphics.fill(to - i, y + i, to - i + 1, y + i + 1, color);
        }
    }
}
