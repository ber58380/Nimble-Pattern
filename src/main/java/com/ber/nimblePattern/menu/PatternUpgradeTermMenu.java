package com.ber.nimblePattern.menu;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.helpers.InventoryAction;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.slot.DisabledSlot;
import appeng.menu.slot.FakeSlot;
import appeng.menu.slot.InaccessibleSlot;
import appeng.menu.slot.RestrictedInputSlot;
import appeng.parts.crafting.PatternProviderPart;
import appeng.util.inv.AppEngInternalInventory;
import com.ber.nimblePattern.compat.extendedae.ExtendedAECompat;
import com.ber.nimblePattern.helpers.IPatternUpgradeMenuHost;
import com.ber.nimblePattern.network.ClearPacket;
import com.ber.nimblePattern.network.ConditionPacket;
import com.ber.nimblePattern.network.NimblePatternNetwork;
import com.ber.nimblePattern.network.PatternPacket;
import com.ber.nimblePattern.parts.PatternUpgradeLogic;
import com.ber.nimblePattern.pattern.NimblePatternTag;
import com.ber.nimblePattern.pattern.PatternUpgradeTracker;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static appeng.helpers.InventoryAction.PICKUP_OR_SET_DOWN;
import static appeng.helpers.InventoryAction.SPLIT_OR_PLACE_SINGLE;
import static com.ber.nimblePattern.parts.PatternUpgradeLogic.INPUT_PATTERN_SLOTS;

public class PatternUpgradeTermMenu extends AEBaseMenu {
    public static final MenuType<PatternUpgradeTermMenu> TYPE = MenuTypeBuilder
            .create(PatternUpgradeTermMenu::new, IPatternUpgradeMenuHost.class)
            .build("patternupgradeterminal");

    private static long inventorySerial = Long.MIN_VALUE;
    // Pattern provider -> Container information
    private final Map<PatternContainer, ContainerTracker> diList = new IdentityHashMap<>();
    // Pattern provider temp id -> Container information
    private final Long2ObjectOpenHashMap<ContainerTracker> byId = new Long2ObjectOpenHashMap<>();

    public static final SlotSemantic INPUT_PATTERN = SlotSemantics.register("INPUT_PATTERN", false);
    public static final SlotSemantic CONDITION_ITEM = SlotSemantics.register("CONDITION_ITEM", false);
    private final IPatternUpgradeMenuHost host;
    private final PatternUpgradeLogic upgradeLogic;
    private final InternalInventory inputPatternInv;
    private final InternalInventory conditionItemInv;
    private final RestrictedInputSlot[] inputPatternSlots = new RestrictedInputSlot[INPUT_PATTERN_SLOTS];
    // dummy pattern provider, used for rendering the blank slots in the last row
    public static final long VIRTUAL_ID = Long.MAX_VALUE;
    public static final AppEngInternalInventory VIRTUAL_INV = new AppEngInternalInventory(9);
    private final FakeSlot conditionItemSlot;
    private Set<String> conditionsHistory = new LinkedHashSet<>();

    public PatternUpgradeTermMenu(int id, Inventory ip, IPatternUpgradeMenuHost host) {
        this(TYPE, id, ip, host, true);
    }

