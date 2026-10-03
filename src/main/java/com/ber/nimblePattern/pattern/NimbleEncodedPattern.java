package com.ber.nimblePattern.pattern;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import net.minecraft.world.item.BookItem;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class NimbleEncodedPattern implements IPatternDetails {
    private final IPatternDetails pattern;
    private final IInput[] fuzzyInputs;
    private final GenericStack[] outputs;
    private final boolean fakeMode;
    private final boolean fuzzyMode;
    private final LoopPatternData loopData;
    private final AEItemKey definition;
    private final AEItemKey identity;
    private final PatternMetadata metadata;
    private final int identityHash;

    public static NimbleEncodedPattern wrap(IPatternDetails pattern, boolean fuzzy) {
        return pattern instanceof appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern assembler
                ? new NimbleAssemblerPattern(assembler, fuzzy)
                : new NimbleEncodedPattern(pattern, fuzzy);
    }

    public NimbleEncodedPattern(IPatternDetails pattern, boolean fuzzyMode) {
        this(pattern, fuzzyMode, null);
    }

    /**
     * Wraps a processing pattern while retaining loop metadata recovered from its physical encoded pattern.
     * Some third-party providers rebuild processing patterns before exposing them to AE and therefore lose
     * non-AE NBT stored on the definition stack.
     */
    public NimbleEncodedPattern(IPatternDetails pattern, boolean fuzzyMode, @Nullable LoopPatternData loopData) {
        this(pattern, fuzzyMode, loopData, pattern.getDefinition());
    }

    protected NimbleEncodedPattern(IPatternDetails pattern, boolean fuzzyMode,
            @Nullable LoopPatternData loopData, AEItemKey definition) {
        this.pattern = pattern;
        var stack = definition.toStack();
        var root = stack.getTagElement("nimble_pattern");
        fuzzyMode |= root != null && root.getBoolean("execution_fuzzy");
        final boolean effectiveFuzzy = fuzzyMode;
        if (fuzzyMode) stack.getOrCreateTagElement("nimble_pattern").putBoolean("execution_fuzzy", true);
        if (loopData != null && NimblePatternTag.getLoopData(stack) == null) NimblePatternTag.tagLoop(stack, loopData);
        this.definition = AEItemKey.of(stack);
        this.metadata = PatternMetadata.read(stack);
        // Management annotations do not change execution. Updating a condition must not strand a running job.
        var identityRoot = stack.getTagElement("nimble_pattern");
        if (identityRoot != null) {
            identityRoot.remove("source");
            identityRoot.remove("update");
            if (identityRoot.isEmpty()) stack.removeTagKey("nimble_pattern");
        }
        this.identity = AEItemKey.of(stack);
        this.identityHash = identity.hashCode();
        loopData = metadata.loop();
        // One-pattern loops belong to a separate future feature. Ignore legacy one-pattern loop tags at runtime too.
        this.loopData = loopData != null && loopData.size() >= 2 ? loopData : null;
        if (this.loopData != null && this.loopData.isClosingPattern()) {
            this.fuzzyInputs = this.loopData.externalInputs().stream()
                    .map(input -> (IInput) new LoopInput(input, effectiveFuzzy))
                    .toArray(IInput[]::new);
            this.outputs = new GenericStack[] {
                    new GenericStack(this.loopData.entry().what(), this.loopData.netOutputAmount())
            };
        } else if (fuzzyMode) {
            this.fuzzyInputs = Arrays.stream(pattern.getInputs())
                    .map(FuzzyInput::new)
                    .toArray(IInput[]::new);
            this.outputs = pattern.getOutputs();
        } else {
            this.fuzzyInputs = pattern.getInputs();
            this.outputs = pattern.getOutputs();
        }
        this.fakeMode = isFakePattern();
        this.fuzzyMode = fuzzyMode;
    }

    public IPatternDetails getPattern() {
        return pattern;
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

    private boolean isFakePattern() {
        if (metadata.probability()) return false;
        // Broadening the wrapper must not turn real crafting recipes into fake processing recipes.
        if (!(pattern instanceof appeng.crafting.pattern.AEProcessingPattern)) return false;
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
        return definition;
    }

    public PatternMetadata metadata() { return metadata; }

    @Override
    public IInput[] getInputs() {
        return fuzzyInputs;
    }

    @Override
    public GenericStack[] getOutputs() {
        return outputs;
    }

    @Override
    public boolean supportsPushInputsToExternalInventory() {
        return pattern.supportsPushInputsToExternalInventory();
    }

    @Override
    public int hashCode() {
        return identityHash;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof NimbleEncodedPattern other && identity.equals(other.identity);
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
            return new GenericStack[] { new GenericStack(stack.what(), 1) };
        }

        @Override
        public long getMultiplier() {
            return stack.amount();
        }

        @Override
        public boolean isValid(AEKey input, Level level) {
            return fuzzy
                    ? input.dropSecondary().equals(stack.what().dropSecondary())
                    : input.equals(stack.what());
        }

        @Nullable
        @Override
        public AEKey getRemainingKey(AEKey template) {
            return null;
        }
    }
}
