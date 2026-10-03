package com.ber.nimblePattern.mixin.ae2;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.service.CraftingService;
import com.ber.nimblePattern.probability.ProbabilityCpu;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Set;

@Mixin(value = CraftingService.class, remap = false, priority = 1300)
public class ProbabilityPriorityMixin {
    @Shadow @Final private Set<CraftingCPUCluster> craftingCPUClusters;
    @Inject(method = "insertIntoCpus", at = @At("HEAD"), cancellable = true)
    private void probabilityFirst(AEKey key, long amount, Actionable mode, CallbackInfoReturnable<Long> cir) {
        if (!com.ber.nimblePattern.probability.ProbabilityController.anyActive()) return;
        long accepted = com.ber.nimblePattern.probability.ProbabilityController.insertPriority(craftingCPUClusters, key, amount, mode);
        if (accepted < 0) return;
        for (var cpu : craftingCPUClusters)
            if (accepted < amount && !((ProbabilityCpu) cpu.craftingLogic).nimble$hasProbability())
                accepted += cpu.craftingLogic.insert(key, amount - accepted, mode);
        cir.setReturnValue(accepted);
    }
}
