package com.ber.nimblePattern.mixin.ae2;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.helpers.patternprovider.PatternProviderLogic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PatternProviderBlockEntity.class, remap = false)
public class PatternProviderBlockEntityMixin implements IUpgradeableObject {
    // implement the IUpgradeableObject, so the pattern provider block could add upgraded card by shift-right click
    @Final
    @Shadow
    protected PatternProviderLogic logic;

    @Override
    public IUpgradeInventory getUpgrades() {
        return ((IUpgradeableObject) logic).getUpgrades();
    }
}
