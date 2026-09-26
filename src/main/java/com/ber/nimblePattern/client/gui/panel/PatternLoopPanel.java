package com.ber.nimblePattern.client.gui.panel;

import appeng.client.Point;
import appeng.client.gui.WidgetContainer;
import appeng.client.gui.style.Blitter;
import com.ber.nimblePattern.NimblePattern;
import com.ber.nimblePattern.client.gui.PatternTagTermScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PatternLoopPanel extends TagModePanel{
    private static final int WIDTH = 88;
    private static final int HEIGHT = 68;
    private static final Blitter BG = Blitter.texture(
            ResourceLocation.fromNamespaceAndPath(NimblePattern.MOD_ID, "textures/guis/loop_mode.png"),
            WIDTH,
            HEIGHT).src(0, 0, WIDTH, HEIGHT);

    public PatternLoopPanel(PatternTagTermScreen<?> screen, WidgetContainer widgets) {
        super(screen, widgets);
    }

    @Override
    public void drawBackgroundLayer(GuiGraphics guiGraphics, Rect2i bounds, Point mouse) {
        BG.dest(bounds.getX() + x, bounds.getY() + y).blit(guiGraphics);
    }

    @Override
    public ItemStack getTabIconItem() {
        return Items.RED_SANDSTONE.getDefaultInstance();
    }

    @Override
    public Component getTabTooltip() {
        return Component.translatable("gui.nimble_pattern.pattern_tag_terminal.tab.loop");
    }

}
