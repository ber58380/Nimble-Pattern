package com.ber.nimblePattern.compat.extendedae;

import appeng.api.inventories.InternalInventory;
import appeng.api.parts.IPart;
import appeng.helpers.patternprovider.PatternContainer;
import com.glodblock.github.extendedae.common.parts.PartExPatternProvider;
import net.minecraft.core.Direction;
import net.minecraftforge.fml.ModList;

public final class ExtendedAECompat {
    public static final boolean LOADED = ModList.get().isLoaded("expatternprovider");

    public static Direction getSide(PatternContainer container) {
        if (container instanceof PartExPatternProvider provider) {
            return provider.getSide();
        }
        return null;
    }

    public static InternalInventory getTerminalPatternInventory(IPart part) {
        if (part instanceof PartExPatternProvider provider) {
            return provider.getTerminalPatternInventory();
        }
        return null;
    }

    private ExtendedAECompat() {
    }
}
