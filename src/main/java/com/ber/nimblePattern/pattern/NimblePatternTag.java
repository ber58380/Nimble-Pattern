package com.ber.nimblePattern.pattern;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.InternalInventory;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.helpers.patternprovider.PatternContainer;
import com.ber.nimblePattern.NimblePattern;
import com.ber.nimblePattern.compat.extendedae.ExtendedAECompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

import static com.ber.nimblePattern.pattern.UpgradeState.*;

public class NimblePatternTag {
    private static final String ROOT = NimblePattern.MOD_ID;
    // the source tag includes: dim, pos, side, slot
    private static final String SOURCE_TAG = "source";
    // the upgrade tag includes: condition, status
    private static final String UPGRADE_TAG = "upgrade";
    // the fake tag includes: isFake
    private static final String FAKE_TAG = "fake";
    // the fuzzy tag includes: isFuzzy
    private static final String FUZZY_TAG = "fuzzy";

    public static void removeTag(ItemStack pattern) {
        CompoundTag root = pattern.getTagElement(ROOT);
        if (root == null) {
            return;
        }
        root.remove(SOURCE_TAG);
        if (root.contains(UPGRADE_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag upgradeTag = root.getCompound(UPGRADE_TAG);
            int status = -1;
            if (upgradeTag.contains("status", Tag.TAG_BYTE)) {
                status = upgradeTag.getByte("status");
            }
            if (status == UNTRACKED.ordinal()) {
                root.remove(UPGRADE_TAG);
            }
        }
        if (root.isEmpty()) {
            pattern.removeTagKey(ROOT);
        }
    }

    private static InternalInventory getTerminalPatternInventory(BlockEntity block, Direction side) {
        if (block == null) {
            return null;
        }
        if (side == null) { // block entity of pattern provider
            if (block instanceof PatternContainer container) {
                return container.getTerminalPatternInventory();
            }
        } else {
            if (block instanceof CableBusBlockEntity cbb) {
                var part = cbb.getPart(side);
                // parts of pattern provider
                if (part instanceof PatternContainer container) {
                    return container.getTerminalPatternInventory();
                }
                // parts of extendedAE's pattern provider
                if (ExtendedAECompat.LOADED) {
                    return ExtendedAECompat.getTerminalPatternInventory(part);
                }
            }
        }
        // GT series
        try {
            String name = block.getClass().getName();
            if (name.contains("MachineBlockEntity") || name.contains("Machine") || name.contains("IMachine")) {
                var method = block.getClass().getMethod("getMetaMachine");
                var machine = method.invoke(block);
                if (machine == null) return null;
                // pattern buffer
                if (machine instanceof PatternContainer pc) {
                    return pc.getTerminalPatternInventory();
                }
                // molecular assembler matrix
                for (var methodName : List.of("getPatternInventory", "getExposedInventory", "getHandler", "getInventory", "getInternalInventory")) {
                    try {
                        var m = machine.getClass().getMethod(methodName);
                        var inv = m.invoke(machine);
                        if (inv instanceof InternalInventory ii) {
                            return ii;
                        }
                        if (inv != null && inv.getClass().getName().contains("ItemStackTransfer")) {
                            InternalInventory adapted = adaptItemStackTransfer(inv);
                            if (adapted != null) return adapted;
                        }
                    } catch (NoSuchMethodException ignore) {
                    }
                }
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    // transform the ItemStackTransfer of LDlib to InternalInventory of ae2
    private static InternalInventory adaptItemStackTransfer(Object transfer) {
        try {
            var getSlots = transfer.getClass().getMethod("getSlots");
            var getStack = transfer.getClass().getMethod("getStackInSlot", int.class);
            var setStack = transfer.getClass().getMethod("setStackInSlot", int.class, ItemStack.class);
            return new InternalInventory() {
                @Override
                public int size() {
                    try {
                        return (int) getSlots.invoke(transfer);
                    } catch (Exception e) {
                        return 0;
                    }
                }

                @Override
                public ItemStack getStackInSlot(int slotIndex) {
                    try {
                        return (ItemStack) getStack.invoke(transfer, slotIndex);
                    } catch (Exception e) {
                        return ItemStack.EMPTY;
                    }
                }

                @Override
                public void setItemDirect(int slotIndex, ItemStack stack) {
                    try {
                        setStack.invoke(transfer, slotIndex, stack);
                    } catch (Exception ignored) {
                    }
                }
            };
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean pushPatternBack(ItemStack pattern, MinecraftServer server) {
        if (pattern.isEmpty() || !PatternDetailsHelper.isEncodedPattern(pattern)) {
            return false;
        }
        NimblePatternSource source = getSource(pattern, server);
        if (source == null) {
            return false;
        }
        InternalInventory container = source.container();
        if (container == null || source.slot() >= container.size() || !container.getStackInSlot(source.slot()).isEmpty()) {
            return false;
        }
        removeTag(pattern);
        container.setItemDirect(source.slot(), pattern);
        return true;
    }

    // region tag functions
    private static void tagPattern(ItemStack pattern, String key, CompoundTag value) {
        CompoundTag root = pattern.getOrCreateTagElement(ROOT);
        root.put(key, value);
    }

    public static void tagSource(ItemStack pattern, ServerLevel level, BlockPos pos, Direction side, int slot) {
        var sourceTag = new CompoundTag();
        sourceTag.putString("dim", level.dimension().location().toString());
        sourceTag.putLong("pos", pos.asLong());
        sourceTag.putByte("side", (byte) (side == null ? 6 : side.ordinal()));
        sourceTag.putInt("slot", slot);
        tagPattern(pattern, SOURCE_TAG, sourceTag);
    }

    public static void tagUpgrade(ItemStack pattern, String condition) {
        CompoundTag upgradeTag = new CompoundTag();
        upgradeTag.putString("condition", condition);
        upgradeTag.putByte("status", (byte) LATEST.ordinal());
        tagPattern(pattern, UPGRADE_TAG, upgradeTag);
    }

    public static void tagStatus(ItemStack pattern) {
        CompoundTag root = pattern.getTagElement(ROOT);
        if (root == null) {
            return;
        }
        if (root.contains(UPGRADE_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag upgradeTag = root.getCompound(UPGRADE_TAG);
            upgradeTag.putByte("status", (byte) UPGRADE.ordinal());
        }
    }

    public static void tagFake(ItemStack pattern) {
        CompoundTag fakeTag = new CompoundTag();
        fakeTag.putBoolean("isFake", true);
        tagPattern(pattern, FAKE_TAG, fakeTag);
    }

    public static void tagFuzzy(ItemStack pattern) {
        CompoundTag fuzzyTag = new CompoundTag();
        fuzzyTag.putBoolean("isFuzzy", true);
        tagPattern(pattern, FUZZY_TAG, fuzzyTag);
    }
    // endregion

    // region get functions
    private static NimblePatternSource getSource(ItemStack pattern, MinecraftServer server) {
        CompoundTag root = pattern.getTagElement(ROOT);
        if (root == null) {
            return null;
        }
        if (root.contains(SOURCE_TAG, Tag.TAG_COMPOUND)) {
            try {
                CompoundTag sourceTag = root.getCompound(SOURCE_TAG);
                ResourceLocation dim = ResourceLocation.tryParse(sourceTag.getString("dim"));
                if (dim == null) {
                    return null;
                }
                BlockPos pos = BlockPos.of(sourceTag.getLong("pos"));
                int sideNum = sourceTag.getByte("side");
                Direction side = sideNum == 6 ? null : Direction.from3DDataValue(sideNum);
                int slot = sourceTag.getInt("slot");

                // find the source pattern provider based on source info
                var dimKey = ResourceKey.create(Registries.DIMENSION, dim);
                ServerLevel level = server.getLevel(dimKey);
                if (level == null || !level.hasChunkAt(pos)) {
                    return null;
                }
                InternalInventory inv = getTerminalPatternInventory(level.getBlockEntity(pos), side);
                if (inv == null) {
                    return null;
                }
                return new NimblePatternSource(level, pos, side, slot, inv);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public static String getCondition(ItemStack pattern) {
        CompoundTag root = pattern.getTagElement(ROOT);
        if (root == null) {
            return "";
        }
        if (root.contains(UPGRADE_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag upgradeTag = root.getCompound(UPGRADE_TAG);
            return upgradeTag.getString("condition");
        }
        return "";
    }

    public static UpgradeState getStatus(ItemStack pattern) {
        CompoundTag root = pattern.getTagElement(ROOT);
        if (root == null) {
            return UpgradeState.UNTRACKED;
        }
        if (root.contains(UPGRADE_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag upgradeTag = root.getCompound(UPGRADE_TAG);
            byte status = upgradeTag.getByte("status");
            var values = UpgradeState.values();
            return status >= 0 && status < values.length ? values[status] : UpgradeState.UNTRACKED;
        }
        return UpgradeState.UNTRACKED;
    }

    public static boolean getFake(ItemStack pattern) {
        CompoundTag root = pattern.getTagElement(ROOT);
        if (root == null) {
            return false;
        }
        if (root.contains(FAKE_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag fakeTag = root.getCompound(FAKE_TAG);
            return fakeTag.getBoolean("isFake");
        }
        return false;
    }

    public static boolean getFuzzy(ItemStack pattern) {
        CompoundTag root = pattern.getTagElement(ROOT);
        if (root == null) {
            return false;
        }
        if (root.contains(FUZZY_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag fuzzyTag = root.getCompound(FUZZY_TAG);
            return fuzzyTag.getBoolean("isFuzzy");
        }
        return false;
    }
    // endregion

    /**
     * Filter useless tags for crafting, like source and upgrade.
     */
    public static ItemStack filterCraftingTags(ItemStack pattern) {
        CompoundTag root = pattern.getTagElement(ROOT);
        if (root != null) {
            root.remove(SOURCE_TAG);
            root.remove(UPGRADE_TAG);
            if (root.isEmpty()) {
                pattern.removeTagKey(ROOT);
            }
        }
        return pattern;
    }

    public static void removeConditionTag(ItemStack pattern) {
        CompoundTag root = pattern.getTagElement(ROOT);
        if (root == null) {
            return;
        }
        root.remove(UPGRADE_TAG);
        if (root.isEmpty()) {
            pattern.removeTagKey(ROOT);
        }
    }


}
