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
    private final String term;
    private static final Map<String, Component> cachedID = new HashMap<>();

    ConditionSearchPredicate(String term) {
        this.term = term.toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean test(ItemStack pattern) {
        if (term.isEmpty()) {
            return true;
        }
        String condition = NimblePatternTag.getCondition(pattern);
        if (condition.isEmpty()) {
            return false;
        }
        String lowerName = condition.toLowerCase(Locale.ROOT);
        // check if condition is an ID
        ResourceLocation id = ResourceLocation.tryParse(condition);
        if (id != null) {
            Component name = cachedID.computeIfAbsent(condition, key -> {
                if (ForgeRegistries.ITEMS.containsKey(id)) {
                    return Component.translatable(ForgeRegistries.ITEMS.getValue(id).getDescriptionId());
                } else if (ForgeRegistries.BLOCKS.containsKey(id)) {
                    return Component.translatable(ForgeRegistries.BLOCKS.getValue(id).getDescriptionId());
                } else if (ForgeRegistries.FLUIDS.containsKey(id)) {
                    return Component.translatable(ForgeRegistries.FLUIDS.getValue(id).getFluidType().getDescriptionId());
                } else {
                    return null;
                }
            });
            if (name != null) {
                lowerName = name.getString().toLowerCase(Locale.ROOT);
            }
        }
        return lowerName.contains(term) || PinInHelper.contains(lowerName, term);
    }
}
