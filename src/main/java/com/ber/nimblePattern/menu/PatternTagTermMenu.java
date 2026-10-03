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
import appeng.menu.guisync.GuiSync;
import appeng.menu.slot.DisabledSlot;
import appeng.menu.slot.FakeSlot;
import appeng.menu.slot.InaccessibleSlot;
import appeng.menu.slot.RestrictedInputSlot;
import appeng.parts.crafting.PatternProviderPart;
import appeng.util.inv.AppEngInternalInventory;
import com.ber.nimblePattern.compat.extendedae.ExtendedAECompat;
import com.ber.nimblePattern.helpers.IPatternTagMenuHost;
import com.ber.nimblePattern.network.*;
import com.ber.nimblePattern.item.storage.LoopStorageCellItem;
import com.ber.nimblePattern.menu.slot.LoopStorageCellSlot;
import com.ber.nimblePattern.parts.PatternTagLogic;
import com.ber.nimblePattern.parts.TagMode;
import com.ber.nimblePattern.pattern.LoopPatternParser;
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
import net.minecraft.network.chat.Component;
import appeng.api.stacks.GenericStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static appeng.helpers.InventoryAction.PICKUP_OR_SET_DOWN;
import static appeng.helpers.InventoryAction.SPLIT_OR_PLACE_SINGLE;
import static com.ber.nimblePattern.parts.PatternTagLogic.INPUT_PATTERN_SLOTS;

public class PatternTagTermMenu extends AEBaseMenu {
    public static final MenuType<PatternTagTermMenu> TYPE = MenuTypeBuilder
            .create(PatternTagTermMenu::new, IPatternTagMenuHost.class)
            .build("patterntagterminal");

    private static long inventorySerial = Long.MIN_VALUE;
    // Pattern provider -> Container information
    private final Map<PatternContainer, ContainerTracker> diList = new IdentityHashMap<>();
    // Pattern provider temp id -> Container information
    private final Long2ObjectOpenHashMap<ContainerTracker> byId = new Long2ObjectOpenHashMap<>();

    private static final String ACTION_SET_MODE = "setMode";

    @GuiSync(97)
    public TagMode mode = TagMode.UPGRADE;

    public static final SlotSemantic INPUT_PATTERN = SlotSemantics.register("INPUT_PATTERN", false);
    public static final SlotSemantic TOOL_INPUT = SlotSemantics.register("TOOL_INPUT", false);
    public static final SlotSemantic CONDITION_ITEM = SlotSemantics.register("CONDITION_ITEM", false);
    public static final SlotSemantic LOOP_INPUT = SlotSemantics.register("LOOP_INPUT", false);
    public static final SlotSemantic LOOP_OUTPUT = SlotSemantics.register("LOOP_OUTPUT", false);
    public static final SlotSemantic LOOP_LOCK_STORAGE_CELL = SlotSemantics.register("LOOP_LOCK_STORAGE_CELL", false);
    private final IPatternTagMenuHost host;
    private final PatternTagLogic tagLogic;
    private final InternalInventory inputPatternInv;
    private final InternalInventory conditionItemInv;
    private final InternalInventory loopEndpointInv;
    private final InternalInventory loopStorageCellInv;
    private final RestrictedInputSlot[] inputPatternSlots = new RestrictedInputSlot[INPUT_PATTERN_SLOTS];
    // dummy pattern provider, used for rendering the blank slots in the last row
    public static final long VIRTUAL_ID = Long.MAX_VALUE;
    public static final AppEngInternalInventory VIRTUAL_INV = new AppEngInternalInventory(9);
    private final FakeSlot conditionItemSlot;
    private final FakeSlot loopInputSlot;
    private final FakeSlot loopOutputSlot;
    private final RestrictedInputSlot loopStorageCellSlot;
    private Set<String> conditionsHistory = new LinkedHashSet<>();
    // Number of patterns currently using each non-empty upgrade condition.
    // Incremental updates only need to touch conditions of changed slots.
    private final Map<String, Integer> conditionRefCounts = new HashMap<>();

    public PatternTagTermMenu(int id, Inventory ip, IPatternTagMenuHost host) {
        this(TYPE, id, ip, host, true);
    }

