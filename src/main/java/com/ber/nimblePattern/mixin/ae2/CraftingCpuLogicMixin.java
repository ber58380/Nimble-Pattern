package com.ber.nimblePattern.mixin.ae2;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.CraftingLink;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.crafting.execution.ExecutingCraftingJob;
import appeng.crafting.inv.ListCraftingInventory;
import com.ber.nimblePattern.crafting.FuzzyOutputLedger;
import com.ber.nimblePattern.pattern.NimbleProcessingPattern;
import com.ber.nimblePattern.pattern.PatternMapping;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// mod GTLCore will overwrite "executeCrafting", set higher priority to mixin later
@Mixin(value = CraftingCpuLogic.class, remap = false, priority = 1200)
public class CraftingCpuLogicMixin {
    @Unique
    private final FuzzyOutputLedger fuzzyOutputLedger = new FuzzyOutputLedger();
    @Unique
    private NimbleProcessingPattern currentPattern;
    @Unique
    private boolean isFakePattern;
    @Unique
    private boolean isFuzzyPattern;
    @Shadow
    private @Nullable ExecutingCraftingJob job;

    @Inject(method = "executeCrafting", at = @At("HEAD"))
    private void initializeFlags(CallbackInfoReturnable<Integer> cir) {
        currentPattern = null;
        isFakePattern = false;
        isFuzzyPattern = false;
    }

    // record what is the current pushing pattern
    @Redirect(
            method = "executeCrafting",
            at = @At(value = "INVOKE",
                    target = "Lappeng/api/networking/crafting/ICraftingProvider;pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;)Z"))
    private boolean trackCurrentPattern(ICraftingProvider provider, IPatternDetails details, KeyCounter[] craftingContainer) {
        boolean result = provider.pushPattern(PatternMapping.getOriginalPattern(provider, details), craftingContainer);
        currentPattern = result && details instanceof NimbleProcessingPattern npp ? npp : null;
        return result;
    }

    @Redirect(
            method = "executeCrafting",
            at = @At(value = "INVOKE",
                    target = "Lappeng/crafting/inv/ListCraftingInventory;insert(Lappeng/api/stacks/AEKey;JLappeng/api/config/Actionable;)V"))
    private void nimbleWaitingForInsert(ListCraftingInventory inv, AEKey what, long amount, Actionable mode) {
        var cpu = (CraftingCpuLogic) (Object) this;
        // fake pattern
        if (currentPattern != null && currentPattern.getFakeMode()) {
            var finalOutput = cpu.getFinalJobOutput();
            if (what.matches(finalOutput)) {
                inv.insert(what, amount, mode); // register output
                isFakePattern = true; // claim that a fake pattern invokes insert function
                try {
                    cpu.insert(what, amount, Actionable.MODULATE); // finish output immediately
                } finally {
                    isFakePattern = false;
                }
                return;
            }
        }
        // fuzzy pattern
        if (currentPattern != null && currentPattern.getFuzzyMode() && mode == Actionable.MODULATE) {
            fuzzyOutputLedger.add(what, amount);
        }
        // register output for normal and fuzzy patterns
        inv.insert(what, amount, mode);
    }

    @Redirect(
            method = "insert",
            at = @At(value = "INVOKE",
                    target = "Lappeng/crafting/CraftingLink;insert(Lappeng/api/stacks/AEKey;JLappeng/api/config/Actionable;)J"))
    private long fakeInsert(CraftingLink link, AEKey what, long amount, Actionable mode) {
        // When insert function is invoked, might not invoke new executeCrafting, so currentPattern
        // might not latest. Use a boolean flag variable instead to clarify the source of invoke.
        if (isFakePattern) {
            // simulate the output is back to the network
            return amount;
        }
        return link.insert(what, amount, mode);
    }

    @Redirect(
            method = "insert",
            at = @At(value = "INVOKE",
                    target = "Lappeng/crafting/inv/ListCraftingInventory;extract(Lappeng/api/stacks/AEKey;JLappeng/api/config/Actionable;)J"))
    private long fuzzyExtract(ListCraftingInventory inv, AEKey what, long amount, Actionable mode) {
        var receipt = fuzzyOutputLedger.extract(inv, what, amount, mode, (key, accepted) -> {
        });
        isFuzzyPattern = receipt.fuzzyMode();
        return receipt.amount();
    }

    // check if the output is the final output
    @Redirect(
            method = "insert",
            at = @At(value = "INVOKE",
                    target = "Lappeng/api/stacks/AEKey;matches(Lappeng/api/stacks/GenericStack;)Z"))
    private boolean redirectFuzzyMatches(AEKey what, GenericStack finalOutput) {
        if (isFuzzyPattern) {
            isFuzzyPattern = false;
            return what.dropSecondary().equals(finalOutput.what().dropSecondary());
        }
        return what.matches(finalOutput);
    }

    @Inject(method = "finishJob", at = @At("HEAD"))
    private void injectFinishJob(boolean success, CallbackInfo ci) {
        fuzzyOutputLedger.clear();
    }

    @Inject(method = "cancel", at = @At("HEAD"))
    private void injectCancel(CallbackInfo ci) {
        fuzzyOutputLedger.clear();
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void injectWriteToNBT(CompoundTag tag, CallbackInfo ci) {
        tag.put("fuzzyOutputLedger", fuzzyOutputLedger.save());
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void injectReadFromNBT(CompoundTag tag, CallbackInfo ci) {
        fuzzyOutputLedger.load(tag.getList("fuzzyOutputLedger", Tag.TAG_COMPOUND));
        if (job == null) {
            fuzzyOutputLedger.clear();
        }
    }
}
