package com.ber.nimblePattern.pattern.upgrade;

import com.ber.nimblePattern.NimblePattern;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class UpgradeJeiPlugin implements IModPlugin {
    static IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(NimblePattern.MOD_ID, "upgrade_preview");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime value) {
        runtime = value;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }
}