    public PatternTagTermMenu(MenuType<?> menuType, int id, Inventory ip, IPatternTagMenuHost host, boolean bindInventory) {
        super(menuType, id, ip, host);
        this.host = host;
        this.tagLogic = host.getLogic();
        this.mode = tagLogic.getMode();
        // input pattern slots (shared)
        this.inputPatternInv = tagLogic.getInputPatternInv();
        for (int i = 0; i < INPUT_PATTERN_SLOTS; i++) {
            var slot = new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN, inputPatternInv, i);
            slot.setStackLimit(1);
            this.inputPatternSlots[i] = slot;
            this.addSlot(slot, INPUT_PATTERN);
        }
        // upgrade panel
        this.conditionItemInv = tagLogic.getConditionItemInv();
        this.conditionItemSlot = new FakeSlot(conditionItemInv, 0);
        this.addSlot(conditionItemSlot, CONDITION_ITEM);
        // loop panel
        this.loopEndpointInv = tagLogic.getLoopEndpointInv();
        this.loopInputSlot = new FakeSlot(loopEndpointInv, 0);
        this.loopOutputSlot = new FakeSlot(loopEndpointInv, 1);
        this.addSlot(loopInputSlot, LOOP_INPUT);
        this.addSlot(loopOutputSlot, LOOP_OUTPUT);
        this.loopStorageCellInv = tagLogic.getLoopStorageCellInv();
        this.loopStorageCellSlot = new LoopStorageCellSlot(loopStorageCellInv, 0);
        this.addSlot(loopStorageCellSlot, LOOP_LOCK_STORAGE_CELL);
        for (int i = 0; i < 4; i++) {
            this.addSlot(new FakeSlot(tagLogic.getToolInv(), i), TOOL_INPUT);
        }
        if (bindInventory) {
            this.createPlayerInventorySlots(ip);
        }
        registerClientAction("applyCondition", String.class, this::applyCondition);
        registerClientAction("clearCondition", this::clearCondition);
        registerClientAction("applyLoop", this::applyLoop);
        registerClientAction("applyTools", this::applyTools);
        registerClientAction(ACTION_SET_MODE, TagMode.class, this::setMode);
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

    public FakeSlot getLoopInputSlot() {
        return loopInputSlot;
    }

    public FakeSlot getLoopOutputSlot() {
        return loopOutputSlot;
    }

    public RestrictedInputSlot getLoopStorageCellSlot() {
        return loopStorageCellSlot;
    }

    public TagMode getMode() {
        return this.mode;
    }

