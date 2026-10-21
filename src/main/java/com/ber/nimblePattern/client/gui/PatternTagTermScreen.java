package com.ber.nimblePattern.client.gui;

import appeng.api.client.AEKeyRendering;
import appeng.api.config.Settings;
import appeng.api.stacks.AEKey;
import appeng.client.Point;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.AESubScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.TerminalStyle;
import appeng.client.gui.widgets.*;
import appeng.core.AEConfig;
import appeng.core.definitions.AEBlocks;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.InventoryActionPacket;
import appeng.helpers.InventoryAction;
import com.ber.nimblePattern.client.gui.panel.*;
import com.ber.nimblePattern.client.gui.search.PatternSearch;
import com.ber.nimblePattern.client.gui.widgets.DialogBox;
import com.ber.nimblePattern.client.gui.widgets.PatternUpgradeSlot;
import com.ber.nimblePattern.menu.PatternTagTermMenu;
import com.ber.nimblePattern.parts.PatternLayout;
import com.ber.nimblePattern.parts.TagMode;
import com.ber.nimblePattern.pattern.NimblePatternTag;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.ber.nimblePattern.menu.PatternTagTermMenu.VIRTUAL_ID;
import static com.ber.nimblePattern.menu.PatternTagTermMenu.VIRTUAL_INV;
import static com.ber.nimblePattern.menu.SlotSemantics.INPUT_PATTERN;
import static com.ber.nimblePattern.parts.PatternTagLogic.*;

public class PatternTagTermScreen<C extends PatternTagTermMenu> extends AEBaseScreen<C> {
    private static final int COLUMNS = 9;
    private static final int MIN_ROWS = 2;
    private final TerminalStyle style;
    private final Scrollbar scrollbar;
    private final Scrollbar inputPatternScrollbar;
    private final AETextField searchField;
    private final PatternSearch search = new PatternSearch();
    private final Map<TagMode, TagModePanel> modePanels = new EnumMap<>(TagMode.class);
    private final Map<TagMode, TabButton> modeTabButtons = new EnumMap<>(TagMode.class);
    private final Long2ObjectOpenHashMap<PatternContainerRecord> byId = new Long2ObjectOpenHashMap<>();
    private int rows = 0;
    private final List<PatternRecord> patterns = new ArrayList<>();
    private final List<DisplayRow> displayRows = new ArrayList<>();
    private PatternLayout displayMode;
    private final IconButton displayModeButton;
    private static final int GROUP_HEADER_BACKGROUND = 0xFF9A9FB4;
    private static final int GROUP_HEADER_TOP_SHADOW = 0xFF696D88;

    // ClearPacket starts a full synchronization batch. Full PatternPackets only
    // populate byId; PatternSyncCompletePacket rebuilds the expensive global view once.
    private boolean fullUpdateInProgress = false;
    private boolean patternViewDirty = false;

    private long lastDialogRevision = 0;

    public PatternTagTermScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        this.style = style.getTerminalStyle();
        this.displayMode = menu.getLayout();
        addToLeftToolbar(new SettingToggleButton<>(Settings.TERMINAL_STYLE, AEConfig.instance().getTerminalStyle(), this::toggleTerminalStyle));
        this.displayModeButton = addToLeftToolbar(new IconButton(button -> toggleDisplayMode()) {
            @Override
            protected Icon getIcon() {
                return displayMode == PatternLayout.GRID ? Icon.VIEW_MODE_ALL : Icon.SORT_BY_NAME;
            }
        });
        updateDisplayModeButton();
        this.scrollbar = widgets.addScrollBar("scrollbar");
        this.searchField = widgets.addTextField("search");
        this.searchField.setPlaceholder(GuiText.SearchPlaceholder.text());
        this.searchField.setTooltipMessage(List.of(
                GuiText.SearchTooltip.text(),
                GuiText.SearchTooltipModId.text(),
                GuiText.SearchTooltipTag.text(),
                GuiText.SearchTooltipItemId.text(),
                Component.translatable("gui.nimble_pattern.pattern_tag_terminal.search_tooltip_condition"),
                Component.translatable("gui.nimble_pattern.pattern_tag_terminal.search_tooltip_status")
        ));
        this.searchField.setResponder(text -> {
            if (!fullUpdateInProgress) {
                rebuildPatternView();
            }
        });
        this.inputPatternScrollbar = widgets.addScrollBar("inputPatternScrollbar", Scrollbar.SMALL);
        this.inputPatternScrollbar.setRange(
                0,
                INPUT_PATTERN_TOTAL_ROWS - INPUT_PATTERN_VISIBLE_ROWS,
                INPUT_PATTERN_VISIBLE_ROWS);
        this.inputPatternScrollbar.setCaptureMouseWheel(false);

