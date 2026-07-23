package com.nightbeam.tbos.blockentity;

import com.nightbeam.tbos.advancement.ModAdvancements;
import com.nightbeam.tbos.item.MemoryScene;
import com.nightbeam.tbos.registry.ModBlockEntities;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class MemoryLanternBlockEntity extends BlockEntity {
    public static final int PLAYBACK_DURATION_TICKS = 160;
    private MemoryScene scene;
    private boolean playing;
    private int playbackTicks;

    public MemoryLanternBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MEMORY_LANTERN.get(), pos, state);
    }

    public Optional<MemoryScene> scene() {
        return Optional.ofNullable(scene);
    }

    public boolean isPlaying() {
        return playing;
    }

    public int playbackTicks() {
        return playbackTicks;
    }

    public void select(MemoryScene selected) {
        scene = selected;
        playing = false;
        playbackTicks = 0;
        sync();
    }

    public boolean togglePlayback() {
        if (scene == null) {
            return false;
        }
        playing = !playing;
        playbackTicks = 0;
        sync();
        return playing;
    }

    public boolean clearScene() {
        if (scene == null) {
            return false;
        }
        scene = null;
        playing = false;
        playbackTicks = 0;
        sync();
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MemoryLanternBlockEntity lantern) {
        if (!(level instanceof ServerLevel serverLevel) || !lantern.playing || lantern.scene == null) {
            return;
        }
        lantern.playbackTicks++;
        if (lantern.playbackTicks < PLAYBACK_DURATION_TICKS) {
            return;
        }
        lantern.playbackTicks = 0;
        for (var player : serverLevel.getPlayers(player -> player.distanceToSqr(
                        pos.getX() + 0.5D,
                        pos.getY() + 0.5D,
                        pos.getZ() + 0.5D) <= 16.0D * 16.0D)) {
            ModAdvancements.awardCompleteMemoryScene(player);
        }
        lantern.setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        scene = input.read("scene", MemoryScene.CODEC).orElse(null);
        playing = scene != null && input.getBooleanOr("playing", false);
        playbackTicks = Math.clamp(input.getIntOr("playback_ticks", 0), 0, PLAYBACK_DURATION_TICKS - 1);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (scene != null) {
            output.store("scene", MemoryScene.CODEC, scene);
        }
        output.putBoolean("playing", playing);
        output.putInt("playback_ticks", playbackTicks);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }
}
