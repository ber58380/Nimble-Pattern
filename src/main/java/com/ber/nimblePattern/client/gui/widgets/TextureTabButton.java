package com.ber.nimblePattern.client.gui.widgets;

import appeng.client.gui.Icon;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.TabButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** An AE2 tab with a standalone texture instead of a rendered item. */
public final class TextureTabButton extends TabButton {
    private final Blitter icon;

    public TextureTabButton(ResourceLocation texture, Component tooltip, OnPress onPress) {
        super((Icon) null, tooltip, onPress);
        // Reference dimensions describe GUI pixels, not the PNG resolution. This
        // also allows resource packs to replace these icons at any resolution.
        icon = Blitter.texture(texture, 16, 16).src(0, 0, 16, 16);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        if (!visible) {
            return;
        }
        // Match AE2's icon inset; keep its selected/focused background and tooltip.
        int insetX = switch (getStyle()) {
            case CORNER -> 4;
            case BOX -> 3;
            case HORIZONTAL -> 1;
        };
        icon.dest(getX() + insetX, getY() + 3).blit(graphics);
    }
}
