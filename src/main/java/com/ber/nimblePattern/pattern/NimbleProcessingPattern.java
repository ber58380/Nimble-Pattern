package com.ber.nimblePattern.pattern;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AEProcessingPattern;
import net.minecraft.world.item.BookItem;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class NimbleProcessingPattern implements IPatternDetails {
    private final AEProcessingPattern pattern;
    private final IInput[] fuzzyInputs;
    private final boolean fakeMode;
    private final boolean fuzzyMode;

    public NimbleProcessingPattern(AEProcessingPattern pattern, boolean fuzzyMode) {
        this.pattern = pattern;
        if (fuzzyMode) {
            this.fuzzyInputs = Arrays.stream(pattern.getInputs())
                    .map(FuzzyInput::new)
                    .toArray(IInput[]::new);
        } else {
            this.fuzzyInputs = pattern.getInputs();
        }
        this.fakeMode = isFakePattern();
        this.fuzzyMode = fuzzyMode;
    }

    public AEProcessingPattern getPattern() {
        return pattern;
    }

    public boolean getFakeMode() {
        return fakeMode;
    }

    public boolean getFuzzyMode() {
        return fuzzyMode;
    }

    private boolean isFakePattern() {
        var outputs = pattern.getOutputs();
        // If the output of pattern is only a renamed book, it's a fake pattern
        if (outputs.length != 1) {
            return false;
        }
        if (!(outputs[0].what() instanceof AEItemKey key)) {
            return false;
        }
        var stack = key.toStack();
        if (!stack.hasCustomHoverName()) {
            return false;
        }
        var item = stack.getItem();
        return item instanceof BookItem;
    }

    @Override
    public AEItemKey getDefinition() {
        return pattern.getDefinition();
    }

    @Override
    public IInput[] getInputs() {
        return fuzzyInputs;
    }

    @Override
    public GenericStack[] getOutputs() {
        return pattern.getOutputs();
    }

    @Override
    public int hashCode() {
        return pattern.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NimbleProcessingPattern npp) {
            return pattern.equals(npp.pattern);
        }
        return pattern.equals(obj);
    }

    private record FuzzyInput(IInput pattern) implements IInput {
        @Override
        public GenericStack[] getPossibleInputs() {
            return pattern.getPossibleInputs();
        }

        @Override
        public long getMultiplier() {
            return pattern.getMultiplier();
        }

        @Override
        public boolean isValid(AEKey input, Level level) {
            // always true, don't let AE filter items having different NBT
            return true;
        }

        @Nullable
        @Override
        public AEKey getRemainingKey(AEKey template) {
            return pattern.getRemainingKey(template);
        }
    }
}
