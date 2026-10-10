package com.ber.nimblePattern.pattern.wrapper;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AEProcessingPattern;
import com.ber.nimblePattern.pattern.NimblePatternTag;
import com.ber.nimblePattern.pattern.loop.LoopPatternData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class NimbleProcessingPattern extends NimblePatternWrapper<AEProcessingPattern> {
    private final IInput[] inputs;
    private final boolean fakeMode;
    private final boolean fuzzyMode;
    private final GenericStack[] outputs;
    private final LoopPatternData loopData;

    public NimbleProcessingPattern(AEProcessingPattern pattern, AEItemKey definition) {
        super(pattern, definition);
        var stack = definition.toStack();
        this.fakeMode = NimblePatternTag.getFake(stack);
        this.fuzzyMode = NimblePatternTag.getFuzzy(stack);
        this.loopData = NimblePatternTag.getLoop(stack);
        if (loopData != null && loopData.isClosingPattern()) {
            this.inputs = this.loopData.externalInputs().stream()
                    .map(input -> (IInput) new LoopInput(input, fuzzyMode))
                    .toArray(IInput[]::new);
            this.outputs = new GenericStack[]{new GenericStack(this.loopData.entry().what(), this.loopData.netOutputAmount())};
        } else if (fuzzyMode) {
            this.inputs = Arrays.stream(pattern.getInputs()).map(FuzzyInput::new).toArray(IInput[]::new);
            this.outputs = pattern.getOutputs();
        } else {
            this.inputs = pattern.getInputs();
            this.outputs = pattern.getOutputs();
        }
    }

    public boolean getFakeMode() {
        return fakeMode;
    }

    public boolean getFuzzyMode() {
        return fuzzyMode;
    }

    public boolean getLoopMode() {
        return loopData != null;
    }

    public boolean isLoopComposite() {
        return loopData != null && loopData.isClosingPattern();
    }

    public LoopPatternData getLoopData() {
        return loopData;
    }

    @Override
    public IInput[] getInputs() {
        return inputs;
    }

    @Override
    public GenericStack[] getOutputs() {
        return outputs;
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

    private record LoopInput(GenericStack stack, boolean fuzzy) implements IInput {
        @Override
        public GenericStack[] getPossibleInputs() {
            return new GenericStack[]{new GenericStack(stack.what(), 1)};
        }

        @Override
        public long getMultiplier() {
            return stack().amount();
        }

        @Override
        public boolean isValid(AEKey input, Level level) {
            return fuzzy ? input.dropSecondary().equals(stack.what().dropSecondary()) :
                    input.equals(stack.what());
        }

        @Override
        public @Nullable AEKey getRemainingKey(AEKey template) {
            return null;
        }
    }
}
