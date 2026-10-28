package com.nightbeam.tbos.expedition;

import com.nightbeam.tbos.codex.Insight;
import com.nightbeam.tbos.codex.PlayerProfile;
import com.nightbeam.tbos.codex.Profiles;
import com.nightbeam.tbos.compat.RpgCompat;
import com.nightbeam.tbos.party.Parties;
import com.nightbeam.tbos.relic.RelicEffects;
import com.nightbeam.tbos.rpg.RpgSounds;
import com.nightbeam.tbos.run.ArchiveDimensions;
import com.nightbeam.tbos.run.ArchiveRun;
import com.nightbeam.tbos.run.ArchiveRunMember;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** Codex Insights in play. All of them work inside the Fractured Archive only. */
public final class InsightEffects {
    private InsightEffects() {
    }

    public static boolean inArchive(ServerPlayer player) {
        return player.level().dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE);
    }

    /** Every 10 ticks: Archivist's Grace and Bulwark Memory follow the player in and out of the Archive. */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 10 != 5) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerProfile profile = Profiles.get(player);
            boolean inside = inArchive(player);
            apply(player, RpgCompat.Stat.MAX_HEALTH, "insight_grace", inside ? 2.0 * profile.rank(Insight.ARCHIVISTS_GRACE) : 0);
            apply(player, RpgCompat.Stat.ARMOR, "insight_bulwark", inside ? profile.rank(Insight.BULWARK_MEMORY) : 0);
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    private static void apply(ServerPlayer player, RpgCompat.Stat stat, String key, double value) {
        if (value <= 0) {
            RpgCompat.removeModifier(player, stat, key);
        } else {
            RpgCompat.setModifier(player, stat, key, value, false);
        }
    }

    public static void onFloorStart(MinecraftServer server, ArchiveRun run) {
        for (ArchiveRunMember member : run.members()) {
            ServerPlayer player = server.getPlayerList().getPlayer(member.playerId());
            if (player == null || !inArchive(player)) {
                continue;
            }
            PlayerProfile profile = Profiles.get(player);
            if (profile.rank(Insight.SWIFT_RECOLLECTION) > 0) {
                RpgCompat.addEffect(player, RpgCompat.Effect.SPEED, 600, 0, true);
            }
            if (profile.rank(Insight.WAYSTONE_WARD) > 0) {
                RpgCompat.addEffect(player, RpgCompat.Effect.RESISTANCE, 200, 0, true);
            }
        }
    }

    /** Damage about to land on the player; returns the adjusted amount. */
    public static float onIncoming(ServerPlayer player, DamageSource source, float amount) {
        if (amount <= 0 || !inArchive(player)) {
            return amount;
        }
        PlayerProfile profile = Profiles.get(player);
        if (profile.rank(Insight.PARTY_BOND) > 0) {
            int near = Math.min(3, Parties.nearby(player, 16).size() - 1);
            amount *= 1 - 0.05F * near;
        }
        if (profile.rank(Insight.LAST_STAND) > 0
                && amount >= player.getHealth() + player.getAbsorptionAmount()
                && player.getHealth() > 1.0F
                && Expeditions.consumeLastStand(player)) {
            amount = Math.max(0, player.getHealth() + player.getAbsorptionAmount() - 1.0F);
            player.level().playSound(null, player.blockPosition(), RpgSounds.CODEX_INSIGHT.get(), SoundSource.PLAYERS, 1.0F, 0.7F);
            player.sendSystemMessage(Component.translatable("codex.tbos.insight.last_stand.triggered"), true);
            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 30, 0.4, 0.6, 0.4, 0.2);
            }
        }
        return amount;
    }

    public static void onAttack(ServerPlayer player, LivingEntity target, float amount) {
        if (RelicEffects.applyingBonus() || amount <= 0 || !inArchive(player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        int echo = Profiles.get(player).rank(Insight.ECHO_STRIKE);
        if (echo > 0 && target.isAlive() && player.getRandom().nextFloat() < 0.05F * echo) {
            RpgCompat.dust(level, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 0xA8F0F0, 1.0F, 10, 0.35, 0.02);
            RelicEffects.strike(level, target, player, amount * 0.5F);
        }
    }

    public static void onKill(ServerPlayer player, LivingEntity victim) {
        if (!inArchive(player)) {
            return;
        }
        int vampiric = Profiles.get(player).rank(Insight.VAMPIRIC_RECALL);
        if (vampiric > 0) {
            player.heal(vampiric);
        }
    }
}
