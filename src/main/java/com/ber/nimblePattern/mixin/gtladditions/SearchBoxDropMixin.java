package com.ber.nimblePattern.mixin.gtladditions;

import com.ber.nimblePattern.client.gui.PatternTagTermScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * This is a compatible method for gtladditions, only highlight the search box.
 */
@Pseudo
@Mixin(targets = "com.gtladd.gtladditions.integration.SearchBoxDrop", remap = false)
public abstract class SearchBoxDropMixin {
    @Inject(method = "getTextFieldAreas", at = @At("RETURN"), cancellable = true, require = 0)
    private static void filterTerminalSearchTargets(Screen screen, CallbackInfoReturnable<List<EditBox>> cir) {
        if (screen instanceof PatternTagTermScreen<?> terminal) {
            cir.setReturnValue(cir.getReturnValue().stream().filter(terminal::isJeiSearchField).toList());
        }
    }

    @Inject(method = "fillTextField", at = @At("HEAD"), cancellable = true, require = 0)
    private static void rejectPanelTextTargets(Screen screen, EditBox field, String text,
                                               CallbackInfoReturnable<Boolean> cir) {
        // A drag may have captured its target before the panel changed.
        if (screen instanceof PatternTagTermScreen<?> terminal && !terminal.isJeiSearchField(field)) {
            cir.setReturnValue(false);
        }
    }
}
