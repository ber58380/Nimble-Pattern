package com.ber.nimblePattern.compat.jecharacters;

import me.towdium.pinin.PinIn;
import net.minecraftforge.fml.ModList;

public final class PinInHelper {
    public static final boolean LOADED = ModList.get().isLoaded("jecharacters");

    private static class Holder {
        static final PinIn PIN_IN = new PinIn();
    }

    public static boolean contains(String name, String query) {
        if (!LOADED) {
            return false;
        }
        return Holder.PIN_IN.contains(name, query);
    }

    private PinInHelper() {
    }
}
