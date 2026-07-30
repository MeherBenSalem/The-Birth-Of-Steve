package com.nightbeam.tbos.item;

import com.nightbeam.tbos.run.ArchiveEncounterManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A run relic that restores one shared revive when the party has lost one. */
public final class RecalledHourItem extends Item {
    public RecalledHourItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer
                && ArchiveEncounterManager.restoreSharedRevive(serverPlayer)) {
            ItemStack stack = player.getItemInHand(hand);
            stack.shrink(1);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }
}
