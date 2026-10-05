package com.ber.nimblePattern.client.gui.panel;

import appeng.client.Point;
import appeng.client.gui.WidgetContainer;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.TabButton;
import com.ber.nimblePattern.NimblePattern;
import com.ber.nimblePattern.client.gui.PatternTagTermScreen;
import com.ber.nimblePattern.client.gui.widgets.NimbleButton;
import com.ber.nimblePattern.client.gui.widgets.PanelTabButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import static com.ber.nimblePattern.menu.SlotSemantics.*;

public class PatternLoopPanel extends TagModePanel {
    private static final Blitter BG = Blitter.texture(
            ResourceLocation.fromNamespaceAndPath(NimblePattern.MOD_ID, "textures/guis/panel/loop/loop_panel.png"),
            WIDTH,
            HEIGHT).src(0, 0, WIDTH, HEIGHT);
    private final Button loopApplyButton;

    public PatternLoopPanel(PatternTagTermScreen<?> screen, WidgetContainer widgets) {
        super(screen, widgets);
        this.loopApplyButton = new NimbleButton(0, 0, 0, 0, Component.translatable("gui.nimble_pattern.pattern_tag_terminal.apply"), button -> menu.applyLoop());
        widgets.add("loopApplyButton", loopApplyButton);
    }

    @Override
    public void updateBeforeRender() {
        screen.repositionSlots(LOOP_INPUT);
        screen.repositionSlots(LOOP_OUTPUT);
        screen.repositionSlots(LOOP_STORAGE_CELL);
    }

    @Override
    public void drawBackgroundLayer(GuiGraphics guiGraphics, Rect2i bounds, Point mouse) {
        BG.dest(bounds.getX() + x, bounds.getY() + y).blit(guiGraphics);
    }

    @Override
    public TabButton createTabButton(Button.OnPress onPress) {
        return new PanelTabButton(ResourceLocation.fromNamespaceAndPath(NimblePattern.MOD_ID, "textures/guis/panel/loop/loop_tab.png"),
                getTabTooltip(),
                onPress);
    }

    @Override
    public Component getTabTooltip() {
        return Component.translatable("gui.nimble_pattern.pattern_tag_terminal.tab.loop");
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        loopApplyButton.visible = visible;
        screen.setSlotsHidden(LOOP_INPUT, !visible);
        screen.setSlotsHidden(LOOP_OUTPUT, !visible);
        screen.setSlotsHidden(LOOP_STORAGE_CELL, !visible);
    }
}
