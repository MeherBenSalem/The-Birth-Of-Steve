package com.nightbeam.tbos.block;

import com.nightbeam.tbos.site.TemporalSiteManager;
import com.nightbeam.tbos.run.ArchiveEncounterManager;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class ResonantBellBlock extends Block {
    public static final MapCodec<ResonantBellBlock> CODEC = simpleCodec(ResonantBellBlock::new);
    public static final IntegerProperty SYMBOL = IntegerProperty.create("symbol", 0, 3);
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public ResonantBellBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SYMBOL, 0).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends ResonantBellBlock> codec() {
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
                && (ArchiveEncounterManager.ringChoirBell(serverPlayer, pos)
                        || TemporalSiteManager.ringChoirBell(serverPlayer, pos))) {
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
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
        return useWithoutItem(state, level, pos, player, hitResult);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.is(this) && state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SYMBOL, LIT);
    }
}
