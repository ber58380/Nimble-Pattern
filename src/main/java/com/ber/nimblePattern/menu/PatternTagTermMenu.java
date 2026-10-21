package com.ber.nimblePattern.menu;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AECraftingPattern;
import appeng.helpers.InventoryAction;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.slot.DisabledSlot;
import appeng.menu.slot.FakeSlot;
import appeng.menu.slot.InaccessibleSlot;
import appeng.menu.slot.RestrictedInputSlot;
import appeng.parts.crafting.PatternProviderPart;
import appeng.util.inv.AppEngInternalInventory;
import com.ber.nimblePattern.client.gui.widgets.LoopDiagramData;
import com.ber.nimblePattern.compat.extendedae.ExtendedAECompat;
import com.ber.nimblePattern.helpers.IPatternTagMenuHost;
import com.ber.nimblePattern.item.storage.LoopStorageCellItem;
import com.ber.nimblePattern.menu.slot.LoopStorageCellSlot;
import com.ber.nimblePattern.network.ClearPacket;
import com.ber.nimblePattern.network.NimblePatternNetwork;
import com.ber.nimblePattern.network.PatternPacket;
import com.ber.nimblePattern.network.PatternSyncCompletePacket;
import com.ber.nimblePattern.parts.PatternLayout;
import com.ber.nimblePattern.parts.PatternTagLogic;
import com.ber.nimblePattern.parts.TagMode;
import com.ber.nimblePattern.pattern.NimblePatternTag;
import com.ber.nimblePattern.pattern.PatternValidation;
import com.ber.nimblePattern.pattern.loop.LoopPatternClearPlan;
import com.ber.nimblePattern.pattern.loop.LoopPatternParser;
import com.ber.nimblePattern.pattern.upgrade.PatternInventorySnapshots;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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

import java.util.*;

import static appeng.helpers.InventoryAction.PICKUP_OR_SET_DOWN;
import static appeng.helpers.InventoryAction.SPLIT_OR_PLACE_SINGLE;
import static com.ber.nimblePattern.menu.SlotSemantics.*;
import static com.ber.nimblePattern.parts.PatternTagLogic.INPUT_PATTERN_SLOTS;

public class PatternTagTermMenu extends AEBaseMenu {
    // dummy pattern provider, used for rendering the blank slots in the last row
    public static final long VIRTUAL_ID = Long.MAX_VALUE;
    public static final MenuType<PatternTagTermMenu> TYPE = MenuTypeBuilder
            .create(PatternTagTermMenu::new, IPatternTagMenuHost.class)
            .build("patterntagterminal");
    public static final AppEngInternalInventory VIRTUAL_INV = new AppEngInternalInventory(9);
    private static long inventorySerial = Long.MIN_VALUE;
    // Pattern provider -> Container information
    private final Map<PatternContainer, ContainerTracker> diList = new IdentityHashMap<>();
    // Pattern provider temp id -> Container information
    private final Long2ObjectOpenHashMap<ContainerTracker> byId = new Long2ObjectOpenHashMap<>();
    private final IPatternTagMenuHost host;
    private final PatternTagLogic tagLogic;
    private final InternalInventory inputPatternInv;
    private final InternalInventory conditionItemInv;
    private final InternalInventory loopEndpointInv;
    private final InternalInventory loopStorageCellInv;
    private final InternalInventory toolInputInv;
    private final RestrictedInputSlot[] inputPatternSlots = new RestrictedInputSlot[INPUT_PATTERN_SLOTS];
    private final FakeSlot conditionItemSlot;
    private final FakeSlot loopInputSlot;
    private final FakeSlot loopOutputSlot;
    private final LoopStorageCellSlot loopStorageCellSlot;
    private final FakeSlot toolInputSlot;

    @GuiSync(97)
    public TagMode mode;
    @GuiSync(98)
    public PatternLayout layout;

    private long lastNetworkCheck = Long.MIN_VALUE;

