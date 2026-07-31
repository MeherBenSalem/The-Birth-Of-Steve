package com.nightbeam.tbos.block;

import com.nightbeam.tbos.run.ArchiveRunManager;
import com.nightbeam.tbos.run.ArchiveDimensions;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The post-Curator gateway that starts a Fractured Archive run. */
public final class RiftThresholdBlock extends Block {
    public static final MapCodec<RiftThresholdBlock> CODEC = simpleCodec(RiftThresholdBlock::new);
    private static final int VEIL_CYAN = 0x8CEFE4;

    public RiftThresholdBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends RiftThresholdBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Portal motes fall inward toward the veil; the gateway takes you somewhere
        // rather than spilling the Archive out here.
        for (int index = 0; index < 4; index++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = 0.9D + random.nextDouble() * 0.8D;
            double x = pos.getX() + 0.5D + Math.cos(angle) * radius;
            double y = pos.getY() + random.nextDouble() * 1.4D;
            double z = pos.getZ() + 0.5D + Math.sin(angle) * radius;
            level.addParticle(
                    ParticleTypes.PORTAL,
                    x,
                    y,
                    z,
                    (pos.getX() + 0.5D - x) * 0.35D,
                    (pos.getY() + 0.6D - y) * 0.35D,
                    (pos.getZ() + 0.5D - z) * 0.35D);
        }
        if (random.nextInt(2) == 0) {
            level.addParticle(
                    ParticleTypes.REVERSE_PORTAL,
                    pos.getX() + 0.3D + random.nextDouble() * 0.4D,
                    pos.getY() + 0.2D + random.nextDouble() * 0.9D,
                    pos.getZ() + 0.3D + random.nextDouble() * 0.4D,
                    0.0D,
                    0.03D,
                    0.0D);
        }
        if (random.nextInt(6) == 0) {
            level.addParticle(
                    new DustParticleOptions(VEIL_CYAN, 1.3F),
                    pos.getX() + 0.5D,
                    pos.getY() + 0.4D + random.nextDouble() * 0.5D,
                    pos.getZ() + 0.5D,
                    0.0D,
                    0.0D,
                    0.0D);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return activate(level, pos, player);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult) {
        return activate(level, pos, player);
    }

    private static InteractionResult activate(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (level.dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE)) {
                ArchiveRunManager.advanceFromRewardGateway(serverPlayer, pos);
            } else {
                ArchiveRunManager.enterFromThreshold(serverPlayer, pos);
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }
}
