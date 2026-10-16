package com.ber.nimblePattern.client.gui.panel;

import appeng.api.stacks.GenericStack;
import appeng.client.Point;
import appeng.client.gui.WidgetContainer;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.TabButton;
import appeng.core.definitions.AEItems;
import com.ber.nimblePattern.NimblePattern;
import com.ber.nimblePattern.client.gui.PatternTagTermScreen;
import com.ber.nimblePattern.client.gui.widgets.NimbleButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import static com.ber.nimblePattern.menu.SlotSemantics.TOOL_INPUT;

public class ToolPatternPanel extends TagModePanel {
    private static final Blitter BG = Blitter.texture(
            ResourceLocation.fromNamespaceAndPath(NimblePattern.MOD_ID, "textures/guis/panel/tool/tool_panel.png"),
            WIDTH,
            HEIGHT
    ).src(0, 0, WIDTH, HEIGHT);

    private final AETextField toolTextField;
    private final Button toolApplyButton;
    private final Button toolClearButton;

    public ToolPatternPanel(PatternTagTermScreen<?> screen, WidgetContainer widgets) {
        super(screen, widgets);

        this.toolTextField = new AETextField(screen.getStyle(), Minecraft.getInstance().font, 0, 0, 0, 0);
        this.toolTextField.setBordered(false);
        this.toolTextField.setEditable(false);
        this.toolTextField.active = false;
        this.toolTextField.setTextColorUneditable(0xFFFFFF);
        this.toolTextField.setMaxLength(Integer.MAX_VALUE);
        this.toolApplyButton = new NimbleButton(0, 0, 0, 0, Component.translatable("gui.nimble_pattern.pattern_tag_terminal.apply"), button -> menu.applyTool());
        this.toolClearButton = new NimbleButton(0, 0, 0, 0, Component.translatable("gui.nimble_pattern.pattern_tag_terminal.clear"), button -> menu.clearTool());
        widgets.add("toolTextField", toolTextField);
        widgets.add("toolApplyButton", this.toolApplyButton);
        widgets.add("toolClearButton", this.toolClearButton);
    }

    @Override
    public void updateBeforeRender() {
        screen.repositionSlots(TOOL_INPUT);

        var stack = menu.getToolInputSlot().getItem();
        var generic = GenericStack.fromItemStack(stack);
        String name = generic == null ? "" : generic.what().getDisplayName().getString();
        if (!name.equals(toolTextField.getValue())) {
            toolTextField.setValue(name);
            toolTextField.moveCursorToStart();
        }
    }

    @Override
    public void drawBackgroundLayer(GuiGraphics guiGraphics, Rect2i bounds, Point mouse) {
        BG.dest(bounds.getX() + x, bounds.getY() + y).blit(guiGraphics);
    }

    @Override
    public TabButton createTabButton(Button.OnPress onPress) {
        return new TabButton(AEItems.NETHER_QUARTZ_WRENCH.stack(), getTabTooltip(), onPress);
    }

    @Override
    public Component getTabTooltip() {
        return Component.translatable("gui.nimble_pattern.pattern_tag_terminal.tab.tool");
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        toolTextField.setVisible(visible);
        toolApplyButton.visible = visible;
        toolClearButton.visible = visible;
        screen.setSlotsHidden(TOOL_INPUT, !visible);
    }

}
