package com.ber.nimblePattern.client.gui;

import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashSet;
import java.util.Set;

public class PromptTextField extends AETextField {
    private Set<String> history = new LinkedHashSet<String>();
    private String prompt = null;
    private final ScreenStyle style;

    public PromptTextField(ScreenStyle style, Font fontRenderer, int xPos, int yPos, int width, int height) {
        super(style, fontRenderer, xPos, yPos, width, height);
        this.style = style;
        setBordered(false);
        setTextColor(0xFFFFFF);
        setSelectionColor(style.getColor(PaletteColor.TEXTFIELD_SELECTION).toARGB());
        setVisible(true);
        setResponder(v -> update());
    }

    public void setHistory(Set<String> history) {
        this.history = history;
        update();
    }

    public void update() {
        var cur = getValue();
        if (cur == null) {
            prompt = null;
            return;
        }
        var input = cur.toLowerCase();
        prompt = history.stream().filter(s -> s.toLowerCase().startsWith(input) && !s.toLowerCase().equals(input)).findFirst().map(s -> s.substring(cur.length())).orElse(null);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (prompt != null && keyCode == GLFW.GLFW_KEY_TAB) {
            setValue(getValue() + prompt);
            moveCursorToEnd();
            update();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partial);
        if (prompt != null && isFocused() && !getValue().isEmpty()) {
            var font = Minecraft.getInstance().font;
            // too long, don't show the prompt
            if (font.width(getValue()) > this.width - 8) {
                return;
            }
            guiGraphics.drawString(
                    font,
                    prompt,
                    getX() + font.width(getValue()),
                    getY(),
                    style.getColor(PaletteColor.TEXTFIELD_PLACEHOLDER).toARGB(),
                    false);
        }
    }
}