        for (var mode : TagMode.values()) {
            var panel = switch (mode) {
                case UPGRADE -> new UpgradePatternPanel(this, widgets);
                case LOOP -> new LoopPatternPanel(this, widgets);
                case TOOL -> new ToolPatternPanel(this, widgets);
                case PROBABILITY -> new ProbabilityPatternPanel(this, widgets);
            };
            var tabButton = panel.createTabButton(button -> getMenu().setMode(mode));
            tabButton.setStyle(TabButton.Style.HORIZONTAL);
            var modeIndex = modeTabButtons.size();
            widgets.add("modePanel" + modeIndex, panel);
            widgets.add("modeTabButton" + modeIndex, tabButton);
            modeTabButtons.put(mode, tabButton);
            modePanels.put(mode, panel);
        }
    }

    /**
     * The screen has a dialog box as subscreen. When it opens, the screen should still receive
     * packets, but the update should be delayed and executed when subscreen closed. Otherwise,
     * the update would lead slots rendering when dialog box is opened.
     */
    @Nullable
    public static PatternTagTermScreen<?> getOpenTerminal() {
        var minecraft = Minecraft.getInstance();
        var current = minecraft.screen;
        while (current instanceof AESubScreen<?, ?> subScreen) {
            current = subScreen.getParent();
        }
        if (current instanceof PatternTagTermScreen<?> terminal
                && minecraft.player != null
                && minecraft.player.containerMenu == terminal.getMenu()) {
            return terminal;
        }
        return null;
    }

    public void clear() {
        fullUpdateInProgress = true;
        byId.clear();
        patterns.clear();
        displayRows.clear();
        patternViewDirty = true;
        if (Minecraft.getInstance().screen == this) {
            menu.slots.removeIf(slot -> slot instanceof PatternUpgradeSlot);
            updateScrollbar();
        }
    }

    public void finishFullUpdate() {
        fullUpdateInProgress = false;
        rebuildPatternView();
    }

    private void rebuildPatternView() {
        patternViewDirty = true;
        if (fullUpdateInProgress || Minecraft.getInstance().screen != this) {
            return;
        }
        patternViewDirty = false;
        updatePatterns();
        updateDisplayRows();
        updateScrollbar();
        updateSlots();
    }

    @Override
    protected void init() {
        var availableHeight = height - 2 * AEConfig.instance().getTerminalMargin();
        this.rows = Math.max(MIN_ROWS, config.getTerminalStyle().getRows(style.getPossibleRows(availableHeight)));
        this.imageHeight = style.getScreenHeight(rows);
        super.init();
        this.updateScrollbar();
    }

    @Override
    protected <P extends AEBaseScreen<C>> void onReturnFromSubScreen(AESubScreen<C, P> subScreen) {
        super.onReturnFromSubScreen(subScreen);
        if (patternViewDirty && !fullUpdateInProgress) {
            rebuildPatternView();
        }
    }

    private void reinitialize() {
        new ArrayList<>(this.children()).forEach(this::removeWidget);
        this.init();
    }

    private void toggleTerminalStyle(SettingToggleButton<appeng.api.config.TerminalStyle> btn, boolean backwards) {
        appeng.api.config.TerminalStyle next = btn.getNextValue(backwards);
        config.setTerminalStyle(next);
        btn.set(next);
        this.reinitialize();
    }

    private void updatePatterns() {
        patterns.clear();
        search.setSearchString(searchField.getValue());
        for (var record : byId.values()) {
            var inventory = record.getInventory();
            for (int i = 0; i < inventory.size(); i++) {
                var stack = inventory.getStackInSlot(i);
                if (stack.isEmpty() || !search.matches(stack)) {
                    continue;
                }
                patterns.add(new PatternRecord(record.getServerId(), i, stack));
            }
        }
        patterns.sort(Comparator.comparing(PatternRecord::sortKey)
                .thenComparingLong(PatternRecord::serverId)
                .thenComparingInt(PatternRecord::machineSlot));
    }

    private void toggleDisplayMode() {
        menu.setLayout(menu.getLayout() == PatternLayout.GRID
                ? PatternLayout.GROUPED : PatternLayout.GRID);
    }

    private void updateDisplayModeButton() {
        displayModeButton.setMessage(Component.translatable(
                displayMode == PatternLayout.GRID
                        ? "gui.nimble_pattern.pattern_tag_terminal.layout_grid"
                        : "gui.nimble_pattern.pattern_tag_terminal.layout_condition"));
    }

    private void updateDisplayRows() {
        displayRows.clear();
        if (displayMode == PatternLayout.GRID) {
            appendPatternRows(patterns);
            return;
        }

        // LinkedHashMap preserves deterministic tie ordering from the sorted patterns.
        var groups = new LinkedHashMap<AEKey, List<PatternRecord>>();
        var unassigned = new ArrayList<PatternRecord>();
        for (var pattern : patterns) {
            var condition = NimblePatternTag.getCondition(pattern.stack());
            if (condition == null) {
                unassigned.add(pattern);
            } else {
                groups.computeIfAbsent(condition, key -> new ArrayList<>()).add(pattern);
            }
        }

        var conditions = new ArrayList<>(groups.keySet());
        conditions.sort(Comparator
                .comparing((AEKey key) -> key.getDisplayName().getString().toLowerCase(Locale.ROOT))
                .thenComparing(key -> key.getType().getId().toString())
                .thenComparing(key -> key.getId().toString()));
        for (var condition : conditions) {
            var group = groups.get(condition);
            displayRows.add(new GroupHeaderRow(condition));
            appendPatternRows(group);
        }
        if (!unassigned.isEmpty()) {
            displayRows.add(new GroupHeaderRow(null));
            appendPatternRows(unassigned);
        }
    }

    private void appendPatternRows(List<PatternRecord> source) {
        for (int offset = 0; offset < source.size(); offset += COLUMNS) {
            displayRows.add(new PatternRow(List.copyOf(
                    source.subList(offset, Math.min(offset + COLUMNS, source.size())))));
        }
    }

    @Nullable
    private DisplayRow getVisibleRow(int visibleRow) {
        int index = scrollbar.getCurrentScroll() + visibleRow;
        return index >= 0 && index < displayRows.size() ? displayRows.get(index) : null;
    }

    private void updateScrollbar() {
        scrollbar.setHeight(this.rows * style.getRow().getSrcHeight() - 2);
        scrollbar.setRange(0, Math.max(0, displayRows.size() - this.rows), Math.max(1, this.rows / 6));
    }

    private void updateSlots() {
        menu.slots.removeIf(slot -> slot instanceof PatternUpgradeSlot);
        if (fullUpdateInProgress) {
            return;
        }
        for (int r = 0; r < rows; r++) {
            var row = getVisibleRow(r);
            if (row instanceof GroupHeaderRow) {
                continue;
            }
            List<PatternRecord> rowPatterns = row instanceof PatternRow patternRow ? patternRow.patterns() : List.of();
            for (int col = 0; col < COLUMNS; col++) {
                var pos = style.getSlotPos(r, col);
                if (col >= rowPatterns.size()) {
                    menu.slots.add(new PatternUpgradeSlot(VIRTUAL_INV, VIRTUAL_ID, col, pos.getX(), pos.getY()));
                    continue;
                }
                var pattern = rowPatterns.get(col);
                var record = byId.get(pattern.serverId());
                if (record != null) {
                    menu.slots.add(new PatternUpgradeSlot(
                            record.getInventory(),
                            pattern.serverId(),
                            pattern.machineSlot(),
                            pos.getX(),
                            pos.getY()));
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // display the dialog box
        if (menu.dialogRevision != lastDialogRevision) {
            lastDialogRevision = menu.dialogRevision;
            switchToScreen(new DialogBox<>(this, menu.dialogTitle, menu.dialogMessage, menu.dialogLoopDiagram));
            return;
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        if (displayMode != menu.getLayout()) {
            displayMode = menu.getLayout();
            updateDisplayModeButton();
            scrollbar.setCurrentScroll(0);
            patternViewDirty = true;
        }
        if (patternViewDirty && !fullUpdateInProgress) {
            rebuildPatternView();
        }
        // Build slots before vanilla rendering and hover detection.
        updateSlots();

        repositionSlots(INPUT_PATTERN);
        for (int i = 0; i < menu.getInputPatternSlots().length; i++) {
            var slot = menu.getInputPatternSlots()[i];
            var effectiveRow = (i / INPUT_PATTERN_COLUMNS) - inputPatternScrollbar.getCurrentScroll();
            slot.setActive(effectiveRow >= 0 && effectiveRow < INPUT_PATTERN_VISIBLE_ROWS);
            slot.y -= inputPatternScrollbar.getCurrentScroll() * 18;
        }

        for (var mode : TagMode.values()) {
            var selected = menu.getMode() == mode;
            modeTabButtons.get(mode).setSelected(selected);
            modePanels.get(mode).setVisible(selected);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double wheelDelta) {
        var relativeMouse = new Point(
                (int) Math.round(mouseX - leftPos),
                (int) Math.round(mouseY - topPos));
        var inputAreaTop = imageHeight - 166;
        if (relativeMouse.getX() >= 9 && relativeMouse.getX() < 88
                && relativeMouse.getY() >= inputAreaTop && relativeMouse.getY() < inputAreaTop + 68
                && inputPatternScrollbar.onMouseWheel(relativeMouse, wheelDelta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, wheelDelta);
    }

    public void postFullUpdate(long serverId, int inventorySize, Int2ObjectMap<ItemStack> slots) {
        var record = new PatternContainerRecord(serverId, inventorySize);
        this.byId.put(serverId, record);
        slots.forEach((key, value) -> record.getInventory().setItemDirect(key, value));
    }

    public void postIncrementalUpdate(long serverId, Int2ObjectMap<ItemStack> slots) {
        var record = byId.get(serverId);
        if (record != null) {
            slots.forEach((key, value) -> {
                record.getInventory().setItemDirect(key, value.isEmpty() ? ItemStack.EMPTY : value);
            });
            if (!fullUpdateInProgress) {
                rebuildPatternView();
            }
        }
    }

    @Override
    protected void slotClicked(Slot slot, int slotIdx, int mouseButton, ClickType clickType) {
        if (slot instanceof PatternUpgradeSlot ps) {
            InventoryAction action = null;
            switch (clickType) {
                case PICKUP: // pickup / set-down.
                    action = mouseButton == 1 ? InventoryAction.SPLIT_OR_PLACE_SINGLE
                            : InventoryAction.PICKUP_OR_SET_DOWN;
                    break;
                case QUICK_MOVE:
                    action = mouseButton == 1 ? InventoryAction.PICKUP_SINGLE : InventoryAction.SHIFT_CLICK;
                    break;
                case CLONE: // creative dupe:
                    if (getPlayer().getAbilities().instabuild) {
                        action = InventoryAction.CREATIVE_DUPLICATE;
                    }
                    break;
            }
            if (action != null) {
                final InventoryActionPacket p = new InventoryActionPacket(action, ps.getMachineSlot(), ps.getServerId());
                NetworkHandler.instance().sendToServer(p);
            }
            return;
        }
        super.slotClicked(slot, slotIdx, mouseButton, clickType);
    }

    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);
        int textColor = getStyle().getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB();
        for (int r = 0; r < rows; r++) {
            if (getVisibleRow(r) instanceof GroupHeaderRow header) {
                int y = style.getHeader().getSrcHeight() + r * style.getRow().getSrcHeight() + 6;
                var pose = guiGraphics.pose();
                pose.pushPose();
                pose.translate(10, y, 0);
                pose.scale(0.5f, 0.5f, 1.0f);
                if (header.condition() != null) {
                    AEKeyRendering.drawInGui(Minecraft.getInstance(), guiGraphics, 0, 0, header.condition());
                } else {
                    guiGraphics.renderItem(new ItemStack(AEBlocks.PATTERN_PROVIDER.asItem()), 0, 0);
                }
                pose.popPose();
                var text = font.substrByWidth(header.title(), 145);
                guiGraphics.drawString(font, Language.getInstance().getVisualOrder(text),
                        20, y, textColor, false);
            }
        }
    }

    @Override
    public void drawBG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        int y = offsetY;
        style.getHeader().dest(offsetX, y).blit(guiGraphics);
        y += style.getHeader().getSrcHeight();
        int rowsToDraw = Math.max(2, this.rows);
        for (int r = 0; r < rowsToDraw; r++) {
            Blitter row = style.getRow();
            if (r == 0) row = style.getFirstRow();
            else if (r + 1 == rowsToDraw) row = style.getLastRow();
            row.dest(offsetX, y).blit(guiGraphics);
            if (getVisibleRow(r) instanceof GroupHeaderRow) {
                // Cover only the inner slot area, preserving the original frame and scrollbar.
                int left = offsetX + style.getSlotPos(r, 0).getX();
                int right = offsetX + style.getSlotPos(r, COLUMNS - 1).getX() + 16;
                int top = y + (r == 0 ? 1 : 0);
                int bottom = y + style.getRow().getSrcHeight() - (r + 1 == rowsToDraw ? 1 : 0);
                guiGraphics.fill(left, top, right, bottom, GROUP_HEADER_BACKGROUND);
                if (r == 0) {
                    guiGraphics.fill(left, top, right, top + 2, GROUP_HEADER_TOP_SHADOW);
                }
            }
            y += style.getRow().getSrcHeight();
        }
        style.getBottom().dest(offsetX, y).blit(guiGraphics);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = mouseX - leftPos;
        int y = mouseY - topPos - style.getHeader().getSrcHeight();
        int slotLeft = style.getSlotPos(0, 0).getX() - 1;
        int rowHeight = style.getRow().getSrcHeight();
        if (x >= slotLeft && x < slotLeft + COLUMNS * 18 && y >= 0 && y < rows * rowHeight
                && getVisibleRow(y / rowHeight) instanceof GroupHeaderRow header) {
            guiGraphics.renderTooltip(font, header.title(), mouseX, mouseY);
            return;
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private sealed interface DisplayRow permits GroupHeaderRow, PatternRow {
    }

    private record GroupHeaderRow(@Nullable AEKey condition) implements DisplayRow {
        Component title() {
            return condition == null
                    ? Component.translatable("gui.nimble_pattern.pattern_tag_terminal.no_condition")
                    : condition.getDisplayName();
        }
    }

    private record PatternRow(List<PatternRecord> patterns) implements DisplayRow {
    }
}
