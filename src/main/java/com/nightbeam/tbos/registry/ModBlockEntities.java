package com.nightbeam.tbos.registry;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.blockentity.MemoryLanternBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Yesterglass.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MemoryLanternBlockEntity>> MEMORY_LANTERN =
            BLOCK_ENTITIES.register(
                    "memory_lantern",
                    () -> new BlockEntityType<>(
                            MemoryLanternBlockEntity::new,
                            ModBlocks.MEMORY_LANTERN.get()));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}
