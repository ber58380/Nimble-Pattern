package com.ber.nimblePattern.pattern.wrapper;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;

public class NimblePatternWrapper<WrappedPattern extends IPatternDetails> implements IPatternDetails {
    protected final WrappedPattern pattern;
    private final AEItemKey definition;
    private final AEItemKey identity;
    private int hashCode;

    public NimblePatternWrapper(WrappedPattern pattern, AEItemKey definition) {
        this.pattern = pattern;
        this.definition = definition;
        this.identity = AEItemKey.of(definition.toStack());
        this.hashCode = identity.hashCode();
    }

    public WrappedPattern getPattern() {
        return pattern;
    }

    @Override
    public AEItemKey getDefinition() {
        return definition;
    }

    @Override
    public IInput[] getInputs() {
        return pattern.getInputs();
    }

    @Override
    public GenericStack[] getOutputs() {
        return pattern.getOutputs();
    }

    @Override
    public boolean supportsPushInputsToExternalInventory() {
        return pattern.supportsPushInputsToExternalInventory();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof NimblePatternWrapper<?> other && identity.equals(other.identity);
    }
}
