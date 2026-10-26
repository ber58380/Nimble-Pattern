package com.ber.nimblePattern.mixin.gtlcore;

import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.stacks.AEKey;
import appeng.me.service.CraftingService;
import com.ber.nimblePattern.compat.gtlcore.GtlGraphLoopAccess;
import com.ber.nimblePattern.crafting.LoopCpuAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GTL graph jobs bypass AE's classic task execution. Drive the loop controller alongside the graph
 * tick, intercept the internal machine outputs that belong to a running loop, and release the loop's
 * borrowed seed when the graph job ends.
 */
@Pseudo
@Mixin(targets = "org.gtlcore.gtlcore.integration.ae2.graph.GraphCpuController", remap = false)
public class GraphCpuControllerMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void nimblePattern$tickLoop(IEnergyService energy, CraftingService service, CallbackInfo ci) {
        LoopCpuAccess access = GtlGraphLoopAccess.find(this);
        if (access != null) {
            access.nimblePattern$tickLoop(service, energy);
        }
    }

    @Inject(method = "insert", at = @At("HEAD"), cancellable = true)
    private void nimblePattern$acceptLoopOutput(AEKey key, long amount, Actionable mode, CallbackInfoReturnable<Long> cir) {
        LoopCpuAccess access = GtlGraphLoopAccess.find(this);
        if (access == null || !access.nimblePattern$hasActiveLoop()) {
            return;
        }
        var acceptance = access.nimblePattern$acceptLoopOutput(key, amount, mode);
        if (!acceptance.handled()) {
            return;
        }

        if (mode == Actionable.MODULATE && acceptance.completedOutput() != null) {
            for (var remainder : acceptance.remainder()) {
                access.nimblePattern$deliverLoopRemainder(remainder.what(), remainder.amount());
            }
            access.nimblePattern$deliverLoopRemainder(
                    acceptance.completedOutput().what(), acceptance.completedOutput().amount());
        }
        if (mode == Actionable.MODULATE && acceptance.accepted() < amount) {
            access.nimblePattern$deliverLoopRemainder(key, amount - acceptance.accepted());
        }
        cir.setReturnValue(acceptance.accepted());
    }

    @Inject(method = "read", at = @At("HEAD"))
    private void nimblePattern$readLoop(CompoundTag parent, CallbackInfo ci) {
        LoopCpuAccess access = GtlGraphLoopAccess.find(this);
        if (access != null && parent.contains("loopController", Tag.TAG_COMPOUND)) {
            access.nimblePattern$loadGraphLoop(parent.getCompound("loopController"));
        }
    }

    @Inject(method = "finish", at = @At("HEAD"))
    private void nimblePattern$finishLoop(CallbackInfo ci) {
        LoopCpuAccess access = GtlGraphLoopAccess.find(this);
        if (access != null) {
            access.nimblePattern$finishGraphLoop();
        }
    }
}
