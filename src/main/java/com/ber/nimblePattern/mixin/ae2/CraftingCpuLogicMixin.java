package com.ber.nimblePattern.mixin.ae2;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.CraftingLink;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.crafting.inv.ListCraftingInventory;
import com.ber.nimblePattern.pattern.NimbleProcessingPattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

// mod GTLCore will overwrite "executeCrafting", set higher priority to mixin later
@Mixin(value = CraftingCpuLogic.class, remap = false, priority = 1200)
public class CraftingCpuLogicMixin {
    @Unique
    // {fuzzyKey: {exactKey: amount}}
    private final Map<AEKey, Map<AEKey, Long>> fuzzyOutputs = new HashMap<AEKey, Map<AEKey, Long>>();
    @Unique
    private NimbleProcessingPattern currentPattern;
    @Unique
    private boolean isFakePattern;
    @Unique
    private boolean isFuzzyPattern;

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
        boolean result = provider.pushPattern(details, craftingContainer);
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
        if (currentPattern != null && currentPattern.getFuzzyMode()) {
            // record fuzzy outputs by fuzzy keys
            fuzzyOutputs.computeIfAbsent(what.dropSecondary(), k -> new HashMap<>())
                    .merge(what, amount, Long::sum);
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
        if (mode == Actionable.SIMULATE) {
            // try exact extract first
            long exact = inv.extract(what, amount, Actionable.SIMULATE);
            if (exact > 0) {
                return exact;
            }
            // if failed, try fuzzy extract
            var inner = fuzzyOutputs.get(what.dropSecondary());
            exact = inner == null ? 0 : inner.values().stream().mapToLong(Long::longValue).sum();
            if (exact > 0) {
                isFuzzyPattern = true;
            }
            return Math.min(exact, amount);
        }
        // mode == Actionable.MODULATE
        // try exact extract first
        long exact = inv.extract(what, amount, Actionable.MODULATE);
        // if still have remainder, try fuzzy extract
        long remaining = amount - exact;
        if (remaining > 0) {
            var inner = fuzzyOutputs.get(what.dropSecondary());
            if (inner != null) {
                isFuzzyPattern = true;
                var iterator = inner.entrySet().iterator();
                while (iterator.hasNext()) {
                    var entry = iterator.next();
                    long deduction = Math.min(remaining, entry.getValue());
                    inv.extract(entry.getKey(), deduction, Actionable.MODULATE);
                    remaining -= deduction;
                    long leftValue = entry.getValue() - deduction;
                    if (leftValue == 0) {
                        iterator.remove();
                    } else {
                        entry.setValue(leftValue);
                    }
                    if (remaining == 0) {
                        break;
                    }
                }
                if (inner.isEmpty()) {
                    fuzzyOutputs.remove(what.dropSecondary());
                }
            }
        }
        return amount - remaining;
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
        fuzzyOutputs.clear();
    }

    @Inject(method = "cancel", at = @At("HEAD"))
    private void injectCancel(CallbackInfo ci) {
        fuzzyOutputs.clear();
    }
}
