package com.ber.nimblePattern.client.gui.widgets;

import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AEKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public final class LoopSeedLostToast implements Toast {
    private static final long TIME_VISIBLE = 5000;
    private static final int TITLE_COLOR = 0xFF500050;
    private static final int TEXT_COLOR = 0xFF000000;

    private final AEKey what;
    private final List<FormattedCharSequence> lines;
    private final int height;
    private final String title;

    public LoopSeedLostToast(AEKey what) {
        this(what, "toast.nimble_pattern.loop_seed_lost_title", "toast.nimble_pattern.loop_seed_lost_content");
    }

    public LoopSeedLostToast(AEKey what, String title, String content) {
        this.what = what;
        this.title = title;
        var font = Minecraft.getInstance().font;
        var text = Component.translatable(
                content,
                AEKeyRendering.getDisplayName(what));
        this.lines = font.split(text, width() - 35);
        this.height = Toast.super.height() + (lines.size() - 1) * font.lineHeight;
    }

    @Override
    public Visibility render(GuiGraphics graphics, ToastComponent toasts, long timeSinceLastVisible) {
        var font = toasts.getMinecraft().font;
        graphics.blit(TEXTURE, 0, 0, 0, 32, width(), 8);
        int middleHeight = height - 16;
        for (int middleY = 0; middleY < middleHeight; middleY += 16) {
            int tileHeight = Math.min(middleHeight - middleY, 16);
            graphics.blit(TEXTURE, 0, 8 + middleY, 0, 40, width(), tileHeight);
        }
        graphics.blit(TEXTURE, 0, height - 8, 0, 56, width(), 8);
        graphics.drawString(font,
                Component.translatable(title),
                30, 7, TITLE_COLOR, false);
        int lineY = 18;
        for (var line : lines) {
            graphics.drawString(font, line, 30, lineY, TEXT_COLOR, false);
            lineY += font.lineHeight;
        }
        AEKeyRendering.drawInGui(toasts.getMinecraft(), graphics, 8, 8, what);
        return timeSinceLastVisible >= TIME_VISIBLE ? Visibility.HIDE : Visibility.SHOW;
    }

    @Override
    public int height() {
        return height;
    }
}
