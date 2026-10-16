package com.ber.nimblePattern.mixin.ae2;

import appeng.api.stacks.AEItemKey;
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
        var info = NimblePatternTag.getInfo(stack);
        if (info.get("fake")) {
            lines.add(Component.translatable("tooltip.nimble_pattern.fake_pattern").withStyle(ChatFormatting.AQUA));
        }
        if (info.get("loop")) {
            lines.add(Component.translatable("tooltip.nimble_pattern.loop_pattern").withStyle(ChatFormatting.AQUA));
        }
        var tools = NimblePatternTag.getTool(stack);
        if (!tools.isEmpty()) {
            lines.add(Component.translatable("tooltip.nimble_pattern.tool_pattern").withStyle(ChatFormatting.AQUA));
            for (var tool : NimblePatternTag.getTool(stack)) {
                lines.add(Component.literal("  " + tool.amount() + " × ")
                        .append(((AEItemKey) tool.what()).toStack().getHoverName())
                        .withStyle(ChatFormatting.GRAY));
            }
        }
        var maxWaitingTicks = NimblePatternTag.getProbability(stack);
        if (maxWaitingTicks > 0) {
            lines.add(Component.translatable("tooltip.nimble_pattern.probability_pattern").withStyle(ChatFormatting.AQUA));
            lines.add(Component.literal("  ")
                    .append(Component.translatable("gui.nimble_pattern.pattern_tag_terminal.probability.waiting_time"))
                    .append(": " + maxWaitingTicks / 20L + "s")
                    .withStyle(ChatFormatting.GRAY));
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
        Component conditionLine = Component.translatable("tooltip.nimble_pattern.condition", name != null ? name : Component.literal(condition.toString())).withStyle(ChatFormatting.LIGHT_PURPLE);
        lines.add(conditionLine);

        UpgradeState state = NimblePatternTag.getStatus(stack);
        Component stateLine = Component.translatable("tooltip.nimble_pattern.state." + state.name()).withStyle(ChatFormatting.LIGHT_PURPLE);
        lines.add(stateLine);
    }
}
