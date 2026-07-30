package com.nightbeam.tbos.block;

import com.nightbeam.tbos.item.MemoryPlateItem;
import com.nightbeam.tbos.registry.ModItems;
import com.nightbeam.tbos.world.FractureShrineVariant;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/** A persistent one-use shrine cache with an entirely custom Lens repair kit. */
public final class FractureCofferBlock extends Block {
    public static final MapCodec<FractureCofferBlock> CODEC = simpleCodec(FractureCofferBlock::new);
    public static final BooleanProperty OPENED = BooleanProperty.create("opened");
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 2);

    public FractureCofferBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(OPENED, false).setValue(VARIANT, 0));
    }

    @Override
    protected MapCodec<? extends FractureCofferBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return open(state, level, pos, player);
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
        return open(state, level, pos, player);
    }

    private static InteractionResult open(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (state.getValue(OPENED)) {
            serverPlayer.sendOverlayMessage(Component.literal("FRACTURE COFFER  ·  Already recalled")
                    .withStyle(ChatFormatting.GRAY));
            return InteractionResult.SUCCESS_SERVER;
        }
        FractureShrineVariant variant = FractureShrineVariant.values()[state.getValue(VARIANT)];
        for (ItemStack reward : lootForVariant(variant)) {
            if (!serverPlayer.addItem(reward.copy())) {
                serverPlayer.drop(reward.copy(), false);
            }
        }
        level.setBlock(pos, state.setValue(OPENED, true), Block.UPDATE_ALL);
        serverPlayer.sendOverlayMessage(Component.literal("FRACTURE COFFER  ·  Lens kit recovered")
                .withStyle(ChatFormatting.AQUA));
        return InteractionResult.SUCCESS_SERVER;
    }

    public static List<ItemStack> lootForVariant(FractureShrineVariant variant) {
        return List.of(
                new ItemStack(ModItems.CRACKED_YESTERGLASS_LENS.get()),
                new ItemStack(ModItems.ARCHIVE_SURVEY_MAP.get()),
                MemoryPlateItem.forScene(variant.memoryScenes().get(0)),
                MemoryPlateItem.forScene(variant.memoryScenes().get(1)),
                new ItemStack(ModItems.CHRONICLE_SHARD.get(), 3),
                new ItemStack(ModItems.YESTERGLASS.get(), 2),
                new ItemStack(ModItems.LENSWORK_CRYSTAL.get()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPENED, VARIANT);
    }
}
