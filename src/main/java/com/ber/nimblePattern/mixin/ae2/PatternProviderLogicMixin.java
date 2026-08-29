package com.ber.nimblePattern.mixin.ae2;

import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.AEKey;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.definitions.AEItems;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.util.ConfigManager;
import com.ber.nimblePattern.pattern.NimbleProcessingPattern;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

// mod mae2 will overwrite "pushPattern" function, set a higher priority to mixin later than mae2
@Mixin(value = PatternProviderLogic.class, remap = false, priority = 1100)
public class PatternProviderLogicMixin implements IUpgradeableObject {
    @Unique
    private IUpgradeInventory upgrades;
    @Shadow
    @Final
    private PatternProviderLogicHost host;
    @Shadow
    @Final
    private ConfigManager configManager;


    @Inject(method = "<init>(Lappeng/api/networking/IManagedGridNode;Lappeng/helpers/patternprovider/PatternProviderLogicHost;I)V", at = @At("TAIL"))
    private void registerUpgrades(IManagedGridNode mainNode, PatternProviderLogicHost host, int patternInventorySize, CallbackInfo ci) {
        this.upgrades = UpgradeInventories.forMachine(host.getMainMenuIcon().getItem(), 1, () -> {
            this.host.saveChanges();
            ICraftingProvider.requestUpdate(mainNode);
        });
        configManager.registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"))
    public void injectWriteToNBT(CompoundTag tag, CallbackInfo ci) {
        this.upgrades.writeToNBT(tag, "upgrades");
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"))
    public void injectReadFromNBT(CompoundTag tag, CallbackInfo ci) {
        this.upgrades.readFromNBT(tag, "upgrades");
    }

    @Inject(method = "addDrops", at = @At("TAIL"))
    public void injectAddDrops(List<ItemStack> drops, CallbackInfo ci) {
        for (var is : this.upgrades) {
            if (!is.isEmpty()) {
                drops.add(is);
            }
        }
    }

    @Inject(method = "clearContent", at = @At("TAIL"))
    public void injectClearContent(CallbackInfo ci) {
        this.upgrades.clear();
    }

    // wrap the patterns into NimbleProcessingPattern
    @Inject(method = "getAvailablePatterns", at = @At("RETURN"), cancellable = true)
    private void wrapAvailablePatterns(CallbackInfoReturnable<List<IPatternDetails>> cir) {
        var raw = cir.getReturnValue();
        var wrapped = new ArrayList<IPatternDetails>(raw.size());
        for (var pattern : raw) {
            if (pattern instanceof AEProcessingPattern aep) {
                wrapped.add(new NimbleProcessingPattern(aep, this.upgrades.isInstalled(AEItems.FUZZY_CARD)));
            } else {
                wrapped.add(pattern);
            }
        }
        cir.setReturnValue(wrapped);
    }

    @Redirect(
            method = "pushPattern",
            at = @At(value = "INVOKE", target = "Ljava/util/List;contains(Ljava/lang/Object;)Z")
    )
    private boolean redirectPatternContains(List<IPatternDetails> patterns, Object patternDetails) {
        if (patterns.contains(patternDetails)) {
            return true;
        }
        if (patternDetails instanceof NimbleProcessingPattern npp) {
            for (var pattern : patterns) {
                if (pattern.equals(npp.getPattern())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Redirect(
            method = "onStackReturnedToNetwork",
            at = @At(value = "INVOKE",
                    target = "Ljava/lang/Object;equals(Ljava/lang/Object;)Z")
    )
    private boolean fuzzyUnlockValid(Object what, Object output) {
        if (this.upgrades.isInstalled(AEItems.FUZZY_CARD)) {
            return ((AEKey) what).dropSecondary().equals(((AEKey) output).dropSecondary());
        }
        return what.equals(output);
    }
}
