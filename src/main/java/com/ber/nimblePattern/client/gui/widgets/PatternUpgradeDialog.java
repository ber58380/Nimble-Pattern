package com.ber.nimblePattern.client.gui.widgets;

import appeng.api.stacks.AEFluidKey;
import appeng.client.gui.AESubScreen;
import com.ber.nimblePattern.client.gui.PatternTagTermScreen;
import com.ber.nimblePattern.menu.PatternTagTermMenu;
import com.ber.nimblePattern.pattern.upgrade.RecipePreviewView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Optional;

public final class PatternUpgradeDialog<C extends PatternTagTermMenu> extends AESubScreen<C, PatternTagTermScreen<C>> {
    private NimbleButton confirm;
    private NimbleButton skip;
    private long revision;
    private boolean waiting;
    private RecipePreviewView recipeView;
    private int scroll;
    private int recipeX;
    private int recipeY;
    private int pressedButton = -1;
    private static final int TEXT = 0x413F54;
    private static final int HEADER = 55;
    private static final int FOOTER = 55;

    public PatternUpgradeDialog(PatternTagTermScreen<C> parent) {
        super(parent, "/screens/pattern_upgrade_dialog.json");
    }

    @Override
    protected void init() {
        recipeView = RecipePreviewView.create(menu.upgradePreview);
        imageWidth = Math.min(Math.max(280, recipeView == null ? 280 : recipeView.width() + 28), width - 16);
        imageHeight = Math.min(recipeView == null ? 220 : recipeView.height() + HEADER + FOOTER + 12, height - 16);
        super.init();
        confirm = addRenderableWidget(new NimbleButton(leftPos + imageWidth / 2 - 72,
                topPos + imageHeight - 28, 64, 20, tr("confirm"), button -> {
            waiting = true;
            menu.confirmRecipeUpgrade(revision);
        }));
        skip = addRenderableWidget(new NimbleButton(leftPos + imageWidth / 2 + 8,
                topPos + imageHeight - 28, 64, 20, tr("skip"), button -> {
            waiting = true;
            menu.skipRecipeUpgrade(revision);
        }));
        positionRecipe();
    }

    private void positionRecipe() {
        if (recipeView == null) return;
        recipeX = leftPos + Math.max(8, (imageWidth - recipeView.width()) / 2);
        recipeY = topPos + HEADER + 5 - scroll;
        recipeView.position(recipeX, recipeY);
    }

    private boolean overRecipe(double x, double y) {
        return recipeView != null && x >= leftPos + 7 && x < leftPos + imageWidth - 7
                && y >= topPos + HEADER && y < topPos + imageHeight - FOOTER;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        var data = menu.upgradePreview;
        if (data.revision() == 0) {
            returnToParent();
            return;
        }
        if (revision != data.revision()) {
            revision = data.revision();
            getParent().setUpgradePreviewRevision(revision);
            waiting = false;
            scroll = 0;
            pressedButton = -1;
            rebuildWidgets();
        }
        confirm.active = !waiting && recipeView != null;
        skip.active = !waiting;
        super.render(g, mouseX, mouseY, partialTicks);
        if (recipeView != null) {
            positionRecipe();
            boolean inside = overRecipe(mouseX, mouseY);
            g.enableScissor(leftPos + 7, topPos + HEADER, leftPos + imageWidth - 7, topPos + imageHeight - FOOTER);
            recipeView.draw(g, inside ? mouseX : -10000, inside ? mouseY : -10000);
            g.disableScissor();
            if (inside) recipeView.overlays(g, mouseX, mouseY);
        }
        if (mouseX >= leftPos + 10 && mouseX < leftPos + imageWidth - 10
                && mouseY >= topPos + imageHeight - 49 && mouseY < topPos + imageHeight - 32) {
            var lines = new ArrayList<Component>();
            lines.add(tr("encoded_inputs"));
            for (var stack : data.encodedInputs())
                lines.add(Component.literal(stack.amount() + (stack.what() instanceof AEFluidKey ? " mB × " : " × ")).append(stack.what().getDisplayName()));
            lines.add(tr("native_note"));
            g.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (recipeView != null) recipeView.tick();
    }

    @Override
    public void drawBG(GuiGraphics g, int x, int y, int mouseX, int mouseY, float partialTicks) {
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFFF2F2F2);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFCBCCD4);
    }

    @Override
    public void drawFG(GuiGraphics g, int offsetX, int offsetY, int mouseX, int mouseY) {
        var data = menu.upgradePreview;
        var progressTitle = tr("progress", data.index(), data.total());
        g.drawString(font, progressTitle, (imageWidth - font.width(progressTitle)) / 2, 8, TEXT, false);
        if (recipeView != null) {
            g.drawString(font, font.plainSubstrByWidth(recipeView.title().getString(), imageWidth - 24), 12, 23, TEXT, false);
            var output = data.primaryOutput();
            if (output != null) {
                g.drawString(font, font.plainSubstrByWidth(
                                tr("yield", output.what().getDisplayName(),
                                        data.oldAmount() + (output.what() instanceof AEFluidKey ? " mB" : ""),
                                        output.amount() + (output.what() instanceof AEFluidKey ? " mB" : "")).getString(), imageWidth - 24),
                        12, 38, TEXT, false);
            }
        } else {
            g.drawWordWrap(font, tr("jei_unavailable"), 12, HEADER + 8, imageWidth - 24, TEXT);
        }
        g.drawString(font, tr("encoded_inputs_hint"), 12, imageHeight - 46, TEXT, false);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (overRecipe(x, y) && recipeView.click(x - recipeX, y - recipeY, button, true)) {
            pressedButton = button;
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (pressedButton == button) {
            pressedButton = -1;
            if (overRecipe(x, y)) recipeView.click(x - recipeX, y - recipeY, button, false);
            return true;
        }
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        if (overRecipe(x, y)) {
            if (!recipeView.scroll(x - recipeX, y - recipeY, delta)) {
                int max = Math.max(0, recipeView.height() + 10 - (imageHeight - HEADER - FOOTER));
                scroll = Math.max(0, Math.min(max, scroll - (int) (delta * 20)));
                positionRecipe();
            }
            return true;
        }
        return super.mouseScrolled(x, y, delta);
    }

    @Override
    public void onClose() {
        if (waiting) return;
        menu.cancelRecipeUpgrade(revision);
        getParent().setUpgradePreviewRevision(revision);
        returnToParent();
    }

    private static Component tr(String key, Object... args) {
        return Component.translatable("gui.nimble_pattern.recommend_upgrade_recipes." + key, args);
    }
}