    public void setMode(TagMode mode) {
        if (isClientSide()) {
            sendClientAction(ACTION_SET_MODE, mode);
        } else {
            this.mode = mode;
            tagLogic.setMode(mode);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void broadcastChanges() {
        if (isClientSide()) {
            return;
        }
        if (this.mode != tagLogic.getMode()) {
            this.mode = tagLogic.getMode();
        }
        super.broadcastChanges();
        long now = getPlayer().level().getGameTime();
        if (lastNetworkCheck != Long.MIN_VALUE && now - lastNetworkCheck < 5) return;
        lastNetworkCheck = now;
        IGrid grid = getGrid();
        var state = new PatternTagTermMenu.VisitorState();
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

    private static String getCondition(ItemStack pattern) {
        if (pattern.isEmpty()) {
            return "";
        }
        var condition = NimblePatternTag.getCondition(pattern);
        return condition == null ? "" : condition;
    }

    private long lastNetworkCheck = Long.MIN_VALUE;
    private boolean conditionsDirty = true;

    private void addCondition(ItemStack pattern) {
        var condition = getCondition(pattern);
        if (!condition.isBlank()) {
            conditionRefCounts.merge(condition, 1, Integer::sum);
            conditionsDirty = true;
        }
    }

    private void removeCondition(ItemStack pattern) {
        var condition = getCondition(pattern);
        if (condition.isBlank()) {
            return;
        }
        conditionRefCounts.computeIfPresent(condition, (key, count) -> count <= 1 ? null : count - 1);
        conditionsDirty = true;
    }

    private void updateCondition(ItemStack oldPattern, ItemStack newPattern) {
        var oldCondition = getCondition(oldPattern);
        var newCondition = getCondition(newPattern);
        if (oldCondition.equals(newCondition)) {
            return;
        }
        removeCondition(oldPattern);
        addCondition(newPattern);
    }

    private void syncConditionsIfChanged() {
        if (!conditionsDirty) return;
        conditionsDirty = false;
        Set<String> conditions = conditionRefCounts.keySet().stream()
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (conditions.equals(this.conditionsHistory)) {
            return;
        }

        this.conditionsHistory = conditions;
        if (getPlayer() instanceof ServerPlayer serverPlayer) {
            NimblePatternNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new ConditionPacket(conditionsHistory));
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
            } else {
                inputPatternInv.setItemDirect(i, pattern);
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

    public void applyLoop() {
        if (isClientSide()) {
            sendClientAction("applyLoop");
            return;
        }

        var entry = GenericStack.fromItemStack(loopInputSlot.getItem());
        var exit = GenericStack.fromItemStack(loopOutputSlot.getItem());
        var cellStack = loopStorageCellSlot.getItem();
        if (!(cellStack.getItem() instanceof LoopStorageCellItem cellItem)) {
            showLoopResult("missing_cell");
            return;
        }

        var patterns = new ArrayList<ItemStack>();
        var patternSlots = new ArrayList<Integer>();
        for (int i = 0; i < inputPatternInv.size(); i++) {
            var pattern = inputPatternInv.getStackInSlot(i);
            if (!pattern.isEmpty()) {
                patterns.add(pattern.copy());
                patternSlots.add(i);
            }
        }

        try {
            var cellId = cellItem.getOrCreateCellId(cellStack);
            var result = LoopPatternParser.parse(getPlayer().level(), patterns, entry, exit, cellId);
            if (!cellItem.canAddConfiguredAmount(cellStack, result.entry(), result.seedAmount())) {
                showLoopResult("cell_capacity");
                return;
            }
            for (int i = 0; i < result.patterns().size(); i++) {
                NimblePatternTag.tagLoop(result.patterns().get(i), result.metadata().get(i));
            }
            cellItem.addConfiguredAmount(cellStack, result.entry(), result.seedAmount());
            loopStorageCellInv.setItemDirect(0, cellStack);

            for (int i = 0; i < result.patterns().size(); i++) {
                var pattern = result.patterns().get(i);
                int terminalSlot = patternSlots.get(result.sourceIndices().get(i));
                if (NimblePatternTag.pushPatternBack(pattern, getPlayer().getServer())) {
                    inputPatternInv.setItemDirect(terminalSlot, ItemStack.EMPTY);
                } else {
                    // The provider may have been removed or the pattern may have been inserted manually. Keep the
                    // tagged stack in the terminal so the player can return it themselves.
                    inputPatternInv.setItemDirect(terminalSlot, pattern);
                }
            }
            showLoopResult("success");
        } catch (LoopPatternParser.ParseException e) {
            showLoopResult(e.reason());
        } catch (ArithmeticException e) {
            showLoopResult("amount_overflow");
        }
    }

    private void showLoopResult(String result) {
        getPlayer().displayClientMessage(
                Component.translatable("gui.nimble_pattern.pattern_tag_terminal.loop." + result), false);
    }

    public void applyTools() {
        if (isClientSide()) {
            sendClientAction("applyTools");
            return;
        }
        var tools = new LinkedHashSet<appeng.api.stacks.AEItemKey>();
        for (int i = 0; i < 4; i++) {
            var stack = tagLogic.getToolInv().getStackInSlot(i);
            if (stack.isEmpty()) continue;
            var generic = GenericStack.fromItemStack(stack);
            if (generic == null || !(generic.what() instanceof appeng.api.stacks.AEItemKey key)) {
                showToolResult("invalid_tool");
                return;
            }
            tools.add(key);
        }
        if (tools.isEmpty()) {
            showToolResult("missing_tools");
            return;
        }
        // Validate the whole selection before changing any physical pattern or provider inventory.
        var tagged = new LinkedHashMap<Integer, ItemStack>();
        try {
            for (int i = 0; i < inputPatternInv.size(); i++) {
                var stack = inputPatternInv.getStackInSlot(i).copy();
                if (stack.isEmpty()) continue;
                var decoded = PatternDetailsHelper.decodePattern(stack, getPlayer().level());
                if (decoded == null) throw new IllegalArgumentException("invalid_pattern");
                var data = com.ber.nimblePattern.pattern.ToolPatternData.validate(decoded, List.copyOf(tools));
                com.ber.nimblePattern.pattern.ToolPatternData.write(stack, data);
                tagged.put(i, stack);
            }
            if (tagged.isEmpty()) throw new IllegalArgumentException("invalid_pattern");
        } catch (IllegalArgumentException e) {
            showToolResult(e.getMessage());
            return;
        } catch (ArithmeticException e) {
            showToolResult("invalid_pattern");
            return;
        }
        tagged.forEach((slot, stack) -> inputPatternInv.setItemDirect(slot,
                NimblePatternTag.pushPatternBack(stack, getPlayer().getServer()) ? ItemStack.EMPTY : stack));
        showToolResult("success");
    }

    private void showToolResult(String reason) {
        getPlayer().displayClientMessage(Component.translatable(
                "gui.nimble_pattern.pattern_tag_terminal.tool." + reason), false);
    }

    private void sendFullUpdate(@Nullable IGrid grid) {
        conditionsDirty = true;
        this.byId.clear();
        this.diList.clear();
        this.conditionRefCounts.clear();

        if (getPlayer() instanceof ServerPlayer serverPlayer) {
            NimblePatternNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new ClearPacket());
        }

        if (grid == null) {
            if (getPlayer() instanceof ServerPlayer serverPlayer) {
                NimblePatternNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new ConditionPacket(Set.of()));
            }
            this.conditionsHistory = new LinkedHashSet<>();
            return;
        }

        for (var machineClass : grid.getMachineClasses()) {
            var containerClass = tryCastMachineToContainer(machineClass);
            if (containerClass == null) {
                continue;
            }

            for (var container : grid.getActiveMachines(containerClass)) {
                this.diList.put(container, new PatternTagTermMenu.ContainerTracker(container, container.getTerminalPatternInventory()));
            }
        }

        for (var inv : this.diList.values()) {
            this.byId.put(inv.serverId, inv);
            var packet = inv.createFullPacket(this::addCondition);
            if (getPlayer() instanceof ServerPlayer serverPlayer) {
                NimblePatternNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), packet);
            }
        }

        // After all pattern packets sent, trigger a sorting
        if (getPlayer() instanceof ServerPlayer serverPlayer) {
            NimblePatternNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new PatternSyncCompletePacket());
        }

