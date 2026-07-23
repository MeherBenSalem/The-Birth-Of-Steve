package com.nightbeam.tbos.block;

import com.nightbeam.tbos.site.TemporalSiteManager;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class ArchiveCoreBlock extends Block {
    public static final MapCodec<ArchiveCoreBlock> CODEC = simpleCodec(ArchiveCoreBlock::new);

    public ArchiveCoreBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends ArchiveCoreBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer
                && TemporalSiteManager.startCuratorEncounter(serverPlayer, pos)) {
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }
}
