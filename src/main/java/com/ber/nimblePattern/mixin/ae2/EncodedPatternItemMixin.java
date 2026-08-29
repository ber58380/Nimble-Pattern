package com.ber.nimblePattern.mixin.ae2;

import appeng.crafting.pattern.EncodedPatternItem;
import com.ber.nimblePattern.pattern.NimblePatternTag;
import com.ber.nimblePattern.pattern.UpdateState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = EncodedPatternItem.class)
public abstract class EncodedPatternItemMixin {
    @Inject(method = "appendHoverText", at = @At("TAIL"))
    private void addUpdateInfo(ItemStack stack, Level level, List<Component> lines, TooltipFlag advancedTooltips, CallbackInfo ci) {
        String condition = NimblePatternTag.getCondition(stack);
        if (condition.isEmpty()) {
            return;
        }
        Component name = null;
        ResourceLocation id = ResourceLocation.tryParse(condition);
        if (id != null && ForgeRegistries.ITEMS.containsKey(id)) {
            name = ForgeRegistries.ITEMS.getValue(id).getDescription().copy();
        } else if (id != null && ForgeRegistries.BLOCKS.containsKey(id)) {
            name = ForgeRegistries.BLOCKS.getValue(id).getName().copy();
        } else if (id != null && ForgeRegistries.FLUIDS.containsKey(id)) {
            name = ForgeRegistries.FLUIDS.getValue(id).getFluidType().getDescription().copy();
        }
        Component conditionLine = Component.translatable("tooltip.nimble_pattern.condition", name != null ? name : Component.literal(condition)).withStyle(ChatFormatting.GRAY);
        lines.add(conditionLine);

        UpdateState state = NimblePatternTag.getStatus(stack);
        Component stateLine = Component.translatable("tooltip.nimble_pattern.state." + state.name()).withStyle(ChatFormatting.GRAY);
        lines.add(stateLine);
    }
}
