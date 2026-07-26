package com.nightbeam.tbos.blockentity;

import com.nightbeam.tbos.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stateless anchor for the Archive Core's block-entity renderer. The Curator
 * encounter's state lives entirely in the site's {@code progressFlags}; this
 * carries no data and is never ticked.
 */
public final class ArchiveCoreBlockEntity extends BlockEntity {
    public ArchiveCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARCHIVE_CORE.get(), pos, state);
    }
}
