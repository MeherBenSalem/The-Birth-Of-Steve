package com.nightbeam.tbos.block;

import com.nightbeam.tbos.blockentity.AlignmentDialBlockEntity;
import com.nightbeam.tbos.site.TemporalSiteManager;
import com.nightbeam.tbos.run.ArchiveEncounterManager;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import com.nightbeam.tbos.compat.VanillaCompat;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

public final class AlignmentDialBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<AlignmentDialBlock> CODEC = simpleCodec(AlignmentDialBlock::new);

    public AlignmentDialBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends AlignmentDialBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlignmentDialBlockEntity(pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(2) != 0) {
            return;
        }
        // Sparks trace the dial ring rather than scattering, so the block still
        // reads as a measuring instrument.
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double radius = 0.34D;
        level.addParticle(
                ParticleTypes.END_ROD,
                pos.getX() + 0.5D + Math.cos(angle) * radius,
                pos.getY() + 0.72D + Math.sin(angle) * radius,
                pos.getZ() + 0.53D,
                0.0D,
                0.005D,
                0.0D);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
                && (ArchiveEncounterManager.rotateHallDial(serverPlayer, pos)
                        || TemporalSiteManager.rotateAlignmentDial(serverPlayer, pos))) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult) {
        return VanillaCompat.itemResult(useWithoutItem(state, level, pos, player, hitResult));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }
}
