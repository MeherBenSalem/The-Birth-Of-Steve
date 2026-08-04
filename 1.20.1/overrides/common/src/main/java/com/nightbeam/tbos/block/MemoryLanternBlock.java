package com.nightbeam.tbos.block;

import com.nightbeam.tbos.advancement.ModAdvancements;
import com.nightbeam.tbos.blockentity.MemoryLanternBlockEntity;
import com.nightbeam.tbos.item.MemoryPlateItem;
import com.nightbeam.tbos.item.MemoryScene;
import com.nightbeam.tbos.registry.ModBlockEntities;
import com.nightbeam.tbos.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import com.nightbeam.tbos.compat.VanillaCompat;
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
    private static final int GLASS_CYAN = 0x8CEFE4;

    public MemoryLanternBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }


    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MemoryLanternBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
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
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ModItems.MEMORY_PLATE.get())) {
            return useMemoryPlate(level, pos, player, stack);
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof MemoryLanternBlockEntity lantern)) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            if (lantern.clearScene()) {
                player.displayClientMessage(Component.translatable("message.tbos.lantern.cleared"), true);
                level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.7F, 0.8F);
            }
            return InteractionResult.SUCCESS;
        }
        if (lantern.scene().isEmpty()) {
            player.displayClientMessage(Component.translatable("message.tbos.lantern.empty"), true);
            return InteractionResult.SUCCESS;
        }

        boolean playing = lantern.togglePlayback();
        MemoryScene scene = lantern.scene().orElseThrow();
        player.displayClientMessage(Component.translatable(
                playing
                        ? "message.tbos.lantern.started"
                        : "message.tbos.lantern.stopped",
                Component.translatable(scene.titleKey())), true);
        level.playSound(
                null,
                pos,
                playing ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE,
                SoundSource.BLOCKS,
                0.8F,
                playing ? 1.25F : 0.75F);
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult useMemoryPlate(Level level, BlockPos pos, Player player, ItemStack stack) {
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
        player.displayClientMessage(Component.translatable(
                "message.tbos.lantern.scene_loaded",
                Component.translatable(scene.titleKey())), true);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.9F, 1.15F);
        if (player instanceof ServerPlayer serverPlayer && MemoryPlateItem.hasAllScenes(player)) {
            ModAdvancements.awardAllMemoryPlates(serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) {
            return;
        }
        level.addParticle(
                VanillaCompat.dust(GLASS_CYAN, 0.8F),
                pos.getX() + 0.35D + random.nextDouble() * 0.3D,
                pos.getY() + 0.2D + random.nextDouble() * 0.6D,
                pos.getZ() + 0.35D + random.nextDouble() * 0.3D,
                0.0D,
                0.015D,
                0.0D);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof ServerPlayer player) {
            ModAdvancements.awardMemoryLantern(player);
        }
    }
}
