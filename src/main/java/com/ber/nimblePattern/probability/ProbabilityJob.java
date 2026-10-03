package com.ber.nimblePattern.probability;

import appeng.api.networking.crafting.ICraftingPlan;
import appeng.crafting.inv.ListCraftingInventory;

public interface ProbabilityJob {
    void nimble$appendPlan(ICraftingPlan plan, appeng.api.stacks.GenericStack replaced);
    ListCraftingInventory nimble$waiting();
}
