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

import static com.ber.nimblePattern.pattern.UpdateState.*;

public class NimblePatternTag {
    private static final String ROOT = NimblePattern.MOD_ID;
    // the source tag includes: dim, pos, side, slot
    private static final String SOURCE_TAG = "source";
    // the update tag includes: condition, status
    private static final String UPDATE_TAG = "update";

    public static void tagSource(ItemStack pattern, ServerLevel level, BlockPos pos, Direction side, int slot) {
        var root = pattern.getOrCreateTagElement(ROOT);
        var tag = new CompoundTag();
        tag.putString("dim", level.dimension().location().toString());
        tag.putLong("pos", pos.asLong());
        tag.putByte("side", (byte) (side == null ? 6 : side.ordinal()));
        tag.putInt("slot", slot);
        root.put(SOURCE_TAG, tag);
    }

    public static void removeTag(ItemStack pattern) {
        CompoundTag root = pattern.getTagElement(ROOT);
        if (root == null) {
            return;
        }
        root.remove(SOURCE_TAG);
        if (root.contains(UPDATE_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag updateTag = root.getCompound(UPDATE_TAG);
            int status = -1;
            if (updateTag.contains("status", Tag.TAG_BYTE)) {
                status = updateTag.getByte("status");
            }
            if (status == UNTRACKED.ordinal()) {
                root.remove(UPDATE_TAG);
            }
        }
        if (root.isEmpty()) {
            pattern.removeTagKey(ROOT);
        }
    }

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

    public static String getCondition(ItemStack pattern) {
        CompoundTag root = pattern.getTagElement(ROOT);
        if (root == null) {
            return "";
        }
        if (root.contains(UPDATE_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag updateTag = root.getCompound(UPDATE_TAG);
            return updateTag.getString("condition");
        }
        return "";
    }

    public static void tagUpdate(ItemStack pattern, String condition) {
        CompoundTag root = pattern.getOrCreateTagElement(ROOT);
        CompoundTag updateTag = new CompoundTag();
        updateTag.putString("condition", condition);
        updateTag.putByte("status", (byte) LATEST.ordinal());
        root.put(UPDATE_TAG, updateTag);
    }

    public static void tagStatus(ItemStack pattern) {
        CompoundTag root = pattern.getTagElement(ROOT);
        if (root == null) {
            return;
        }
        if (root.contains(UPDATE_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag updateTag = root.getCompound(UPDATE_TAG);
            updateTag.putByte("status", (byte) UPDATE.ordinal());
            root.put(UPDATE_TAG, updateTag);
        }
    }

    public static UpdateState getStatus(ItemStack pattern) {
        CompoundTag root = pattern.getTagElement(ROOT);
        if (root == null) {
            return UpdateState.UNTRACKED;
        }
        if (root.contains(UPDATE_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag updateTag = root.getCompound(UPDATE_TAG);
            byte status = updateTag.getByte("status");
            var values = UpdateState.values();
            return status >= 0 && status < values.length ? values[status] : UpdateState.UNTRACKED;
        }
        return UpdateState.UNTRACKED;
    }

    public static void removeConditionTag(ItemStack pattern) {
        CompoundTag root = pattern.getTagElement(ROOT);
        if (root == null) {
            return;
        }
        root.remove(UPDATE_TAG);
        if (root.isEmpty()) {
            pattern.removeTagKey(ROOT);
        }
    }

}
