package com.nightbeam.tbos.block;

import com.nightbeam.tbos.site.TemporalSiteManager;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class MeridianRelayBlock extends Block {
    public static final MapCodec<MeridianRelayBlock> CODEC = simpleCodec(MeridianRelayBlock::new);
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    public MeridianRelayBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends MeridianRelayBlock> codec() {
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
                && TemporalSiteManager.moveMeridianRelay(serverPlayer, pos)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(POWERED)) {
            return;
        }
        // Charge arcs around the gimbal ring and vents upward from the conductor.
        double angle = random.nextDouble() * Math.PI * 2.0D;
        level.addParticle(
                ParticleTypes.ELECTRIC_SPARK,
                pos.getX() + 0.5D + Math.cos(angle) * 0.32D,
                pos.getY() + 0.63D,
                pos.getZ() + 0.5D + Math.sin(angle) * 0.32D,
                -Math.sin(angle) * 0.06D,
                0.0D,
                Math.cos(angle) * 0.06D);
        if (random.nextInt(3) == 0) {
            level.addParticle(
                    ParticleTypes.END_ROD,
                    pos.getX() + 0.42D + random.nextDouble() * 0.16D,
                    pos.getY() + 0.85D,
                    pos.getZ() + 0.42D + random.nextDouble() * 0.16D,
                    0.0D,
                    0.03D,
                    0.0D);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }
}
