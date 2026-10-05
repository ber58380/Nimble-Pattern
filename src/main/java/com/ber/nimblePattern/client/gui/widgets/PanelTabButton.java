package com.ber.nimblePattern.client.gui.widgets;

import appeng.client.gui.Icon;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.TabButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class PanelTabButton extends TabButton {
    private final Blitter icon;

    public PanelTabButton(ResourceLocation textureFile, Component tooltip, OnPress onPress) {
        super((Icon) null, tooltip, onPress);
        icon = Blitter.texture(textureFile, 16, 16).src(0, 0, 16, 16);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int x, int y, float partial) {
        super.renderWidget(guiGraphics, x, y, partial);
        if (!visible) {
            return;
        }
        int insetX = switch (getStyle()) {
            case CORNER -> 4;
            case BOX -> 3;
            case HORIZONTAL -> 1;
        };
        icon.dest(getX() + insetX, getY() + 3).blit(guiGraphics);
    }
}
