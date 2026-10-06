package com.ber.nimblePattern.pattern;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;

public class NimblePatternWrapper<WrappedPattern extends IPatternDetails> implements IPatternDetails {
    protected final WrappedPattern pattern;

    public NimblePatternWrapper(WrappedPattern pattern) {
        this.pattern = pattern;
    }

    public WrappedPattern getPattern() {
        return pattern;
    }

    @Override
    public AEItemKey getDefinition() {
        return pattern.getDefinition();
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
        return pattern.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NimblePatternWrapper<?> wrapper) {
            return pattern.equals(wrapper.pattern);
        }
        return pattern.equals(obj);
    }
}
