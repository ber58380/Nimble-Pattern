package com.ber.nimblePattern.crafting;

import appeng.api.config.Actionable;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.execution.CraftingCpuHelper;
import appeng.crafting.inv.ListCraftingInventory;
import com.ber.nimblePattern.pattern.NimbleAssemblerPattern;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Keeps real tools CPU-local between assembler operations; never fabricates recipe results or remainders. */
public final class ToolCraftingController {
    private final ListCraftingInventory reserved = inventory();
    private final ListCraftingInventory expected = inventory();
    private final ListCraftingInventory received = inventory();
    private final ListCraftingInventory borrowed = inventory();
    private boolean active;

    private static ListCraftingInventory inventory() { return new ListCraftingInventory(key -> {}); }

    public boolean isActive() { return active; }

    public void reserve(KeyCounter tools, ListCraftingInventory cpu) {
        for (var tool : tools) {
            long extracted = cpu.extract(tool.getKey(), Math.max(0, tool.getLongValue() - held(tool.getKey())), Actionable.MODULATE);
            reserved.insert(tool.getKey(), extracted, Actionable.MODULATE);
        }
    }

    public long held(AEKey key) { return reserved.list.get(key) + borrowed.list.get(key); }

    public boolean push(NimbleAssemblerPattern pattern, ICraftingProvider provider,
            KeyCounter[] consumables, Level level) {
        if (active) return false;
        var available = inventory();
        for (var holder : consumables) available.list.addAll(holder);
        for (var tool : pattern.getTools()) {
            if (reserved.extract(tool.what(), tool.amount(), Actionable.SIMULATE) != tool.amount()) return false;
            available.insert(tool.what(), tool.amount(), Actionable.MODULATE);
        }
        var outputs = new KeyCounter();
        var remainders = new KeyCounter();
        var physical = CraftingCpuHelper.extractPatternInputs(pattern.getPattern(), available, level, outputs, remainders);
        if (physical == null || !available.list.isEmpty()) return false;
        for (var tool : pattern.getTools()) {
            reserved.extract(tool.what(), tool.amount(), Actionable.MODULATE);
            borrowed.insert(tool.what(), tool.amount(), Actionable.MODULATE);
        }
        expected.list.addAll(outputs);
        expected.list.addAll(remainders);
        active = true;
        // Register receipts first: a compatible provider may finish synchronously inside pushPattern.
        // Dispatch the native crafting pattern and full grid: providers may check its concrete type.
        if (!provider.pushPattern(com.ber.nimblePattern.pattern.ProviderPatternIndex.nativePattern(provider, pattern), physical)) {
            reserved.list.addAll(borrowed.list);
            borrowed.list.clear();
            expected.list.clear();
            active = false;
            return false;
        }
        for (var holder : consumables) holder.clear();
        return true;
    }

    public long accept(AEKey key, long amount, Actionable mode) {
        if (!active) return 0;
        long accepted = expected.extract(key, amount, mode);
        if (accepted > 0 && mode == Actionable.MODULATE) received.insert(key, accepted, mode);
        return accepted;
    }

    /** Wait for remainders as well as outputs before delivering a final result that may finish the job. */
    public List<GenericStack> finishStep() {
        expected.list.removeZeros();
        if (!active || !expected.list.isEmpty()) return List.of();
        for (var tool : borrowed.list) {
            long returned = received.extract(tool.getKey(), tool.getLongValue(), Actionable.MODULATE);
            reserved.insert(tool.getKey(), returned, Actionable.MODULATE);
        }
        var results = new ArrayList<GenericStack>();
        for (var result : received.list) {
            if (result.getLongValue() > 0) results.add(new GenericStack(result.getKey(), result.getLongValue()));
        }
        borrowed.list.clear();
        received.list.clear();
        active = false;
        return results;
    }

    public long waiting(AEKey key) { return expected.list.get(key); }
    public void addWaiting(Set<AEKey> keys) {
        for (var entry : expected.list) if (entry.getLongValue() > 0) keys.add(entry.getKey());
    }

    public void release(ListCraftingInventory cpu) {
        cpu.list.addAll(reserved.list);
        cpu.list.addAll(received.list);
        reserved.list.clear();
        received.list.clear();
        borrowed.list.clear();
        expected.list.clear();
        active = false;
        // Items still in an assembler remain physical items and return normally after cancellation.
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putBoolean("active", active);
        tag.put("reserved", reserved.writeToNBT());
        tag.put("expected", expected.writeToNBT());
        tag.put("received", received.writeToNBT());
        tag.put("borrowed", borrowed.writeToNBT());
        return tag;
    }

    public void load(CompoundTag tag) {
        active = tag.getBoolean("active");
        reserved.readFromNBT(tag.getList("reserved", Tag.TAG_COMPOUND));
        expected.readFromNBT(tag.getList("expected", Tag.TAG_COMPOUND));
        received.readFromNBT(tag.getList("received", Tag.TAG_COMPOUND));
        borrowed.readFromNBT(tag.getList("borrowed", Tag.TAG_COMPOUND));
    }
}