        syncConditionsIfChanged();
    }

    private void sendIncrementalUpdate() {
        for (var inv : this.diList.values()) {
            var packet = inv.createUpdatePacket(this::updateCondition);
            if (packet != null && getPlayer() instanceof ServerPlayer serverPlayer) {
                NimblePatternNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), packet);
            }
        }
        syncConditionsIfChanged();
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
        @FunctionalInterface
        private interface FullSlotConsumer {
            void accept(ItemStack pattern);
        }

        @FunctionalInterface
        private interface ChangedSlotConsumer {
            void accept(ItemStack oldPattern, ItemStack newPattern);
        }

        private final PatternContainer container;
        private final long serverId = inventorySerial++;
        // This is used to track the inventory contents we sent to the client for change detection
        private final InternalInventory client;
        // This is a reference to the real inventory used by this machine
        private final InternalInventory server;
        private long lastRevision = -1;

        public ContainerTracker(PatternContainer container, InternalInventory patterns) {
            this.container = container;
            this.server = patterns;
            this.client = new AppEngInternalInventory(this.server.size());
        }

        public PatternPacket createFullPacket(FullSlotConsumer conditionConsumer) {
            var slots = new Int2ObjectArrayMap<ItemStack>(server.size());
            for (int i = 0; i < server.size(); i++) {
                var stack = server.getStackInSlot(i);
                client.setItemDirect(i, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
                if (!stack.isEmpty()) {
                    slots.put(i, stack);
                    conditionConsumer.accept(stack);
                }
            }
            return PatternPacket.fullUpdate(serverId, server.size(), slots);
        }

        @Nullable
        public PatternPacket createUpdatePacket(ChangedSlotConsumer conditionConsumer) {
            long revision = com.ber.nimblePattern.pattern.PatternInventorySnapshots.get(server).revision();
            if (revision == lastRevision) return null;
            lastRevision = revision;
            var changedSlots = detectChangedSlots();
            if (changedSlots == null) {
                return null;
            }

            var slots = new Int2ObjectArrayMap<ItemStack>(changedSlots.size());
            for (int i = 0; i < changedSlots.size(); i++) {
                var slot = changedSlots.getInt(i);
                var stack = server.getStackInSlot(slot);
                var oldStack = client.getStackInSlot(slot);
                // Only changed slots need condition bookkeeping.
                conditionConsumer.accept(oldStack, stack);
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
