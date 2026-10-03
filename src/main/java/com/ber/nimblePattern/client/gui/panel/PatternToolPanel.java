package com.ber.nimblePattern.client.gui.panel;

import appeng.client.Point;
import appeng.client.gui.WidgetContainer;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.TabButton;
import com.ber.nimblePattern.NimblePattern;
import com.ber.nimblePattern.client.gui.PatternTagTermScreen;
import com.ber.nimblePattern.client.gui.widgets.NimbleButton;
import com.ber.nimblePattern.menu.PatternTagTermMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public final class PatternToolPanel extends TagModePanel {
    private static final Blitter BG = Blitter.texture(ResourceLocation.fromNamespaceAndPath(
            NimblePattern.MOD_ID, "textures/guis/tool_mode.png"), 88, 68).src(0, 0, 88, 68);
    private final Button apply;

    public PatternToolPanel(PatternTagTermScreen<?> screen, WidgetContainer widgets) {
        super(screen, widgets);
        apply = new NimbleButton(0, 0, 0, 0,
                Component.translatable("gui.nimble_pattern.pattern_tag_terminal.apply"), b -> menu.applyTools());
        widgets.add("toolApplyButton", apply);
    }

    @Override
    public void updateBeforeRender() {
        screen.repositionSlots(PatternTagTermMenu.TOOL_INPUT);
    }

    @Override
    public void drawBackgroundLayer(GuiGraphics graphics, Rect2i bounds, Point mouse) {
        BG.dest(bounds.getX() + x, bounds.getY() + y).blit(graphics);
    }

    @Override
    public TabButton createTabButton(Button.OnPress onPress) {
        return new TabButton(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE.getDefaultInstance(),
                getTabTooltip(), onPress);
    }

    @Override
    public Component getTabTooltip() {
        return Component.translatable("gui.nimble_pattern.pattern_tag_terminal.tab.tool");
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        apply.visible = visible;
        screen.setSlotsHidden(PatternTagTermMenu.TOOL_INPUT, !visible);
    }
}
