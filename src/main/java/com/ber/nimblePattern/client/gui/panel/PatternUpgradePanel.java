package com.ber.nimblePattern.client.gui.panel;

import appeng.api.stacks.GenericStack;
import appeng.client.Point;
import appeng.client.gui.WidgetContainer;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.TabButton;
import com.ber.nimblePattern.NimblePattern;
import com.ber.nimblePattern.client.gui.PatternTagTermScreen;
import com.ber.nimblePattern.client.gui.widgets.NimbleButton;
import com.ber.nimblePattern.client.gui.widgets.PanelTabButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import static com.ber.nimblePattern.menu.SlotSemantics.UPGRADE_CONDITION;

public class PatternUpgradePanel extends TagModePanel {
    private static final Blitter BG = Blitter.texture(
            ResourceLocation.fromNamespaceAndPath(NimblePattern.MOD_ID, "textures/guis/panel/upgrade/upgrade_panel.png"),
            WIDTH,
            HEIGHT).src(0, 0, WIDTH, HEIGHT);

    private final AETextField conditionTextField;
    private final Button upgradeClearButton;
    private final Button upgradeApplyButton;

    public PatternUpgradePanel(PatternTagTermScreen<?> screen, WidgetContainer widgets) {
        super(screen, widgets);

        this.conditionTextField = new AETextField(screen.getStyle(), Minecraft.getInstance().font, 0, 0, 0, 0);
        this.conditionTextField.setBordered(false);
        this.conditionTextField.setEditable(false);
        this.conditionTextField.active = false;
        this.conditionTextField.setTextColorUneditable(0xFFFFFF);
        this.conditionTextField.setMaxLength(Integer.MAX_VALUE);
        widgets.add("conditionTextField", this.conditionTextField);
        this.upgradeClearButton = new NimbleButton(0, 0, 0, 0, Component.translatable("gui.nimble_pattern.pattern_tag_terminal.clear"), button -> clearUpgradeCondition());
        widgets.add("upgradeClearButton", this.upgradeClearButton);
        this.upgradeApplyButton = new NimbleButton(0, 0, 0, 0, Component.translatable("gui.nimble_pattern.pattern_tag_terminal.apply"), button -> menu.applyUpgradeCondition());
        widgets.add("upgradeApplyButton", this.upgradeApplyButton);
    }

    @Override
    public void updateBeforeRender() {
        screen.repositionSlots(UPGRADE_CONDITION);

        var stack = menu.getConditionItemSlot().getItem();
        var generic = GenericStack.fromItemStack(stack);
        String name = generic == null ? "" : generic.what().getDisplayName().getString();
        if (!name.equals(conditionTextField.getValue())) {
            conditionTextField.setValue(name);
            conditionTextField.moveCursorToStart();
        }
    }

    @Override
    public void drawBackgroundLayer(GuiGraphics guiGraphics, Rect2i bounds, Point mouse) {
        BG.dest(bounds.getX() + x, bounds.getY() + y).blit(guiGraphics);
    }

    @Override
    public TabButton createTabButton(Button.OnPress onPress) {
        return new PanelTabButton(ResourceLocation.fromNamespaceAndPath(NimblePattern.MOD_ID, "textures/guis/panel/upgrade/upgrade_tab.png"), getTabTooltip(), onPress);
    }

    @Override
    public Component getTabTooltip() {
        return Component.translatable("gui.nimble_pattern.pattern_tag_terminal.tab.upgrade");
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        conditionTextField.setVisible(visible);
        upgradeClearButton.visible = visible;
        upgradeApplyButton.visible = visible;
        screen.setSlotsHidden(UPGRADE_CONDITION, !visible);
    }
    
    private void clearUpgradeCondition() {
        conditionTextField.setValue("");
        menu.getConditionItemSlot().set(ItemStack.EMPTY);
        menu.clearUpgradeCondition();
    }
}
