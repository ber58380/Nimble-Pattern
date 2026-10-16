package com.ber.nimblePattern.client.gui.panel;

import appeng.client.gui.WidgetContainer;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.TabButton;
import com.ber.nimblePattern.client.gui.PatternTagTermScreen;
import com.ber.nimblePattern.client.gui.widgets.NimbleButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

public class ProbabilityPatternPanel extends TagModePanel {
    private final AETextField probabilityTextField;
    private final Button probabilityApplyButton;
    private final Button probabilityClearButton;

    public ProbabilityPatternPanel(PatternTagTermScreen<?> screen, WidgetContainer widgets) {
        super(screen, widgets);

        this.probabilityTextField = new AETextField(screen.getStyle(), Minecraft.getInstance().font, 0, 0, 0, 0);
        this.probabilityTextField.setBordered(false);
        this.probabilityTextField.setMaxLength(Integer.MAX_VALUE);
        this.probabilityTextField.setFilter(text -> text.chars().allMatch(character -> character >= '0' && character <= '9'));
        this.probabilityTextField.setPlaceholder(Component.translatable("gui.nimble_pattern.pattern_tag_terminal.probability.waiting_time"));
        this.probabilityApplyButton = new NimbleButton(0, 0, 0, 0, Component.translatable("gui.nimble_pattern.pattern_tag_terminal.apply"), button -> menu.applyProbability(probabilityTextField.getValue()));
        this.probabilityClearButton = new NimbleButton(0, 0, 0, 0, Component.translatable("gui.nimble_pattern.pattern_tag_terminal.clear"), button -> {
            this.probabilityTextField.setValue("");
            menu.clearProbability();
        });
        widgets.add("probabilityTextField", probabilityTextField);
        widgets.add("probabilityApplyButton", this.probabilityApplyButton);
        widgets.add("probabilityClearButton", this.probabilityClearButton);
    }

    @Override
    public TabButton createTabButton(Button.OnPress onPress) {
        return new TabButton(Items.SUSPICIOUS_STEW.getDefaultInstance(),
                getTabTooltip(),
                onPress);
    }

    @Override
    public Component getTabTooltip() {
        return Component.translatable("gui.nimble_pattern.pattern_tag_terminal.tab.probability");
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        probabilityTextField.setVisible(visible);
        probabilityApplyButton.visible = visible;
        probabilityClearButton.visible = visible;
    }
}
