package com.nightbeam.tbos.block;

import com.nightbeam.tbos.advancement.ModAdvancements;
import com.nightbeam.tbos.blockentity.MemoryLanternBlockEntity;
import com.nightbeam.tbos.item.MemoryPlateItem;
import com.nightbeam.tbos.item.MemoryScene;
import com.nightbeam.tbos.registry.ModBlockEntities;
import com.nightbeam.tbos.registry.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class MemoryLanternBlock extends BaseEntityBlock {
    public static final MapCodec<MemoryLanternBlock> CODEC = simpleCodec(MemoryLanternBlock::new);

    public MemoryLanternBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends MemoryLanternBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MemoryLanternBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(
                        type,
                        ModBlockEntities.MEMORY_LANTERN.get(),
                        MemoryLanternBlockEntity::serverTick);
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
        if (!stack.is(ModItems.MEMORY_PLATE.get())) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof MemoryLanternBlockEntity lantern)) {
            return InteractionResult.PASS;
        }

        MemoryScene scene = MemoryPlateItem.scene(stack);
        lantern.select(scene);
        player.sendOverlayMessage(Component.translatable(
                "message.tbos.lantern.scene_loaded",
                Component.translatable(scene.titleKey())));
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.9F, 1.15F);
        if (player instanceof ServerPlayer serverPlayer && MemoryPlateItem.hasAllScenes(player)) {
            ModAdvancements.awardAllMemoryPlates(serverPlayer);
        }
        return InteractionResult.SUCCESS_SERVER;
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
        if (!(level.getBlockEntity(pos) instanceof MemoryLanternBlockEntity lantern)) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            if (lantern.clearScene()) {
                player.sendOverlayMessage(Component.translatable("message.tbos.lantern.cleared"));
                level.playSound(null, pos, SoundEvents.COPPER_BULB_TURN_OFF, SoundSource.BLOCKS, 0.7F, 0.8F);
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        if (lantern.scene().isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.tbos.lantern.empty"));
            return InteractionResult.SUCCESS_SERVER;
        }

        boolean playing = lantern.togglePlayback();
        MemoryScene scene = lantern.scene().orElseThrow();
        player.sendOverlayMessage(Component.translatable(
                playing
                        ? "message.tbos.lantern.started"
                        : "message.tbos.lantern.stopped",
                Component.translatable(scene.titleKey())));
        level.playSound(
                null,
                pos,
                playing ? SoundEvents.COPPER_BULB_TURN_ON : SoundEvents.COPPER_BULB_TURN_OFF,
                SoundSource.BLOCKS,
                0.8F,
                playing ? 1.25F : 0.75F);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof ServerPlayer player) {
            ModAdvancements.awardMemoryLantern(player);
        }
    }
}
