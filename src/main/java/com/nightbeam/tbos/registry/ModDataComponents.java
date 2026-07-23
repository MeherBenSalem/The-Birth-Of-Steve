package com.nightbeam.tbos.registry;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.item.MemoryScene;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Yesterglass.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MemoryScene>> MEMORY_SCENE =
            COMPONENTS.registerComponentType(
                    "memory_scene",
                    builder -> builder
                            .persistent(MemoryScene.CODEC)
                            .networkSynchronized(MemoryScene.STREAM_CODEC)
                            .cacheEncoding());

    private ModDataComponents() {
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }
}
