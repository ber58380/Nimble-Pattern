package com.ber.nimblePattern.mixin.ae2;

import appeng.crafting.pattern.EncodedPatternItem;
import com.ber.nimblePattern.pattern.NimblePatternTag;
import com.ber.nimblePattern.pattern.upgrade.UpgradeState;
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
        if (NimblePatternTag.getFake(stack)) {
            lines.add(Component.translatable("tooltip.nimble_pattern.fake_pattern").withStyle(ChatFormatting.AQUA));
        }

        ResourceLocation condition = NimblePatternTag.getCondition(stack);
        if (condition == null) {
            return;
        }
        Component name = null;
        if (ForgeRegistries.ITEMS.containsKey(condition)) {
            name = ForgeRegistries.ITEMS.getValue(condition).getDescription().copy();
        } else if (ForgeRegistries.FLUIDS.containsKey(condition)) {
            name = ForgeRegistries.FLUIDS.getValue(condition).getFluidType().getDescription().copy();
        }
        Component conditionLine = Component.translatable("tooltip.nimble_pattern.condition", name != null ? name : Component.literal(condition.toString())).withStyle(ChatFormatting.GRAY);
        lines.add(conditionLine);

        UpgradeState state = NimblePatternTag.getStatus(stack);
        Component stateLine = Component.translatable("tooltip.nimble_pattern.state." + state.name()).withStyle(ChatFormatting.GRAY);
        lines.add(stateLine);
    }
}
