package com.ber.nimblePattern.mixin.ae2;

import appeng.helpers.patternprovider.*;
import com.ber.nimblePattern.probability.ProbabilityMachines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = PatternProviderLogic.class, remap = false, priority = 1200)
public class ProbabilityProviderTargetMixin implements ProbabilityMachines.Target {
    @Shadow @Final private PatternProviderLogicHost host;
    @Shadow private Direction sendDirection;
    @Unique private BlockPos nimble$target;
    @Inject(method = "pushPattern", at = @At("HEAD"))
    private void reset(CallbackInfoReturnable<Boolean> cir) { nimble$target = null; }
    @Inject(method = "pushPattern", at = @At(value = "INVOKE", target = "Lappeng/helpers/patternprovider/PatternProviderLogic;sendStacksOut()Z"), require = 0)
    private void selected(CallbackInfoReturnable<Boolean> cir) {
        if (sendDirection != null) nimble$target = host.getBlockEntity().getBlockPos().relative(sendDirection);
    }
    @Override public BlockPos nimble$lastProbabilityTarget() { return nimble$target; }
    @Redirect(method = "pushPattern", at = @At(value = "INVOKE", target = "Lappeng/api/implementations/blockentities/ICraftingMachine;of(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/block/entity/BlockEntity;)Lappeng/api/implementations/blockentities/ICraftingMachine;"), require = 0)
    private appeng.api.implementations.blockentities.ICraftingMachine candidate(net.minecraft.world.level.Level level,
            BlockPos pos, Direction side, net.minecraft.world.level.block.entity.BlockEntity block) {
        nimble$target = pos;
        return appeng.api.implementations.blockentities.ICraftingMachine.of(level, pos, side, block);
    }
}
