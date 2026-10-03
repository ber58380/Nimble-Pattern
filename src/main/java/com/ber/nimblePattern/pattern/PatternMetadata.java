package com.ber.nimblePattern.pattern;

import appeng.api.stacks.GenericStack;
import com.ber.nimblePattern.probability.ProbabilityEncoding;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Immutable decoded metadata. Never parse item NBT in hash/equals or a CPU dispatch loop. */
public record PatternMetadata(boolean probability, ResourceLocation recipe, long cycles,
                              List<GenericStack> tools, LoopPatternData loop) {
    public static PatternMetadata read(ItemStack stack) {
        return new PatternMetadata(ProbabilityEncoding.marked(stack), ProbabilityEncoding.recipe(stack),
                ProbabilityEncoding.cycles(stack), ToolPatternData.read(stack), NimblePatternTag.getLoopData(stack));
    }
}
