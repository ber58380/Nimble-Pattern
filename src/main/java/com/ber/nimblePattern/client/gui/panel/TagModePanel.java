package com.ber.nimblePattern.client.gui.panel;

import appeng.client.Point;
import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.WidgetContainer;
import appeng.client.gui.widgets.TabButton;
import com.ber.nimblePattern.client.gui.PatternTagTermScreen;
import com.ber.nimblePattern.menu.PatternTagTermMenu;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

public abstract class TagModePanel implements ICompositeWidget {
    protected final PatternTagTermScreen<?> screen;
    protected final PatternTagTermMenu menu;
    protected final WidgetContainer widgets;
    protected boolean visible = false;
    protected int x;
    protected int y;
    protected static final int WIDTH = 88;
    protected static final int HEIGHT = 68;

    public TagModePanel(PatternTagTermScreen<?> screen, WidgetContainer widgets) {
        this.screen = screen;
        this.menu = screen.getMenu();
        this.widgets = widgets;
    }

    public abstract TabButton createTabButton(Button.OnPress onPress);

    public abstract Component getTabTooltip();

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
        return new Rect2i(x, y, WIDTH, HEIGHT);
    }

    @Override
    public final boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
