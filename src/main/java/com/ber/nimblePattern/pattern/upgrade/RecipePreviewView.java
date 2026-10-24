package com.ber.nimblePattern.pattern.upgrade;

import com.mojang.blaze3d.platform.InputConstants;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiKeyMapping;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class RecipePreviewView {
    private final IRecipeLayoutDrawable<?> layout;

    private RecipePreviewView(IRecipeLayoutDrawable<?> layout) {
        this.layout = layout;
    }

    @Nullable
    public static RecipePreviewView create(UpgradePreview preview) {
        if (UpgradeJeiPlugin.runtime == null) {
            return null;
        }
        var typeId = preview.recipeTypeId();
        var id = preview.recipeId();
        if (typeId == null || id == null) {
            return null;
        }
        return UpgradeJeiPlugin.runtime.getRecipeManager().getRecipeType(typeId)
                .map(type -> create(type, id)).orElse(null);
    }

    private static <T> RecipePreviewView create(RecipeType<T> type, ResourceLocation id) {
        var manager = UpgradeJeiPlugin.runtime.getRecipeManager();
        var category = manager.getRecipeCategory(type);
        var recipe = manager.createRecipeLookup(type).get()
                .filter(r -> id.equals(category.getRegistryName(r))).findFirst().orElse(null);
        if (recipe == null) {
            return null;
        }
        return manager.createRecipeLayoutDrawable(category, recipe,
                        UpgradeJeiPlugin.runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup())
                .map(RecipePreviewView::new).orElse(null);
    }

    public int width() {
        return layout.getRect().getWidth();
    }

    public int height() {
        return layout.getRect().getHeight();
    }

    public Component title() {
        return layout.getRecipeCategory().getTitle();
    }

    public void position(int x, int y) {
        layout.setPosition(x, y);
    }

    public void draw(GuiGraphics g, int x, int y) {
        layout.drawRecipe(g, x, y);
    }

    public void overlays(GuiGraphics g, int x, int y) {
        layout.drawOverlays(g, x, y);
    }

    public void tick() {
        layout.tick();
    }

    public boolean click(double x, double y, int button, boolean simulate) {
        return layout.getInputHandler().handleInput(x, y, new MouseInput(button, simulate));
    }

    public boolean scroll(double x, double y, double delta) {
        return layout.getInputHandler().handleMouseScrolled(x, y, delta);
    }

    private record MouseInput(int button, boolean simulate) implements IJeiUserInput {
        public InputConstants.Key getKey() {
            return InputConstants.Type.MOUSE.getOrCreate(button);
        }

        public int getModifiers() {
            return 0;
        }

        public boolean isSimulate() {
            return simulate;
        }

        public boolean is(KeyMapping mapping) {
            return mapping.isActiveAndMatches(getKey());
        }

        public boolean is(IJeiKeyMapping mapping) {
            return mapping.isActiveAndMatches(getKey());
        }
    }
}
