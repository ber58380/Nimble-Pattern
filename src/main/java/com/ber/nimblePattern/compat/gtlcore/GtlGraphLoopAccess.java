package com.ber.nimblePattern.compat.gtlcore;

import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.ber.nimblePattern.crafting.LoopCpuAccess;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflective bridge for the optional GTL graph classes. Both GTL's graph controller and its execution
 * adapter keep the CPU host in a private {@code host} field. Avoiding a hard dependency on the GTL jar
 * keeps this mod usable when GTL is not installed or when its graph API changes.
 */
public final class GtlGraphLoopAccess {
    private static final Map<Class<?>, Field> HOST_FIELDS = new ConcurrentHashMap<>();

    private GtlGraphLoopAccess() {
    }

    public static LoopCpuAccess find(Object owner) {
        if (owner == null) {
            return null;
        }
        try {
            Object host = hostField(owner.getClass()).get(owner);
            if (host == null) {
                return null;
            }
            Object cpu = host.getClass().getMethod("cpu").invoke(host);
            if (cpu instanceof CraftingCPUCluster cluster && cluster.craftingLogic instanceof LoopCpuAccess access) {
                return access;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // GTL is optional and its internals may change. In that case graph jobs simply keep their
            // normal behaviour instead of failing the whole CPU tick.
        }
        return null;
    }

    private static Field hostField(Class<?> type) throws NoSuchFieldException {
        Field field = HOST_FIELDS.get(type);
        if (field == null) {
            field = type.getDeclaredField("host");
            field.setAccessible(true);
            HOST_FIELDS.put(type, field);
        }
        return field;
    }
}
