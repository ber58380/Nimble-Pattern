package com.ber.nimblePattern.mixin.gtlcore;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.CraftingCalculation;
import appeng.me.service.CraftingService;
import com.ber.nimblePattern.compat.gtlcore.GtlPatternRouting;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.Future;

@Pseudo
@Mixin(targets = "org.gtlcore.gtlcore.integration.ae2.graph.CraftingEngineRouter", remap = false)
public class CraftingEngineRouterMixin {
    @Inject(method = "begin(Lorg/gtlcore/gtlcore/integration/ae2/graph/GtlPatternCatalog;Lappeng/api/networking/IGrid;Lappeng/me/service/CraftingService;Lnet/minecraft/world/level/Level;Lappeng/api/networking/crafting/ICraftingSimulationRequester;Lappeng/api/stacks/AEKey;JLappeng/api/networking/crafting/CalculationStrategy;)Ljava/util/concurrent/Future;",
            at = @At("HEAD"), cancellable = true, require = 1)
    private static void nimblePattern$routeSpecialPatterns(@Coerce Object catalog, IGrid grid,
                                                           CraftingService service, Level level, ICraftingSimulationRequester requester, AEKey requested,
                                                           long amount, CalculationStrategy strategy, CallbackInfoReturnable<Future<ICraftingPlan>> cir) {
        boolean classic;
        try {
            // GTL exposes its complete catalog on CraftingService, including secondary outputs.
            @SuppressWarnings("unchecked")
            var patterns = (Iterable<IPatternDetails>) service.getClass()
                    .getMethod("gtlcore$registeredGraphPatterns").invoke(service);
            classic = GtlPatternRouting.requiresClassic(requested, patterns);
        } catch (ReflectiveOperationException e) {
            // If GTL changes this optional API, preserve the established special-pattern semantics.
            classic = true;
        }
        if (classic) {
            if (level == null || requester == null) {
                throw new IllegalArgumentException("Invalid Crafting Job Request");
            }
            var calculation = new CraftingCalculation(level, grid, requester,
                    new GenericStack(requested, amount), strategy);
            cir.setReturnValue(CraftingServicePoolAccessor.nimblePattern$craftingPool().submit(calculation::run));
        }
    }
}
