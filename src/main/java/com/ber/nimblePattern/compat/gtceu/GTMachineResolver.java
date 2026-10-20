package com.ber.nimblePattern.compat.gtceu;

import com.ber.nimblePattern.crafting.ProbabilityDispatchContext;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.integration.ae2.machine.MEPatternBufferPartMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.MEPatternBufferProxyPartMachine;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class GTMachineResolver {
    private GTMachineResolver() {
    }

    /**
     * Check the machine doesn't change and is loaded at the position.
     */
    private static boolean isLoadedMachine(ServerLevel level, @Nullable MetaMachine machine) {
        if (machine == null || machine.getLevel() != level || machine.isInValid()) {
            return false;
        }
        var pos = machine.getPos();
        return level.hasChunkAt(pos) && MetaMachine.getMachine(level, pos) == machine;
    }

    @Nullable
    private static MetaMachine findExecutor(ServerLevel level, MetaMachine receiver) {
        // For a single-block machine, itself is both the receiver and executor
        MetaMachine executor = receiver;
        if (receiver instanceof IMultiPart part) {
            var controllers = part.getControllers();
            // A shared input part could not determine the controller
            if (controllers.size() != 1) {
                return null;
            }
            var controller = controllers.get(0);
            executor = controller.self();
            if (!isLoadedMachine(level, executor) || !controller.getParts().contains(part)) {
                return null;
            }
        }
        if (executor instanceof IMultiController controller && !controller.isFormed()) {
            return null;
        }
        return executor;
    }

    /**
     * Resolve a candidate executor for a captured delivery
     */
    public static Optional<ResolvedMachine> resolve(ServerLevel level, ProbabilityDispatchContext.Delivery delivery) {
        var target = delivery.target();
        if (!level.dimension().equals(target.dimension())) {
            return Optional.empty();
        }
        if (!level.hasChunkAt(target.pos())) {
            return Optional.empty();
        }
        var receiver = MetaMachine.getMachine(level, target.pos());
        if (!isLoadedMachine(level, receiver)) {
            return Optional.empty();
        }
        if (receiver instanceof MEPatternBufferPartMachine || receiver instanceof MEPatternBufferProxyPartMachine) {
            return Optional.empty();
        }
        var executor = findExecutor(level, receiver);
        if (executor == null) {
            return Optional.empty();
        }
        if (!(executor instanceof IRecipeLogicMachine recipeMachine)) {
            return Optional.empty();
        }
        var recipeLogic = recipeMachine.getRecipeLogic();
        return Optional.of(new ResolvedMachine(receiver, executor, recipeLogic));
    }


    /**
     * Take a multi-block machine as an example.
     *
     * @param receiver    input receiving chamber
     * @param executor    the core controller of the multi-block machine
     * @param recipeLogic the object of receiving patterns and executing
     */
    public record ResolvedMachine(MetaMachine receiver, MetaMachine executor, RecipeLogic recipeLogic) {
    }
}
