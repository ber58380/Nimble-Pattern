package com.ber.nimblePattern.client.gui.widgets;

import appeng.api.stacks.GenericStack;
import appeng.client.gui.AESubScreen;
import appeng.client.gui.me.common.ClientDisplaySlot;
import appeng.menu.SlotSemantics;
import com.ber.nimblePattern.client.gui.PatternTagTermScreen;
import com.ber.nimblePattern.menu.PatternTagTermMenu;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class DialogBox<C extends PatternTagTermMenu> extends AESubScreen<C, PatternTagTermScreen<C>> {
    public static final String STYLE_PATH = "/screens/dialog_box.json";
    public static final int WIDTH = 219;
    public static final int HEIGHT = 87;

    public DialogBox(PatternTagTermScreen<C> parent, Component title, Component message) {
        this(parent, title, message, null);
    }

    public DialogBox(PatternTagTermScreen<C> parent, Component title, Component message,
                     @Nullable GenericStack material) {
        super(parent, STYLE_PATH);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;

        setTextContent("dialog_title", title);
        setTextContent("message", message);
        widgets.add("confirm", new NimbleButton(0, 0, 50, 20, Component.translatable("gui.ok"), button -> returnToParent()));

        if (material != null) {
            addClientSideSlot(new ClientDisplaySlot(material), SlotSemantics.MISSING_INGREDIENT);
        }
    }

    @Override
    public void onClose() {
        returnToParent();
    }

}
