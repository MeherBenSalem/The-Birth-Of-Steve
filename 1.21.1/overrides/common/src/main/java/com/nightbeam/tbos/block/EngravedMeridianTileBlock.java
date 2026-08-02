package com.nightbeam.tbos.block;

import com.mojang.serialization.MapCodec;
import com.nightbeam.tbos.site.TemporalSiteManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import com.nightbeam.tbos.compat.VanillaCompat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A carved routing surface used by the overworld Broken Meridian puzzle.
 * Ordinary decorative tiles remain inert; only authored sockets and remote
 * routing seals are accepted by {@link TemporalSiteManager}.
 */
public final class EngravedMeridianTileBlock extends Block {
    public static final MapCodec<EngravedMeridianTileBlock> CODEC =
            simpleCodec(EngravedMeridianTileBlock::new);
    public static final BooleanProperty CHARGED = BooleanProperty.create("charged");

    public EngravedMeridianTileBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CHARGED, false));
    }

    @Override
    protected MapCodec<? extends EngravedMeridianTileBlock> codec() {
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
                && TemporalSiteManager.routeBrokenMeridian(serverPlayer, pos)) {
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGED);
    }
}
