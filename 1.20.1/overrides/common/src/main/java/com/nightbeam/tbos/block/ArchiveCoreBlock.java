package com.nightbeam.tbos.block;

import com.nightbeam.tbos.compat.VanillaCompat;
import com.nightbeam.tbos.blockentity.ArchiveCoreBlockEntity;
import com.nightbeam.tbos.site.TemporalSiteManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class ArchiveCoreBlock extends Block implements EntityBlock {
    private static final int CORE_VIOLET = 0x9C8AE8;

    public ArchiveCoreBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }


    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArchiveCoreBlockEntity(pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Enchant particles converge on the housing, so the core reads as pulling
        // catalogued moments inward rather than venting them.
        for (int index = 0; index < 3; index++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = 1.1D + random.nextDouble() * 0.6D;
            level.addParticle(
                    ParticleTypes.ENCHANT,
                    pos.getX() + 0.5D + Math.cos(angle) * radius,
                    pos.getY() + 0.2D + random.nextDouble() * 1.2D,
                    pos.getZ() + 0.5D + Math.sin(angle) * radius,
                    -Math.cos(angle) * 0.18D,
                    0.02D,
                    -Math.sin(angle) * 0.18D);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(
                    VanillaCompat.dust(CORE_VIOLET, 1.1F),
                    pos.getX() + 0.25D + random.nextDouble() * 0.5D,
                    pos.getY() + 0.3D + random.nextDouble() * 0.5D,
                    pos.getZ() + 0.25D + random.nextDouble() * 0.5D,
                    0.0D,
                    0.01D,
                    0.0D);
        }
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer
                && TemporalSiteManager.startCuratorEncounter(serverPlayer, pos)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
