package com.ber.nimblePattern.client.gui.panel;

import appeng.api.stacks.GenericStack;
import appeng.client.Point;
import appeng.client.gui.WidgetContainer;
import appeng.client.gui.style.Blitter;
import com.ber.nimblePattern.NimblePattern;
import com.ber.nimblePattern.client.gui.widgets.NimbleButton;
import com.ber.nimblePattern.client.gui.PatternTagTermScreen;
import com.ber.nimblePattern.client.gui.widgets.PromptTextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

import static com.ber.nimblePattern.menu.PatternTagTermMenu.CONDITION_ITEM;
public class PatternUpgradePanel extends TagModePanel {
    private static final int WIDTH = 88;
    private static final int HEIGHT = 68;
    private static final Blitter BG = Blitter.texture(
            ResourceLocation.fromNamespaceAndPath(NimblePattern.MOD_ID, "textures/guis/upgrade_mode.png"),
            WIDTH,
            HEIGHT).src(0, 0, WIDTH, HEIGHT);

    private final PromptTextField conditionTextField;
    // true means code is doing sync (fakeSlot -> textField), not allow clear in setResponder (textField -> EMPTY)
    private boolean syncTextAndSlot = false;
    private final Button clearButton;
    private final Button applyButton;

    public PatternUpgradePanel(PatternTagTermScreen<?> screen, WidgetContainer widgets) {
        super(screen, widgets);

        this.conditionTextField = new PromptTextField(screen.getStyle(), Minecraft.getInstance().font, 0, 0, 0, 0);
        this.conditionTextField.setPlaceholder(Component.translatable("gui.nimble_pattern.pattern_tag_terminal.conditions"));
        conditionTextField.setResponder(text -> {
            conditionTextField.update();
            if (syncTextAndSlot) {
                return;
            }
            var stack = menu.getConditionItemSlot().getItem();
            if (!stack.isEmpty()) {
                var unwrapped = GenericStack.unwrapItemStack(stack);
                String slotName = unwrapped == null ? stack.getHoverName().getString() : unwrapped.what().getDisplayName().getString();
                if (!slotName.equals(text)) {
                    menu.getConditionItemSlot().set(ItemStack.EMPTY);
                }
            }
        });
        widgets.add("conditionTextField", this.conditionTextField);
        this.clearButton = new NimbleButton(0, 0, 0, 0, Component.translatable("gui.nimble_pattern.pattern_tag_terminal.clear"), button -> clear());
        widgets.add("clearButton", this.clearButton);
        this.applyButton = new NimbleButton(0, 0, 0, 0, Component.translatable("gui.nimble_pattern.pattern_tag_terminal.apply"), button -> apply());
        widgets.add("applyButton", this.applyButton);
    }

    @Override
    public void updateBeforeRender() {
        screen.repositionSlots(CONDITION_ITEM);

        var stack = menu.getConditionItemSlot().getItem();
        if (!stack.isEmpty()) {
            // Fluid is wrapped
            var unwrapped = GenericStack.unwrapItemStack(stack);
            String name = unwrapped == null ? stack.getHoverName().getString() : unwrapped.what().getDisplayName().getString();
            if (!name.equals(conditionTextField.getValue())) {
                syncTextAndSlot = true;
                conditionTextField.setValue(name);
                conditionTextField.moveCursorToEnd();
                syncTextAndSlot = false;
                conditionTextField.update();
            }
        } else { // clear the text field when the slot is cleared
            if (!conditionTextField.getValue().isEmpty()) {
                syncTextAndSlot = true;
                conditionTextField.setValue("");
                syncTextAndSlot = false;
                conditionTextField.update();
            }
        }
    }

    @Override
    public void drawBackgroundLayer(GuiGraphics guiGraphics, Rect2i bounds, Point mouse) {
        BG.dest(bounds.getX() + x, bounds.getY() + y).blit(guiGraphics);
    }

    @Override
    public ItemStack getTabIconItem() {
        return Items.FURNACE.getDefaultInstance();
    }

    @Override
    public Component getTabTooltip() {
        return Component.translatable("gui.nimble_pattern.pattern_tag_terminal.tab.upgrade");
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        conditionTextField.setVisible(visible);
        clearButton.visible = visible;
        applyButton.visible = visible;
        screen.setSlotsHidden(CONDITION_ITEM, !visible);
    }

    public void setHistory(Set<String> history) {
        conditionTextField.setHistory(history);
    }

    private void apply() {
        var stack = menu.getConditionItemSlot().getItem();
        String condition;
        if (stack.isEmpty()) {
            condition = conditionTextField.getValue().trim();
        } else {
            var generic = GenericStack.fromItemStack(stack);
            condition = generic == null ? String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem())) : generic.what().getId().toString();
        }
        if (condition.isEmpty() || condition.equals("null")) {
            return;
        }
        menu.applyCondition(condition);
    }

    private void clear() {
        syncTextAndSlot = true;
        conditionTextField.setValue("");
        menu.getConditionItemSlot().set(ItemStack.EMPTY);
        menu.clearCondition();
        syncTextAndSlot = false;
    }
}
