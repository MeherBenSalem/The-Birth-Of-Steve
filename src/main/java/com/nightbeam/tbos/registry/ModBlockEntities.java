package com.nightbeam.tbos.registry;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.blockentity.AlignmentDialBlockEntity;
import com.nightbeam.tbos.blockentity.ArchiveCoreBlockEntity;
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

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArchiveCoreBlockEntity>> ARCHIVE_CORE =
            BLOCK_ENTITIES.register(
                    "archive_core",
                    () -> new BlockEntityType<>(
                            ArchiveCoreBlockEntity::new,
                            ModBlocks.ARCHIVE_CORE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlignmentDialBlockEntity>> ALIGNMENT_DIAL =
            BLOCK_ENTITIES.register(
                    "alignment_dial",
                    () -> new BlockEntityType<>(
                            AlignmentDialBlockEntity::new,
                            ModBlocks.ALIGNMENT_DIAL.get()));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}
