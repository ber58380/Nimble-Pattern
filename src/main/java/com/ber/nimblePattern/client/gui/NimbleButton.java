package com.ber.nimblePattern.client.gui;

import appeng.client.gui.style.Blitter;
import com.ber.nimblePattern.NimblePattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class NimbleButton extends Button {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(NimblePattern.MOD_ID, "textures/gui/button.png");
    private static final ResourceLocation TEX_H = ResourceLocation.fromNamespaceAndPath(NimblePattern.MOD_ID, "textures/gui/button_highlighted.png");
    private static final ResourceLocation TEX_D = ResourceLocation.fromNamespaceAndPath(NimblePattern.MOD_ID, "textures/gui/button_disabled.png");
    private static final int TEX_W = 200, TEX_HI = 20, B = 2; // 2px border

    public NimbleButton(int x, int y, int w, int h, Component msg, OnPress onPress) {
        super(x, y, w, h, msg, onPress, DEFAULT_NARRATION);
    }

    @Override
    public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
        var tex = !active ? TEX_D : isHoveredOrFocused() ? TEX_H : TEX;
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        // corners
        Blitter.texture(tex, TEX_W, TEX_HI).src(0, 0, B, B).dest(x, y, B, B).blit(g);
        Blitter.texture(tex, TEX_W, TEX_HI).src(TEX_W - B, 0, B, B).dest(x + w - B, y, B, B).blit(g);
        Blitter.texture(tex, TEX_W, TEX_HI).src(0, TEX_HI - B, B, B).dest(x, y + h - B, B, B).blit(g);
        Blitter.texture(tex, TEX_W, TEX_HI).src(TEX_W - B, TEX_HI - B, B, B).dest(x + w - B, y + h - B, B, B).blit(g);
        // sides
        Blitter.texture(tex, TEX_W, TEX_HI).src(B, 0, TEX_W - 2 * B, B).dest(x + B, y, w - 2 * B, B).blit(g);
        Blitter.texture(tex, TEX_W, TEX_HI).src(B, TEX_HI - B, TEX_W - 2 * B, B).dest(x + B, y + h - B, w - 2 * B, B).blit(g);
        Blitter.texture(tex, TEX_W, TEX_HI).src(0, B, B, TEX_HI - 2 * B).dest(x, y + B, B, h - 2 * B).blit(g);
        Blitter.texture(tex, TEX_W, TEX_HI).src(TEX_W - B, B, B, TEX_HI - 2 * B).dest(x + w - B, y + B, B, h - 2 * B).blit(g);
        // center
        Blitter.texture(tex, TEX_W, TEX_HI).src(B, B, TEX_W - 2 * B, TEX_HI - 2 * B).dest(x + B, y + B, w - 2 * B, h - 2 * B).blit(g);
        // text center-align
        int col = active ? 0xFFFFFF : 0xA0A0A0;
        if (!active) g.setColor(1, 1, 1, 0.5f);
        g.drawCenteredString(Minecraft.getInstance().font, getMessage(), x + w / 2, y + (h - 8) / 2, col);
        if (!active) g.setColor(1, 1, 1, 1);
        if (isHovered()) renderHover(g, mx, my);
    }

    private void renderHover(GuiGraphics g, int mx, int my) {
        if (isFocused()) {
            g.fill(getX() - 1, getY() - 1, getX() + getWidth() + 1, getY(), 0xFFFFFFFF);
            g.fill(getX() - 1, getY(), getX(), getY() + getHeight(), 0xFFFFFFFF);
            g.fill(getX() + getWidth(), getY(), getX() + getWidth() + 1, getY() + getHeight(), 0xFFFFFFFF);
            g.fill(getX() - 1, getY() + getHeight(), getX() + getWidth() + 1, getY() + getHeight() + 1, 0xFFFFFFFF);
        }
    }
}
