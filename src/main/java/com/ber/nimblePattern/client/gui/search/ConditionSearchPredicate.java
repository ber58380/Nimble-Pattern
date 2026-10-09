package com.ber.nimblePattern.client.gui.search;

import com.ber.nimblePattern.compat.jecharacters.PinInHelper;
import com.ber.nimblePattern.pattern.NimblePatternTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

final class ConditionSearchPredicate implements Predicate<ItemStack> {
    private static final Map<ResourceLocation, Component> cachedID = new HashMap<>();
    private final String term;

    ConditionSearchPredicate(String term) {
        this.term = term.toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean test(ItemStack pattern) {
        if (term.isEmpty()) {
            return true;
        }
        ResourceLocation condition = NimblePatternTag.getCondition(pattern);
        if (condition == null) {
            return false;
        }
        Component name = cachedID.computeIfAbsent(condition, id -> {
            if (ForgeRegistries.ITEMS.containsKey(id)) {
                return Component.translatable(ForgeRegistries.ITEMS.getValue(id).getDescriptionId());
            }
            if (ForgeRegistries.FLUIDS.containsKey(id)) {
                return Component.translatable(ForgeRegistries.FLUIDS.getValue(id).getFluidType().getDescriptionId());
            }
            return Component.literal(id.toString());
        });
        String lowerName = name.getString().toLowerCase(Locale.ROOT);
        return lowerName.contains(term) || PinInHelper.contains(lowerName, term);
    }
}
