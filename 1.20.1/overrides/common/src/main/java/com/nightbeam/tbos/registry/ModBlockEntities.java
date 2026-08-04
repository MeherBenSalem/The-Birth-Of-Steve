package com.nightbeam.tbos.registry;

import com.nightbeam.tbos.blockentity.AlignmentDialBlockEntity;
import com.nightbeam.tbos.blockentity.ArchiveCoreBlockEntity;
import com.nightbeam.tbos.blockentity.MemoryLanternBlockEntity;
import com.nightbeam.tbos.platform.registry.ModRegistries;
import com.nightbeam.tbos.platform.registry.RegistryEntry;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Minecraft 1.20.1 exposes block entity types through {@code BlockEntityType.Builder},
 * whose public {@code of} accepts a {@code BlockEntitySupplier} without requiring
 * mods to name the package-private supplier interface.
 */
public final class ModBlockEntities {
    public static final RegistryEntry<BlockEntityType<MemoryLanternBlockEntity>> MEMORY_LANTERN =
            ModRegistries.BLOCK_ENTITIES.register(
                    "memory_lantern",
                    () -> BlockEntityType.Builder.of(
                                    MemoryLanternBlockEntity::new, ModBlocks.MEMORY_LANTERN.get())
                            .build(null));

    public static final RegistryEntry<BlockEntityType<ArchiveCoreBlockEntity>> ARCHIVE_CORE =
            ModRegistries.BLOCK_ENTITIES.register(
                    "archive_core",
                    () -> BlockEntityType.Builder.of(
                                    ArchiveCoreBlockEntity::new, ModBlocks.ARCHIVE_CORE.get())
                            .build(null));

    public static final RegistryEntry<BlockEntityType<AlignmentDialBlockEntity>> ALIGNMENT_DIAL =
            ModRegistries.BLOCK_ENTITIES.register(
                    "alignment_dial",
                    () -> BlockEntityType.Builder.of(
                                    AlignmentDialBlockEntity::new, ModBlocks.ALIGNMENT_DIAL.get())
                            .build(null));

    private ModBlockEntities() {
    }

    /** Touching this class is what queues every block entity; the loader flushes later. */
    public static void register() {
    }
}
