package com.ber.nimblePattern.mixin.extendedae;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.helpers.patternprovider.PatternProviderLogic;
import com.glodblock.github.extendedae.common.parts.PartExPatternProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PartExPatternProvider.class, remap = false)
public class PartExPatternProviderMixin implements IUpgradeableObject {
    // implement the IUpgradeableObject, so the pattern provider part could add upgraded card by shift-right click
    // the block one extends the ae2 block entity, so don't need a mixin
    @Final
    @Shadow
    protected PatternProviderLogic logic;

    @Override
    public IUpgradeInventory getUpgrades() {
        return ((IUpgradeableObject) logic).getUpgrades();
    }
}
