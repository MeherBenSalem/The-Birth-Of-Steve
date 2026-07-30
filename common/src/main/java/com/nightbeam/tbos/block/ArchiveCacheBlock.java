package com.nightbeam.tbos.block;

import com.nightbeam.tbos.run.ArchiveEncounterManager;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Break-to-claim Archive loot seal with durable shared or per-member ownership. */
public final class ArchiveCacheBlock extends Block {
    public static final MapCodec<ArchiveCacheBlock> CODEC = simpleCodec(ArchiveCacheBlock::new);

    public ArchiveCacheBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends ArchiveCacheBlock> codec() {
        return CODEC;
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
            return ArchiveEncounterManager.inspectArchiveCache(serverPlayer, pos)
                    ? InteractionResult.SUCCESS_SERVER
                    : InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }
}
