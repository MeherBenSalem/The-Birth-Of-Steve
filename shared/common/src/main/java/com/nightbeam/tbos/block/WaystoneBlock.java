package com.nightbeam.tbos.block;

import com.mojang.serialization.MapCodec;
import com.nightbeam.tbos.run.ArchiveRunManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class WaystoneBlock extends Block {
    public static final MapCodec<WaystoneBlock> CODEC = simpleCodec(WaystoneBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
            Shapes.box(1.0D / 16.0D, 0.0D, 1.0D / 16.0D, 15.0D / 16.0D, 4.0D / 16.0D, 15.0D / 16.0D),
            Shapes.box(4.0D / 16.0D, 4.0D / 16.0D, 4.0D / 16.0D, 12.0D / 16.0D, 1.0D, 12.0D / 16.0D));

    public WaystoneBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends WaystoneBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult) {
        return activate(level, player);
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
        return activate(level, player);
    }

    private static InteractionResult activate(Level level, Player player) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        return ArchiveRunManager.useWaystone(serverPlayer)
                ? InteractionResult.SUCCESS_SERVER
                : InteractionResult.CONSUME;
    }
}
