package com.ber.nimblePattern.client.gui;

import appeng.api.stacks.GenericStack;
import appeng.client.Point;
import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.WidgetContainer;
import appeng.client.gui.widgets.Scrollbar;
import com.ber.nimblePattern.menu.PatternUpgradeTermMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

import static com.ber.nimblePattern.menu.PatternUpgradeTermMenu.CONDITION_ITEM;
import static com.ber.nimblePattern.menu.PatternUpgradeTermMenu.INPUT_PATTERN;
import static com.ber.nimblePattern.parts.PatternUpgradeLogic.INPUT_PATTERN_TOTAL_ROWS;
import static com.ber.nimblePattern.parts.PatternUpgradeLogic.INPUT_PATTERN_VISIBLE_ROWS;

public class PatternUpgradePanel implements ICompositeWidget {
    protected final PatternUpgradeTermScreen<?> screen;
    protected final PatternUpgradeTermMenu menu;
    protected final WidgetContainer widgets;
    protected int x;
    protected int y;

    private final Scrollbar scrollbar;
    private final PromptTextField conditionTextField;
    // true means code is doing sync (fakeSlot -> textField), not allow clear in setResponder (textField -> EMPTY)
    private boolean syncTextAndSlot = false;
    private final Button clearButton;
    private final Button applyButton;

    public PatternUpgradePanel(PatternUpgradeTermScreen<?> screen, WidgetContainer widgets) {
        this.screen = screen;
        this.menu = screen.getMenu();
        this.widgets = widgets;
        this.scrollbar = widgets.addScrollBar("inputPatternScrollbar", Scrollbar.SMALL);
        this.scrollbar.setRange(0, INPUT_PATTERN_TOTAL_ROWS - INPUT_PATTERN_VISIBLE_ROWS, 3);
        this.scrollbar.setCaptureMouseWheel(false);
        this.conditionTextField = new PromptTextField(screen.getStyle(), Minecraft.getInstance().font, 0, 0, 0, 0);
        this.conditionTextField.setPlaceholder(Component.translatable("gui.nimble_pattern.pattern_upgrade_terminal.conditions"));
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
        this.clearButton = new NimbleButton(0, 0, 0, 0, Component.translatable("gui.nimble_pattern.pattern_upgrade_terminal.clear"), button -> clear());
        widgets.add("clearButton", this.clearButton);
        this.applyButton = new NimbleButton(0, 0, 0, 0, Component.translatable("gui.nimble_pattern.pattern_upgrade_terminal.apply"), button -> apply());
        widgets.add("applyButton", this.applyButton);
    }

    @Override
    public void setPosition(Point position) {
        x = position.getX();
        y = position.getY();
    }

    @Override
    public void setSize(int width, int height) {
    }

    @Override
    public Rect2i getBounds() {
        return new Rect2i(x, y, 126, 68);
    }

    @Override
    public final boolean isVisible() {
        return true;
    }

    @Override
    public void updateBeforeRender() {
        screen.repositionSlots(INPUT_PATTERN);
        screen.repositionSlots(CONDITION_ITEM);
        for (int i = 0; i < menu.getInputPatternSlots().length; i++) {
            var slot = menu.getInputPatternSlots()[i];
            var effectiveRow = (i / 3) - scrollbar.getCurrentScroll();
            slot.setActive(effectiveRow >= 0 && effectiveRow < INPUT_PATTERN_VISIBLE_ROWS);
            slot.y -= scrollbar.getCurrentScroll() * 18;
        }

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
    public boolean onMouseWheel(Point mousePos, double delta) {
        return scrollbar.onMouseWheel(mousePos, delta);
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
