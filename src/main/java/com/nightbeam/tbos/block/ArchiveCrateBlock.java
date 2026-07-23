package com.nightbeam.tbos.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Breakable decorative Archive prop that is handled by run protection. */
public final class ArchiveCrateBlock extends Block {
    public static final MapCodec<ArchiveCrateBlock> CODEC = simpleCodec(ArchiveCrateBlock::new);

    public ArchiveCrateBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends ArchiveCrateBlock> codec() {
        return CODEC;
    }
}