    public PatternUpgradeTermMenu(MenuType<?> menuType, int id, Inventory ip, IPatternUpgradeMenuHost host, boolean bindInventory) {
        super(menuType, id, ip, host);
        this.host = host;
        this.upgradeLogic = host.getLogic();
        this.inputPatternInv = upgradeLogic.getInputPatternInv();
        for (int i = 0; i < INPUT_PATTERN_SLOTS; i++) {
            var slot = new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN, inputPatternInv, i);
            slot.setStackLimit(1);
            this.inputPatternSlots[i] = slot;
            this.addSlot(slot, INPUT_PATTERN);
        }
        this.conditionItemInv = upgradeLogic.getConditionItemInv();
        this.conditionItemSlot = new FakeSlot(conditionItemInv, 0);
        this.addSlot(conditionItemSlot, CONDITION_ITEM);
        if (bindInventory) {
            this.createPlayerInventorySlots(ip);
        }
        registerClientAction("applyCondition", String.class, this::applyCondition);
        registerClientAction("clearCondition", this::clearCondition);
    }

    public RestrictedInputSlot[] getInputPatternSlots() {
        return inputPatternSlots;
    }

    public InternalInventory getInputPatternInv() {
        return inputPatternInv;
    }

    public FakeSlot getConditionItemSlot() {
        return conditionItemSlot;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void broadcastChanges() {
        if (isClientSide()) {
            return;
        }
        super.broadcastChanges();
        IGrid grid = getGrid();
        var state = new PatternUpgradeTermMenu.VisitorState();
        if (grid != null) {
            for (var machineClass : grid.getMachineClasses()) {
                if (PatternContainer.class.isAssignableFrom(machineClass)) {
                    visitPatternProviderHosts(grid, (Class<? extends PatternContainer>) machineClass, state);
                }
            }
        }
        if (state.total != this.diList.size() || state.forceFullUpdate) {
            sendFullUpdate(grid);
        } else {
            sendIncrementalUpdate();
        }
    }

    @Nullable
    private IGrid getGrid() {
        IActionHost host = this.getActionHost();
        if (host != null) {
            final IGridNode agn = host.getActionableNode();
            if (agn != null && agn.isActive()) {
                return agn.getGrid();
            }
        }
        return null;
    }

    private static class VisitorState {
        // Total number of pattern provider hosts found
        int total;
        // Set to true if any visited machines were missing from diList, or had a different name
        boolean forceFullUpdate;
    }

    private <T extends PatternContainer> void visitPatternProviderHosts(IGrid grid, Class<T> machineClass, VisitorState state) {
        for (var container : grid.getActiveMachines(machineClass)) {
            var t = this.diList.get(container);
            if (t == null) {
                state.forceFullUpdate = true;
            }
            state.total++;
        }
    }

    private void collectConditions(ContainerTracker inv, Set<String> conditions) {
        for (int i = 0; i < inv.server.size(); i++) {
            var pattern = inv.server.getStackInSlot(i);
            if (pattern.isEmpty()) {
                continue;
            }
            var condition = NimblePatternTag.getCondition(pattern);
            if (!condition.isBlank()) {
                conditions.add(condition);
            }
        }
    }

    public void applyCondition(String condition) {
        if (isClientSide()) {
            sendClientAction("applyCondition", condition);
            return;
        }
        for (int i = 0; i < inputPatternInv.size(); i++) {
            var pattern = inputPatternInv.getStackInSlot(i).copy();
            if (pattern.isEmpty()) {
                continue;
            }
            NimblePatternTag.tagUpdate(pattern, condition);
            if (NimblePatternTag.pushPatternBack(pattern, getPlayer().getServer())) {
                inputPatternInv.setItemDirect(i, ItemStack.EMPTY);
            }
        }
    }

    public void clearCondition() {
        if (isClientSide()) {
            sendClientAction("clearCondition");
            return;
        }
        conditionItemSlot.set(ItemStack.EMPTY);
        for (int i = 0; i < inputPatternInv.size(); i++) {
            var pattern = inputPatternInv.getStackInSlot(i).copy();
            if (pattern.isEmpty()) {
                continue;
            }
            NimblePatternTag.removeConditionTag(pattern);
            inputPatternInv.setItemDirect(i, pattern);
        }
    }

    private void sendFullUpdate(@Nullable IGrid grid) {
        this.byId.clear();
        this.diList.clear();

        if (getPlayer() instanceof ServerPlayer serverPlayer) {
            NimblePatternNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new ClearPacket());
        }

        if (grid == null) {
            if (getPlayer() instanceof ServerPlayer serverPlayer) {
                NimblePatternNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new ConditionPacket(Set.of()));
            }
            PatternUpgradeTracker.instance().updateTracked(Set.of());
            return;
        }

        for (var machineClass : grid.getMachineClasses()) {
            var containerClass = tryCastMachineToContainer(machineClass);
            if (containerClass == null) {
                continue;
            }

            for (var container : grid.getActiveMachines(containerClass)) {
                this.diList.put(container, new PatternUpgradeTermMenu.ContainerTracker(container, container.getTerminalPatternInventory()));
            }
        }

        Set<String> conditions = new LinkedHashSet<String>();
        for (var inv : this.diList.values()) {
            this.byId.put(inv.serverId, inv);
            if (getPlayer() instanceof ServerPlayer serverPlayer) {
                NimblePatternNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), inv.createFullPacket());
            }
            collectConditions(inv, conditions);
        }
        if (!conditions.equals(this.conditionsHistory)) {
            this.conditionsHistory = conditions.stream().sorted(String::compareToIgnoreCase).collect(Collectors.toCollection(LinkedHashSet::new));
            if (getPlayer() instanceof ServerPlayer serverPlayer) {
                NimblePatternNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new ConditionPacket(conditionsHistory));
                PatternUpgradeTracker.instance().updateTracked(conditionsHistory);
            }
        }

    }

    private void sendIncrementalUpdate() {
        Set<String> conditions = new LinkedHashSet<>();
        for (var inv : this.diList.values()) {
            var packet = inv.createUpdatePacket();
            if (packet != null && getPlayer() instanceof ServerPlayer serverPlayer) {
                NimblePatternNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), packet);
            }
            collectConditions(inv, conditions);
        }
        if (!conditions.equals(this.conditionsHistory)) {
            this.conditionsHistory = conditions.stream().sorted(String::compareToIgnoreCase).collect(Collectors.toCollection(LinkedHashSet::new));
            if (getPlayer() instanceof ServerPlayer serverPlayer) {
                NimblePatternNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new ConditionPacket(conditionsHistory));
                PatternUpgradeTracker.instance().updateTracked(conditionsHistory);
            }
        }
    }

    @Override
    public void doAction(ServerPlayer player, InventoryAction action, int slot, long id) {
        // deal with fake slots
        if (getSlot(slot) instanceof FakeSlot) {
            super.doAction(player, action, slot, id);
            return;
        }

        var carried = getCarried();
        // click on the blank virtual slots, only push pattern back to the network
        if (id == VIRTUAL_ID && !carried.isEmpty() && (action == PICKUP_OR_SET_DOWN || action == SPLIT_OR_PLACE_SINGLE)) {
            if (NimblePatternTag.pushPatternBack(carried.copy(), player.server)) {
                setCarried(ItemStack.EMPTY);
            }
            return;
        }

        final ContainerTracker inv = this.byId.get(id);
        if (inv == null || slot < 0 || slot >= inv.server.size()) {
            return;
        }
        final ItemStack is = inv.server.getStackInSlot(slot);
        var patternSlot = inv.server.getSlotInv(slot);

        ServerLevel level = getContainerLevel(inv.container);
        BlockPos pos = getContainerPos(inv.container);
        Direction side = getContainerSide(inv.container);

        switch (action) {
            case PICKUP_OR_SET_DOWN -> {
                if (!carried.isEmpty()) {
                    // put pattern back to the source pattern provider
                    if (NimblePatternTag.pushPatternBack(carried.copy(), player.server)) {
                        setCarried(ItemStack.EMPTY);
                    } else {
                        return;
                    }
                    // since put pattern back successfully, retrieve the target pattern to hand
                    ItemStack inSlot = patternSlot.getStackInSlot(0);
                    if (!inSlot.isEmpty()) {
                        inSlot = inSlot.copy();
                        if (level != null) {
                            NimblePatternTag.tagSource(inSlot, level, pos, side, slot);
                        }
                        patternSlot.setItemDirect(0, ItemStack.EMPTY);
                        setCarried(inSlot);
                    }
                } else { // hand empty, retrieve pattern from terminal
                    ItemStack pattern = patternSlot.getStackInSlot(0).copy();
                    if (!pattern.isEmpty() && level != null) {
                        NimblePatternTag.tagSource(pattern, level, pos, side, slot);
                        setCarried(pattern);
                        patternSlot.setItemDirect(0, ItemStack.EMPTY);
                    }
                }
            }
            case SPLIT_OR_PLACE_SINGLE -> {
                if (!carried.isEmpty()) {
                    // patterns can't stack, so equals put a pattern back to the terminal
                    if (NimblePatternTag.pushPatternBack(carried.copy(), player.server)) {
                        setCarried(ItemStack.EMPTY);
                    }
                } else if (!is.isEmpty()) {
                    // patterns can't stack, so retrieving the half equals retrieving one
                    ItemStack pattern = patternSlot.getStackInSlot(0).copy();
                    if (!pattern.isEmpty() && level != null) {
                        NimblePatternTag.tagSource(pattern, level, pos, side, slot);
                        setCarried(pattern);
                        patternSlot.setItemDirect(0, ItemStack.EMPTY);
                    }
                }
            }
            case SHIFT_CLICK -> {
                var stack = patternSlot.getStackInSlot(0).copy();
                if (!stack.isEmpty() && level != null) {
                    NimblePatternTag.tagSource(stack, level, pos, side, slot);
                }
                if (!player.getInventory().add(stack)) {
                    NimblePatternTag.removeTag(stack);
                    patternSlot.setItemDirect(0, stack);
                } else {
                    patternSlot.setItemDirect(0, ItemStack.EMPTY);
                }

            }
            case MOVE_REGION -> {
                // 暂不实现，等后续能切换产线视图时再添加该功能
            }
            case CREATIVE_DUPLICATE -> { // the duplicate one doesn't have tags
                if (player.getAbilities().instabuild && carried.isEmpty()) {
                    setCarried(is.isEmpty() ? ItemStack.EMPTY : is.copy());
                }
            }
        }
    }

    // make the shift-click not to be intercepted by input pattern slots, back to network instead
    @Override
    public ItemStack quickMoveStack(Player player, int idx) {
        if (isClientSide()) {
            return ItemStack.EMPTY;
        }
        final Slot clickSlot = this.slots.get(idx);
        SlotSemantic slotSemantic = getSlotSemantic(clickSlot);
        if (clickSlot instanceof DisabledSlot || clickSlot instanceof InaccessibleSlot) {
            return ItemStack.EMPTY;
        }
        boolean playerSide = clickSlot.container == getPlayerInventory()
                || slotSemantic == SlotSemantics.PLAYER_INVENTORY
                || slotSemantic == SlotSemantics.PLAYER_HOTBAR
                || slotSemantic == SlotSemantics.TOOLBOX;
        if (clickSlot.hasItem()) {
            ItemStack pattern = clickSlot.getItem();
            if (playerSide) {
                if (!pattern.isEmpty() && PatternDetailsHelper.isEncodedPattern(pattern)) {
                    if (player instanceof ServerPlayer serverPlayer) {
                        if (NimblePatternTag.pushPatternBack(pattern.copy(), serverPlayer.server)) {
                            clickSlot.set(ItemStack.EMPTY);
                            clickSlot.setChanged();
                            broadcastChanges();
                        }
                    }
                    return ItemStack.EMPTY;
                }
                return super.quickMoveStack(player, idx);
            }
        }
        return super.quickMoveStack(player, idx);
    }

    private static class ContainerTracker {
        private final PatternContainer container;
        private final long serverId = inventorySerial++;
        // This is used to track the inventory contents we sent to the client for change detection
        private final InternalInventory client;
        // This is a reference to the real inventory used by this machine
        private final InternalInventory server;

        public ContainerTracker(PatternContainer container, InternalInventory patterns) {
            this.container = container;
            this.server = patterns;
            this.client = new AppEngInternalInventory(this.server.size());
        }

        public PatternPacket createFullPacket() {
            var slots = new Int2ObjectArrayMap<ItemStack>(server.size());
            for (int i = 0; i < server.size(); i++) {
                var stack = server.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    slots.put(i, stack);
                }
            }
            return PatternPacket.fullUpdate(serverId, server.size(), slots);
        }

        @Nullable
        public PatternPacket createUpdatePacket() {
            var changedSlots = detectChangedSlots();
            if (changedSlots == null) {
                return null;
            }

            var slots = new Int2ObjectArrayMap<ItemStack>(changedSlots.size());
            for (int i = 0; i < changedSlots.size(); i++) {
                var slot = changedSlots.getInt(i);
                var stack = server.getStackInSlot(slot);
                // "update" client side.
                client.setItemDirect(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
                slots.put(slot, stack);
            }

            return PatternPacket.incrementalUpdate(serverId, slots);
        }

        @Nullable
        private IntList detectChangedSlots() {
            IntList changedSlots = null;
            for (int x = 0; x < server.size(); x++) {
                if (isDifferent(server.getStackInSlot(x), client.getStackInSlot(x))) {
                    if (changedSlots == null) {
                        changedSlots = new IntArrayList();
                    }
                    changedSlots.add(x);
                }
            }
            return changedSlots;
        }

        private static boolean isDifferent(ItemStack a, ItemStack b) {
            if (a.isEmpty() && b.isEmpty()) {
                return false;
            }

            if (a.isEmpty() || b.isEmpty()) {
                return true;
            }

            return !ItemStack.matches(a, b);
        }
    }

    @Nullable
    private static ServerLevel getContainerLevel(PatternContainer container) {
        if (container instanceof PatternProviderLogicHost host) {
            var block = host.getBlockEntity();
            if (block != null && block.getLevel() instanceof ServerLevel serverLevel) {
                return serverLevel;
            }
        }

        // assembler matrix of extendedAE
        if (container instanceof BlockEntity block) {
            if (block.getLevel() instanceof ServerLevel serverLevel) {
                return serverLevel;
            }
        }

        try {
            // machine of GT series are wrapped in IMachineBlockEntity
            var machine = container.getClass().getMethod("getLevel");
            var level = machine.invoke(container);
            if (level instanceof ServerLevel serverLevel) {
                return serverLevel;
            }
        } catch (Exception ignore) {
        }

        try {
            var machine = container.getClass().getMethod("getBlockEntity");
            var block = machine.invoke(container);
            if (block instanceof BlockEntity b && b.getLevel() instanceof ServerLevel serverLevel) {
                return serverLevel;
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    @Nullable
    private static BlockPos getContainerPos(PatternContainer container) {
        if (container instanceof PatternProviderLogicHost host) {
            return host.getBlockEntity().getBlockPos();
        }

        // assembler matrix of extendedAE
        if (container instanceof BlockEntity block) {
            return block.getBlockPos();
        }

        try {
            var machine = container.getClass().getMethod("getPos");
            return (BlockPos) machine.invoke(container);
        } catch (Exception ignore) {
        }

        try {
            var machine = container.getClass().getMethod("getBlockPos");
            return (BlockPos) machine.invoke(container);
        } catch (Exception ignore) {
        }
        return null;
    }

    @Nullable
    private static Direction getContainerSide(PatternContainer container) {
        if (container instanceof PatternProviderPart pp) {
            return pp.getSide();
        }
        // extendedAE's pattern provider part
        if (ExtendedAECompat.LOADED) {
            return ExtendedAECompat.getSide(container);
        }
        // all GT series are blocks, not parts
        return null;
    }

    private static Class<? extends PatternContainer> tryCastMachineToContainer(Class<?> machineClass) {
        if (PatternContainer.class.isAssignableFrom(machineClass)) {
            return machineClass.asSubclass(PatternContainer.class);
        }
        return null;
    }
}
