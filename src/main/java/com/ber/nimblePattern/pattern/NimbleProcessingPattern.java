package com.ber.nimblePattern.pattern;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AEProcessingPattern;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class NimbleProcessingPattern extends NimblePatternWrapper<AEProcessingPattern> {
    private final IInput[] inputs;
    private final boolean fakeMode;
    private final boolean fuzzyMode;

    public NimbleProcessingPattern(AEProcessingPattern pattern, boolean fuzzyMode) {
        super(pattern);
        this.fakeMode = NimblePatternTag.getFake(pattern.getDefinition().toStack());
        this.fuzzyMode = fuzzyMode;
        this.inputs = fuzzyMode ? Arrays.stream(pattern.getInputs()).map(FuzzyInput::new).toArray(IInput[]::new) : pattern.getInputs();
    }

    public boolean getFakeMode() {
        return fakeMode;
    }

    public boolean getFuzzyMode() {
        return fuzzyMode;
    }

    @Override
    public IInput[] getInputs() {
        return inputs;
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
