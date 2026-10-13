package com.ber.nimblePattern.client.gui.widgets;

import appeng.client.gui.AESubScreen;
import appeng.client.gui.style.PaletteColor;
import com.ber.nimblePattern.client.gui.PatternTagTermScreen;
import com.ber.nimblePattern.menu.PatternTagTermMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class DialogBox<C extends PatternTagTermMenu> extends AESubScreen<C, PatternTagTermScreen<C>> {
    public static final String STYLE_PATH = "/screens/dialog_box.json";
    public static final int WIDTH = 219;
    public static final int HEIGHT = 87;
    @Nullable
    private final LoopDiagram loopDiagram;

    public DialogBox(PatternTagTermScreen<C> parent, Component title, Component message, LoopDiagramData loopDiagramData) {
        super(parent, STYLE_PATH);
        loopDiagram = loopDiagramData.nodes().isEmpty() ? null : new LoopDiagram(loopDiagramData);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;

        setTextContent("dialog_title", title);
        setTextContent("message", message);
        widgets.add("confirm", new NimbleButton(0, 0, 50, 20, Component.translatable("gui.ok"), button -> returnToParent()));

    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        if (loopDiagram != null) {
            loopDiagram.render(graphics, leftPos + 8, topPos + 25, imageWidth - 16,
                    mouseX, mouseY, getStyle().getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB());
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (loopDiagram != null && loopDiagram.mouseScrolled(mouseX, mouseY, delta,
                leftPos + 8, topPos + 25, imageWidth - 16)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        returnToParent();
    }

}
