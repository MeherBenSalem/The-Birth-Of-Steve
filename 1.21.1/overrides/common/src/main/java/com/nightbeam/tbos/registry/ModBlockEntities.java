package com.nightbeam.tbos.registry;

import com.nightbeam.tbos.blockentity.AlignmentDialBlockEntity;
import com.nightbeam.tbos.blockentity.ArchiveCoreBlockEntity;
import com.nightbeam.tbos.blockentity.MemoryLanternBlockEntity;
import com.nightbeam.tbos.platform.registry.ModRegistries;
import com.nightbeam.tbos.platform.registry.RegistryEntry;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Minecraft 1.21.1 added the data-fixer type as BlockEntityType's third
 * constructor parameter. Mod block entities have no vanilla data-fixer schema,
 * so the null value is equivalent to the shared two-argument construction.
 */
public final class ModBlockEntities {
    public static final RegistryEntry<BlockEntityType<MemoryLanternBlockEntity>> MEMORY_LANTERN =
            ModRegistries.BLOCK_ENTITIES.register(
                    "memory_lantern",
                    () -> new BlockEntityType<>(
                            MemoryLanternBlockEntity::new,
                            java.util.Set.of(ModBlocks.MEMORY_LANTERN.get()),
                            null));

    public static final RegistryEntry<BlockEntityType<ArchiveCoreBlockEntity>> ARCHIVE_CORE =
            ModRegistries.BLOCK_ENTITIES.register(
                    "archive_core",
                    () -> new BlockEntityType<>(
                            ArchiveCoreBlockEntity::new,
                            java.util.Set.of(ModBlocks.ARCHIVE_CORE.get()),
                            null));

    public static final RegistryEntry<BlockEntityType<AlignmentDialBlockEntity>> ALIGNMENT_DIAL =
            ModRegistries.BLOCK_ENTITIES.register(
                    "alignment_dial",
                    () -> new BlockEntityType<>(
                            AlignmentDialBlockEntity::new,
                            java.util.Set.of(ModBlocks.ALIGNMENT_DIAL.get()),
                            null));

    private ModBlockEntities() {
    }

    /** Touching this class is what queues every block entity; the loader flushes later. */
    public static void register() {
    }
}
