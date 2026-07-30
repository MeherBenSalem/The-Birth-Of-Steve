package com.nightbeam.tbos.world;

import com.nightbeam.tbos.registry.ModItems;
import com.nightbeam.tbos.site.TemporalSiteManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * First-join guidance. A player who spawns has no in-game signal that the mod
 * exists, so their first Overworld login hands them the Archivist's Journal and
 * points them at the Fracture Shrines.
 */
public final class PlayerOnboarding {
    private PlayerOnboarding() {
    }

    public static void onPlayerLoggedIn(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD)) {
            return;
        }
        // The greeted set lives in overworld SavedData, so this survives death,
        // dimension changes, reconnects, and server restarts.
        if (!TemporalSiteManager.data(level).markGreeted(player.getUUID())) {
            return;
        }
        grantJournal(player);
        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS,
                0.7F,
                1.1F);
        player.sendSystemMessage(Component.translatable("message.tbos.onboarding.welcome")
                .withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(Component.translatable("message.tbos.onboarding.journal")
                .withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.translatable(
                        "message.tbos.onboarding.shrines",
                        AdventureWorldManager.MIN_SHRINE_DISTANCE,
                        AdventureWorldManager.MAX_SHRINE_DISTANCE)
                .withStyle(ChatFormatting.GRAY));
    }

    public static void grantJournal(ServerPlayer player) {
        ItemStack journal = new ItemStack(ModItems.ARCHIVISTS_JOURNAL.get());
        if (!player.addItem(journal)) {
            player.drop(journal, false);
        }
    }
}
