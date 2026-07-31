package com.nightbeam.tbos.blockentity;

import com.nightbeam.tbos.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stateless anchor for the Alignment Dial's block-entity renderer. Dial puzzle
 * state stays packed in the site's {@code progressFlags}; this carries no data
 * and is never ticked.
 */
public final class AlignmentDialBlockEntity extends BlockEntity {
    public AlignmentDialBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALIGNMENT_DIAL.get(), pos, state);
    }
}
