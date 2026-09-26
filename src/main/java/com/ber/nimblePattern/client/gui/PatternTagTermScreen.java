package com.ber.nimblePattern.client.gui;

import appeng.api.config.Settings;
import appeng.client.Point;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.TerminalStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.client.gui.widgets.TabButton;
import appeng.core.AEConfig;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.InventoryActionPacket;
import appeng.helpers.InventoryAction;
import com.ber.nimblePattern.client.gui.panel.PatternLoopPanel;
import com.ber.nimblePattern.client.gui.panel.TagModePanel;
import com.ber.nimblePattern.client.gui.search.PatternSearch;
import com.ber.nimblePattern.client.gui.panel.PatternUpgradePanel;
import com.ber.nimblePattern.client.gui.widgets.PatternUpgradeSlot;
import com.ber.nimblePattern.menu.PatternTagTermMenu;
import com.ber.nimblePattern.parts.TagMode;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.*;

import static com.ber.nimblePattern.menu.PatternTagTermMenu.VIRTUAL_ID;
import static com.ber.nimblePattern.menu.PatternTagTermMenu.VIRTUAL_INV;
import static com.ber.nimblePattern.menu.PatternTagTermMenu.INPUT_PATTERN;
import static com.ber.nimblePattern.parts.PatternTagLogic.INPUT_PATTERN_COLUMNS;
import static com.ber.nimblePattern.parts.PatternTagLogic.INPUT_PATTERN_TOTAL_ROWS;
import static com.ber.nimblePattern.parts.PatternTagLogic.INPUT_PATTERN_VISIBLE_ROWS;

public class PatternTagTermScreen<C extends PatternTagTermMenu> extends AEBaseScreen<C> {
    private static final int COLUMNS = 9;
    private static final int MIN_ROWS = 2;

    private int rows = 0;

    private final TerminalStyle style;
    private final Scrollbar scrollbar;
    private final Scrollbar inputPatternScrollbar;
    private final AETextField searchField;
    private final PatternSearch search = new PatternSearch();
    private final Map<TagMode, TagModePanel> modePanels = new EnumMap<>(TagMode.class);
    private final Map<TagMode, TabButton> modeTabButtons = new EnumMap<>(TagMode.class);

    private final Long2ObjectOpenHashMap<PatternContainerRecord> byId = new Long2ObjectOpenHashMap<>();
    private List<PatternRecord> patterns = new ArrayList<>();
    private Set<String> conditions = new LinkedHashSet<String>();

    // ClearPacket starts a full synchronization batch. Full PatternPackets only
    // populate byId; PatternSyncCompletePacket rebuilds the expensive global view once.
    private boolean fullUpdateInProgress = false;

    public PatternTagTermScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        this.style = style.getTerminalStyle();
        addToLeftToolbar(new SettingToggleButton<>(Settings.TERMINAL_STYLE, AEConfig.instance().getTerminalStyle(), this::toggleTerminalStyle));
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

        for (var mode: TagMode.values()) {
            var panel = switch (mode) {
                case UPGRADE -> new PatternUpgradePanel(this, widgets);
                case LOOP -> new PatternLoopPanel(this, widgets);
            };
            var tabButton = new TabButton(
                    panel.getTabIconItem(),
                    panel.getTabTooltip(),
                    btn -> getMenu().setMode(mode));
            tabButton.setStyle(TabButton.Style.HORIZONTAL);
            var modeIndex = modeTabButtons.size();
            widgets.add("modePanel" + modeIndex, panel);
            widgets.add("modeTabButton" + modeIndex, tabButton);
            modeTabButtons.put(mode, tabButton);
            modePanels.put(mode, panel);
        }
    }

    public void clear() {
        fullUpdateInProgress = true;
        byId.clear();
        patterns.clear();
        menu.slots.removeIf(slot -> slot instanceof PatternUpgradeSlot);
        updateScrollbar();
    }

    public void finishFullUpdate() {
        fullUpdateInProgress = false;
        rebuildPatternView();
    }

    private void rebuildPatternView() {
        updatePatterns();
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
        patterns.sort(Comparator.comparing(PatternRecord::sortKey));
    }

    private void updateScrollbar() {
        scrollbar.setHeight(this.rows * style.getRow().getSrcHeight() - 2);
        int totalRows = (patterns.size() + COLUMNS - 1) / COLUMNS;
        scrollbar.setRange(0, totalRows - this.rows, Math.max(1, this.rows / 6));
    }

    private void updateSlots() {
        menu.slots.removeIf(slot -> slot instanceof PatternUpgradeSlot);
        int first = scrollbar.getCurrentScroll();
        for (int r = 0; r < rows; r++) {
            int row = first + r;
            for (int col = 0; col < COLUMNS; col++) {
                int idx = row * COLUMNS + col;
                var pos = style.getSlotPos(r, col);
                // add dummy blank slots to show the full row
                if (idx >= patterns.size()) {
                    menu.slots.add(new PatternUpgradeSlot(VIRTUAL_INV, VIRTUAL_ID, col, pos.getX(), pos.getY()));
                    continue;
                }
                var pattern = patterns.get(idx);
                var record = byId.get(pattern.serverId());
                menu.slots.add(new PatternUpgradeSlot(
                        record.getInventory(),
                        pattern.serverId(),
                        pattern.machineSlot(),
                        pos.getX(),
                        pos.getY()));
            }
        }
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        repositionSlots(INPUT_PATTERN);
        for (int i = 0; i < menu.getInputPatternSlots().length; i++) {
            var slot = menu.getInputPatternSlots()[i];
            var effectiveRow = (i / INPUT_PATTERN_COLUMNS) - inputPatternScrollbar.getCurrentScroll();
            slot.setActive(effectiveRow >= 0 && effectiveRow < INPUT_PATTERN_VISIBLE_ROWS);
            slot.y -= inputPatternScrollbar.getCurrentScroll() * 18;
        }

        for (var mode: TagMode.values()) {
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

    public void postConditionUpdate(Set<String> conditions) {
        this.conditions = conditions;
        if (modePanels.get(TagMode.UPGRADE) instanceof PatternUpgradePanel upgradePanel) {
            upgradePanel.setHistory(conditions);
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
        updateSlots();
        super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);
    }

    @Override
    public void drawBG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        int y = offsetY;
        style.getHeader().dest(offsetX, y).blit(guiGraphics);
        y += style.getHeader().getSrcHeight();
        int rowsToDraw = Math.max(2, this.rows);
        for (int x = 0; x < rowsToDraw; x++) {
            Blitter row = style.getRow();
            if (x == 0) row = style.getFirstRow();
            else if (x + 1 == rowsToDraw) row = style.getLastRow();
            row.dest(offsetX, y).blit(guiGraphics);
            y += style.getRow().getSrcHeight();
        }
        style.getBottom().dest(offsetX, y).blit(guiGraphics);
    }
}
