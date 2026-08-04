package com.nightbeam.tbos.block;

import com.nightbeam.tbos.site.TemporalSiteManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class MemoryAnchorBlock extends Block {

    public MemoryAnchorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }


    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return player.isShiftKeyDown() ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (player.isShiftKeyDown()
                    && (TemporalSiteManager.resetAlignmentPuzzle(serverPlayer, pos)
                            || TemporalSiteManager.resetChoirPuzzle(serverPlayer, pos)
                            || TemporalSiteManager.resetBrokenMeridianPuzzle(serverPlayer, pos))) {
                return InteractionResult.SUCCESS;
            }
            if (!player.isShiftKeyDown() && TemporalSiteManager.activateCuratorAnchor(serverPlayer, pos)) {
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
}
