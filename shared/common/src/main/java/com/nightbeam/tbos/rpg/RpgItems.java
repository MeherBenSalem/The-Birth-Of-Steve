package com.nightbeam.tbos.rpg;

import com.nightbeam.tbos.item.MemoryKeyItem;
import com.nightbeam.tbos.platform.registry.ModRegistries;
import com.nightbeam.tbos.platform.registry.RegistryEntry;

/** Items added in 1.0. Kept apart from {@code ModItems}, which several targets override. */
public final class RpgItems {
    public static final RegistryEntry<MemoryKeyItem> MEMORY_KEY = ModRegistries.ITEMS.registerItem(
            "memory_key", MemoryKeyItem::new, properties -> properties.stacksTo(1));

    private RpgItems() {
    }

    /** Touching this class queues the items; the loader flushes the registrar. */
    public static void register() {
    }
}
