package com.ber.nimblePattern.mixin.ae2;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.definitions.AEItems;
import appeng.crafting.pattern.AECraftingPattern;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.helpers.patternprovider.PatternProviderTarget;
import appeng.util.ConfigManager;
import com.ber.nimblePattern.crafting.ProbabilityDispatchContext;
import com.ber.nimblePattern.pattern.NimblePatternTag;
import com.ber.nimblePattern.pattern.wrapper.NimbleCraftingPattern;
import com.ber.nimblePattern.pattern.wrapper.NimblePatternWrapper;
import com.ber.nimblePattern.pattern.wrapper.NimbleProcessingPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
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
import java.util.Set;

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
    @Unique
    private boolean cachedFuzzyMode;
    @Unique
    private List<IPatternDetails> cachedRaw;
    @Unique
    private List<IPatternDetails> cachedWrapped;
    @Shadow
    @Final
    private List<GenericStack> sendList;


    @Inject(method = "<init>(Lappeng/api/networking/IManagedGridNode;Lappeng/helpers/patternprovider/PatternProviderLogicHost;I)V", at = @At("TAIL"))
    private void registerUpgrades(IManagedGridNode mainNode, PatternProviderLogicHost host, int patternInventorySize, CallbackInfo ci) {
        this.cachedFuzzyMode = false;
        this.cachedRaw = List.of();
        this.cachedWrapped = List.of();
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

    /**
     * If the pattern has tagged with nimble_pattern, wrap it
     */
    @Inject(method = "getAvailablePatterns", at = @At("RETURN"), cancellable = true)
    private void wrapAvailablePatterns(CallbackInfoReturnable<List<IPatternDetails>> cir) {
        var raw = cir.getReturnValue();

        // Check if there are any changes in the pattern provider.
        // If not changes, use cached patterns directly.
        boolean fuzzyMode = this.upgrades.isInstalled(AEItems.FUZZY_CARD);
        boolean unchanged = cachedFuzzyMode == fuzzyMode && cachedRaw.size() == raw.size();
        if (unchanged) {
            for (int i = 0; i < raw.size(); i++) {
                if (cachedRaw.get(i) != raw.get(i)) {
                    unchanged = false;
                    break;
                }
            }
        }
        if (unchanged) {
            cir.setReturnValue(cachedWrapped);
            return;
        }

        // Since cacheable patterns don't match the new patterns, wrap patterns from scratch.
        var wrapped = new ArrayList<IPatternDetails>(raw.size());
        for (var pattern : raw) {
            var definition = pattern.getDefinition();
            var stack = NimblePatternTag.filterCraftingTags(definition.toStack());
            if (pattern instanceof AEProcessingPattern app) {
                if (fuzzyMode) {
                    NimblePatternTag.tagFuzzy(stack);
                }
                definition = AEItemKey.of(stack);
                wrapped.add(new NimbleProcessingPattern(app, definition));
            } else if (pattern instanceof AECraftingPattern acp) {
                definition = AEItemKey.of(stack);
                wrapped.add(new NimbleCraftingPattern(acp, definition));
            } else {
                wrapped.add(pattern);
            }
        }
        cachedRaw = List.copyOf(raw);
        cachedWrapped = List.copyOf(wrapped);
        cachedFuzzyMode = fuzzyMode;
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
        if (patternDetails instanceof NimblePatternWrapper<?> npw) {
            for (var pattern : patterns) {
                if (pattern.equals(npw.getPattern())) {
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

    /**
     * Record actual inserted materials
     */
    @Unique
    private static PatternProviderTarget wrapInventoryTarget(PatternProviderTarget delegate, ServerLevel level, BlockPos targetPos, Direction inputSide) {
        return new PatternProviderTarget() {
            @Override
            public long insert(AEKey what, long amount, Actionable mode) {
                long inserted = delegate.insert(what, amount, mode);
                if (mode == Actionable.MODULATE && inserted > 0 && ProbabilityDispatchContext.isCapturing()) {
                    var accepted = new KeyCounter();
                    accepted.add(what, inserted);
                    ProbabilityDispatchContext.recordDelivery(level, targetPos, inputSide, accepted);
                }
                return inserted;
            }

            @Override
            public boolean containsPatternInput(Set<AEKey> patternInputs) {
                return delegate.containsPatternInput(patternInputs);
            }
        };
    }

    /**
     * Get the actual receiving position of deliveries
     */
    @Inject(method = "findAdapter", at = @At("RETURN"), cancellable = true)
    private void captureInventoryTarget(Direction direction, CallbackInfoReturnable<PatternProviderTarget> cir) {
        var target = cir.getReturnValue();
        if (target == null || !ProbabilityDispatchContext.isCapturing()) {
            return;
        }
        var provider = host.getBlockEntity();
        if (!(provider.getLevel() instanceof ServerLevel level)) {
            return;
        }
        var targetPos = provider.getBlockPos().relative(direction);
        var inputSide = direction.getOpposite();
        cir.setReturnValue(wrapInventoryTarget(target, level, targetPos, inputSide));
    }

    /**
     * Record the successful and completed deliveries.
     */
    @Unique
    private static ICraftingMachine wrapCraftingMachine(ICraftingMachine delegate, ServerLevel level, BlockPos targetPos, Direction inputSide, boolean directlyConnected) {
        return new ICraftingMachine() {
            @Override
            public PatternContainerGroup getCraftingMachineInfo() {
                return delegate.getCraftingMachineInfo();
            }

            @Override
            public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder, Direction ejectionDirection) {
                if (!ProbabilityDispatchContext.isCapturing()) {
                    return delegate.pushPattern(patternDetails, inputHolder, ejectionDirection);
                }
                if (!directlyConnected) {
                    boolean accepted = delegate.pushPattern(patternDetails, inputHolder, ejectionDirection);
                    if (accepted) {
                        ProbabilityDispatchContext.markIncomplete();
                    }
                    return accepted;
                }
                var inputs = new KeyCounter();
                for (var input : inputHolder) {
                    inputs.addAll(input);
                }
                boolean accepted = delegate.pushPattern(patternDetails, inputHolder, ejectionDirection);
                if (accepted) {
                    ProbabilityDispatchContext.recordDelivery(level, targetPos, inputSide, inputs);
                }
                return accepted;
            }

            @Override
            public boolean acceptsPlans() {
                return delegate.acceptsPlans();
            }
        };
    }

    /**
     * Some receivers realize the ICraftingMachine and could accept the whole pattern and materials.
     * Get the position of receiver.
     */
    @Redirect(method = "pushPattern", at = @At(value = "INVOKE", target = "Lappeng/api/implementations/blockentities/ICraftingMachine;of(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/block/entity/BlockEntity;)Lappeng/api/implementations/blockentities/ICraftingMachine;"))
    private ICraftingMachine captureCraftingMachine(Level level, BlockPos targetPos, Direction inputSide, BlockEntity blockEntity) {
        var machine = ICraftingMachine.of(level, targetPos, inputSide, blockEntity);
        if (machine == null || !ProbabilityDispatchContext.isCapturing() || !(level instanceof ServerLevel serverLevel)) {
            return machine;
        }
        var provider = host.getBlockEntity();
        boolean directlyConnected = provider.getLevel() == level && provider.getBlockPos().relative(inputSide.getOpposite()).equals(targetPos);
        return wrapCraftingMachine(machine, serverLevel, targetPos.immutable(), inputSide, directlyConnected);
    }

    /**
     * Check the deliveries is completed or not.
     * sendList: the undelivered inputs that buffer in pattern provider.
     */
    @Inject(method = "pushPattern", at = @At("RETURN"))
    private void checkPendingProbabilityInputs(IPatternDetails patternDetails, KeyCounter[] inputHolder, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue()) && ProbabilityDispatchContext.isCapturing() && !sendList.isEmpty()) {
            ProbabilityDispatchContext.markIncomplete();
        }
    }
}