    @GuiSync(99)
    public Component dialogTitle = Component.empty();
    @GuiSync(100)
    public Component dialogMessage = Component.empty();
    @GuiSync(101)
    public LoopDiagramData dialogLoopDiagram = LoopDiagramData.EMPTY;
    @GuiSync(102)
    public long dialogRevision = 0;
    private static final Component LOOP_ERROR_TITLE = Component.translatable("gui.nimble_pattern.tag_loop_pattern_failure").withStyle(ChatFormatting.RED);
    private static final Component LOOP_SUCCESS_TITLE = Component.translatable("gui.nimble_pattern.tag_loop_pattern_success").withStyle(ChatFormatting.DARK_GREEN);
    private static final Component TOOL_ERROR_TITLE = Component.translatable("gui.nimble_pattern.tag_tool_pattern_failure").withStyle(ChatFormatting.RED);

    public PatternTagTermMenu(int id, Inventory ip, IPatternTagMenuHost host) {
        this(TYPE, id, ip, host, true);
    }

    public PatternTagTermMenu(MenuType<?> menuType, int id, Inventory ip, IPatternTagMenuHost host, boolean bindInventory) {
        super(menuType, id, ip, host);
        this.host = host;
        this.tagLogic = host.getLogic();
        this.mode = tagLogic.getMode();
        this.layout = tagLogic.getLayout();
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
        this.conditionItemSlot.setHideAmount(true);
        this.addSlot(conditionItemSlot, UPGRADE_CONDITION);
        // loop panel
        this.loopEndpointInv = tagLogic.getLoopEndpointInv();
        this.loopStorageCellInv = tagLogic.getLoopStorageCellInv();
        this.loopInputSlot = new FakeSlot(loopEndpointInv, 0);
        this.loopInputSlot.setHideAmount(true);
        this.loopOutputSlot = new FakeSlot(loopEndpointInv, 1);
        this.loopOutputSlot.setHideAmount(true);
        this.loopStorageCellSlot = new LoopStorageCellSlot(loopStorageCellInv, 0);
        this.addSlot(loopInputSlot, LOOP_INPUT);
        this.addSlot(loopOutputSlot, LOOP_OUTPUT);
        this.addSlot(loopStorageCellSlot, LOOP_STORAGE_CELL);
        // tool panel
        this.toolInputInv = tagLogic.getToolInputInv();
        this.toolInputSlot = new FakeSlot(toolInputInv, 0);
        this.toolInputSlot.setHideAmount(true);
        this.addSlot(toolInputSlot, TOOL_INPUT);

        if (bindInventory) {
            this.createPlayerInventorySlots(ip);
        }
        registerClientAction("setMode", TagMode.class, this::setMode);
        registerClientAction("setLayout", PatternLayout.class, this::setLayout);
        registerClientAction("applyCondition", this::applyUpgradeCondition);
        registerClientAction("clearCondition", this::clearUpgradeCondition);
        registerClientAction("applyLoop", this::applyLoop);
        registerClientAction("clearLoop", this::clearLoop);
        registerClientAction("applyTool", this::applyTool);
        registerClientAction("clearTool", this::clearTool);
        registerClientAction("applyProbability", String.class, this::applyProbability);
        registerClientAction("clearProbability", this::clearProbability);
    }

    public RestrictedInputSlot[] getInputPatternSlots() {
        return inputPatternSlots;
    }

    public FakeSlot getConditionItemSlot() {
        return conditionItemSlot;
    }

    public FakeSlot getToolInputSlot() {
        return toolInputSlot;
    }

    public TagMode getMode() {
        return this.mode;
    }

    public PatternLayout getLayout() {
        return layout;
    }

    public void setLayout(PatternLayout layout) {
        if (layout == null) {
            return;
        }
        if (isClientSide()) {
            sendClientAction("setLayout", layout);
            return;
        }
        tagLogic.setLayout(layout);
        this.layout = tagLogic.getLayout();
    }

