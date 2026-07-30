package com.nightbeam.tbos.registry;

import com.nightbeam.tbos.platform.registry.ModRegistries;
import com.nightbeam.tbos.platform.registry.RegistryEntry;
import com.nightbeam.tbos.item.MemoryScene;
import net.minecraft.core.component.DataComponentType;

public final class ModDataComponents {
    public static final RegistryEntry<DataComponentType<MemoryScene>> MEMORY_SCENE =
            ModRegistries.DATA_COMPONENTS.register(
                    "memory_scene",
                    () -> DataComponentType.<MemoryScene>builder()
                            .persistent(MemoryScene.CODEC)
                            .networkSynchronized(MemoryScene.STREAM_CODEC)
                            .cacheEncoding()
                            .build());

    private ModDataComponents() {
    }

    /** Touching this class is what queues the component type; the loader flushes later. */
    public static void register() {
    }
}
