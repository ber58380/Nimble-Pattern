package com.ber.nimblePattern.mixin.ae2;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.crafting.execution.*;
import appeng.crafting.inv.ListCraftingInventory;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.ber.nimblePattern.probability.*;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.Set;

@Mixin(value = CraftingCpuLogic.class, remap = false, priority = 1300)
public class ProbabilityCpuMixin implements ProbabilityCpu {
    @Shadow @Final private CraftingCPUCluster cluster;
    @Shadow @Final private ListCraftingInventory inventory;
    @Shadow private ExecutingCraftingJob job;
    @Unique private ProbabilityController nimble$probability;
    @Override public ProbabilityController nimble$probability() {
        if (nimble$probability == null) nimble$probability = new ProbabilityController();
        return nimble$probability;
    }
    @Override public boolean nimble$hasProbability() { return nimble$probability != null && nimble$probability.active(); }

    @Inject(method = "tickCraftingLogic", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        if (nimble$probability != null && job != null && cluster.isActive()) nimble$probability.tick(cluster, job, inventory);
    }
    @Inject(method = "getWaitingFor", at = @At("RETURN"), cancellable = true)
    private void waiting(AEKey key, CallbackInfoReturnable<Long> cir) {
        if (nimble$probability != null) cir.setReturnValue(Math.max(cir.getReturnValue(), nimble$probability.waiting(key)));
    }
    @Inject(method = "getAllWaitingFor", at = @At("TAIL"))
    private void keys(Set<AEKey> keys, CallbackInfo ci) { if (nimble$probability != null) nimble$probability.addWaiting(keys); }
    @Inject(method = "finishJob", at = @At("HEAD"))
    private void finished(CallbackInfo ci) {
        if (nimble$probability != null) nimble$probability.clear();
        nimble$probability = null;
    }
    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void save(CompoundTag tag, CallbackInfo ci) {
        if (nimble$probability != null) tag.put("nimble_probability", nimble$probability.save());
        else tag.remove("nimble_probability");
    }
    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void load(CompoundTag tag, CallbackInfo ci) {
        if (nimble$probability != null) nimble$probability.clear();
        nimble$probability = null;
        if (job != null && tag.contains("nimble_probability", 10)) {
            nimble$probability().load(tag.getCompound("nimble_probability"));
            nimble$probability.bind(cluster);
        }
    }
}