    public void setMode(TagMode mode) {
        if (isClientSide()) {
            sendClientAction("setMode", mode);
            return;
        }
        this.mode = mode;
        tagLogic.setMode(mode);
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

    @SuppressWarnings("unchecked")
    @Override
    public void broadcastChanges() {
        if (isClientSide()) {
            return;
        }
        if (this.mode != tagLogic.getMode()) {
            this.mode = tagLogic.getMode();
        }
        this.layout = tagLogic.getLayout();
        super.broadcastChanges();
        long now = getPlayer().level().getGameTime();
        if (lastNetworkCheck != Long.MIN_VALUE && now - lastNetworkCheck < 5) {
            return;
        }
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

    private <T extends PatternContainer> void visitPatternProviderHosts(IGrid grid, Class<T> machineClass, VisitorState state) {
        for (var container : grid.getActiveMachines(machineClass)) {
            var t = this.diList.get(container);
            if (t == null) {
                state.forceFullUpdate = true;
            }
            state.total++;
        }
    }

    public void applyUpgradeCondition() {
        if (isClientSide()) {
            sendClientAction("applyCondition");
            return;
        }
        var stack = GenericStack.fromItemStack(getConditionItemSlot().getItem());
        if (stack == null) {
            return;
        }
        var key = stack.what();
        if (!(key instanceof AEItemKey) && !(key instanceof AEFluidKey)) {
            return;
        }
        var condition = key.dropSecondary();
        for (int i = 0; i < inputPatternInv.size(); i++) {
            var pattern = inputPatternInv.getStackInSlot(i).copy();
            if (pattern.isEmpty()) {
                continue;
            }
            NimblePatternTag.tagUpgrade(pattern, condition);
            if (NimblePatternTag.pushPatternBack(pattern, getPlayer().getServer())) {
                inputPatternInv.setItemDirect(i, ItemStack.EMPTY);
            } else {
                inputPatternInv.setItemDirect(i, pattern);
            }
        }
    }

    public void clearUpgradeCondition() {
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

    private void showDialogBox(Component title, Component message) {
        this.dialogLoopDiagram = LoopDiagramData.EMPTY;
        this.dialogTitle = title;
        this.dialogMessage = message;
        this.dialogRevision++;
    }

    public void applyLoop() {
        if (isClientSide()) {
            sendClientAction("applyLoop");
            return;
        }
        var entry = GenericStack.fromItemStack(loopInputSlot.getItem());
        var exit = GenericStack.fromItemStack(loopOutputSlot.getItem());
        var cellStack = loopStorageCellSlot.getItem();
        if (!(cellStack.getItem() instanceof LoopStorageCellItem cell)) {
            showDialogBox(LOOP_ERROR_TITLE, Component.translatable("gui.nimble_pattern.missing_loop_storage_cell"));
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
            var cellId = cell.getOrCreateCellId(cellStack);
            var loopChain = LoopPatternParser.parse(getPlayer().level(), patterns, entry, exit, cellId);
            if (!cell.canAddConfiguredAmount(cellStack, loopChain.entry(), loopChain.seedAmount())) {
                showDialogBox(LOOP_ERROR_TITLE, Component.translatable("gui.nimble_pattern.loop_storage_cell_not_enough_capacity"));
                return;
            }
            for (int i = 0; i < loopChain.patterns().size(); i++) {
                NimblePatternTag.tagLoop(loopChain.patterns().get(i), loopChain.metadata().get(i));
            }
            cell.addConfiguredAmount(cellStack, loopChain.entry(), loopChain.seedAmount());
            loopStorageCellInv.setItemDirect(0, cellStack);

            for (int i = 0; i < loopChain.patterns().size(); i++) {
                var pattern = loopChain.patterns().get(i);
                int terminalSlot = patternSlots.get(loopChain.indices().get(i));
                if (NimblePatternTag.pushPatternBack(pattern, getPlayer().getServer())) {
                    inputPatternInv.setItemDirect(terminalSlot, ItemStack.EMPTY);
                } else {
                    inputPatternInv.setItemDirect(terminalSlot, pattern);
                }
            }
            showDialogBox(LOOP_SUCCESS_TITLE, Component.empty());
            this.dialogLoopDiagram = LoopDiagramData.transform(loopChain.metadata().get(0));
        } catch (LoopPatternParser.ParseException e) {
            showDialogBox(LOOP_ERROR_TITLE, Component.translatable("gui.nimble_pattern." + e.getReason()));
        } catch (ArithmeticException e) {
            showDialogBox(LOOP_ERROR_TITLE, Component.translatable("gui.nimble_pattern.loop_amount_overflow"));
        }
    }

    public void clearLoop() {
        if (isClientSide()) {
            sendClientAction("clearLoop");
            return;
        }
        var cellStack = loopStorageCellSlot.getItem().copy();
        if (!(cellStack.getItem() instanceof LoopStorageCellItem cell)) {
            showDialogBox(LOOP_ERROR_TITLE, Component.translatable("gui.nimble_pattern.missing_loop_storage_cell"));
            return;
        }
        var grid = getGrid();
        if (grid == null) {
            showDialogBox(LOOP_ERROR_TITLE, Component.translatable("gui.nimble_pattern.missing_network"));
            return;
        }
        var patterns = new LinkedHashMap<Integer, ItemStack>();
        for (int i = 0; i < inputPatternInv.size(); i++) {
            if (!inputPatternInv.getStackInSlot(i).isEmpty()) {
                patterns.put(i, inputPatternInv.getStackInSlot(i));
            }
        }
        try {
            var plan = LoopPatternClearPlan.prepare(patterns, cell.getCellId(cellStack));
            boolean cleared = cell.clearConfiguredAmounts(cellStack, plan.seeds(),
                    grid.getStorageService().getInventory(), getActionSource());
            // Preserve the exact stock count even if actual insertion accepted only part of the materials.
            loopStorageCellInv.setItemDirect(0, cellStack);
            if (!cleared) {
                showDialogBox(LOOP_ERROR_TITLE, Component.translatable("gui.nimble_pattern.return_seed_failure"));
                return;
            }
            plan.patterns().forEach(inputPatternInv::setItemDirect);
        } catch (LoopPatternParser.ParseException e) {
            showDialogBox(LOOP_ERROR_TITLE, Component.translatable("gui.nimble_pattern." + e.getReason()));
        } catch (ArithmeticException e) {
            showDialogBox(LOOP_ERROR_TITLE, Component.translatable("gui.nimble_pattern.loop_amount_overflow"));
        } catch (IllegalArgumentException e) {
            showDialogBox(LOOP_ERROR_TITLE, Component.translatable("gui.nimble_pattern.wrong_loop_storage_cell"));
        }
        loopInputSlot.set(ItemStack.EMPTY);
        loopOutputSlot.set(ItemStack.EMPTY);
    }

    public void applyTool() {
        if (isClientSide()) {
            sendClientAction("applyTool");
            return;
        }
        var tool = GenericStack.fromItemStack(toolInputSlot.getItem());
        if (tool == null || !(tool.what() instanceof AEItemKey key)) {
            showDialogBox(TOOL_ERROR_TITLE, Component.translatable("gui.nimble_pattern.no_tool"));
            return;
        }
        for (int i = 0; i < inputPatternInv.size(); i++) {
            var stack = inputPatternInv.getStackInSlot(i).copy();
            if (stack.isEmpty()) {
                continue;
            }
            var decoded = PatternDetailsHelper.decodePattern(stack, getPlayer().level());
            if (!(decoded instanceof AECraftingPattern pattern)) {
                showDialogBox(TOOL_ERROR_TITLE, Component.translatable("gui.nimble_pattern.crafting_only"));
                return;
            }

            List<GenericStack> tools;
            try {
                tools = PatternValidation.isToolPattern(pattern, key);
            } catch (IllegalArgumentException e) {
                showDialogBox(TOOL_ERROR_TITLE, Component.translatable("gui.nimble_pattern." + e.getMessage()));
                return;
            }
            if (!tools.isEmpty()) {
                NimblePatternTag.tagTool(stack, tools);
                if (NimblePatternTag.pushPatternBack(stack, getPlayer().getServer())) {
                    inputPatternInv.setItemDirect(i, ItemStack.EMPTY);
                } else {
                    inputPatternInv.setItemDirect(i, stack);
                }
            }
        }
    }

    public void clearTool() {
        if (isClientSide()) {
            sendClientAction("clearTool");
            return;
        }
        toolInputSlot.set(ItemStack.EMPTY);
        for (int i = 0; i < inputPatternInv.size(); i++) {
            var pattern = inputPatternInv.getStackInSlot(i).copy();
            if (pattern.isEmpty()) {
                continue;
            }
            NimblePatternTag.removeToolTag(pattern);
            inputPatternInv.setItemDirect(i, pattern);
        }
    }

    public void applyProbability(String secondsText) {
        if (isClientSide()) {
            sendClientAction("applyProbability", secondsText);
            return;
        }
        if (secondsText.isEmpty()) {
            return;
        }
        long maxWaitingTicks = Long.parseLong(secondsText) * 20L;
        if (maxWaitingTicks == 0) {
            return;
        }
        for (int i = 0; i < inputPatternInv.size(); i++) {
            var pattern = inputPatternInv.getStackInSlot(i).copy();
            if (pattern.isEmpty()) {
                continue;
            }
            NimblePatternTag.tagProbability(pattern, maxWaitingTicks);
            if (NimblePatternTag.pushPatternBack(pattern, getPlayer().getServer())) {
                inputPatternInv.setItemDirect(i, ItemStack.EMPTY);
            } else {
                inputPatternInv.setItemDirect(i, pattern);
            }
        }
    }

    public void clearProbability() {
        if (isClientSide()) {
            sendClientAction("clearProbability");
            return;
        }
        for (int i = 0; i < inputPatternInv.size(); i++) {
            var pattern = inputPatternInv.getStackInSlot(i).copy();
            if (pattern.isEmpty()) {
                continue;
            }
            NimblePatternTag.removeProbabilityTag(pattern);
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
            var packet = inv.createFullPacket();
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
    }

    private void sendIncrementalUpdate() {
        for (var inv : this.diList.values()) {
            var packet = inv.createUpdatePacket();
            if (packet != null && getPlayer() instanceof ServerPlayer serverPlayer) {
                NimblePatternNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), packet);
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

    private static class VisitorState {
        // Total number of pattern provider hosts found
        int total;
        // Set to true if any visited machines were missing from diList, or had a different name
        boolean forceFullUpdate;
    }

    private static class ContainerTracker {
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

        private static boolean isDifferent(ItemStack a, ItemStack b) {
            if (a.isEmpty() && b.isEmpty()) {
                return false;
            }

            if (a.isEmpty() || b.isEmpty()) {
                return true;
            }

            return !ItemStack.matches(a, b);
        }

        public PatternPacket createFullPacket() {
            var slots = new Int2ObjectArrayMap<ItemStack>(server.size());
            for (int i = 0; i < server.size(); i++) {
                var stack = server.getStackInSlot(i);
                client.setItemDirect(i, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
                if (!stack.isEmpty()) {
                    slots.put(i, stack);
                }
            }
            return PatternPacket.fullUpdate(serverId, server.size(), slots);
        }

        @Nullable
        public PatternPacket createUpdatePacket() {
            long revision = PatternInventorySnapshots.getSnapshot(server, false).getRevision();
            if (revision == lastRevision) {
                return null;
            }
            lastRevision = revision;

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
    }


}
