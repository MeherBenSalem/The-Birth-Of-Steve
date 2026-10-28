package com.nightbeam.tbos.rpg;

import com.mojang.brigadier.CommandDispatcher;
import com.nightbeam.tbos.codex.Bestiary;
import com.nightbeam.tbos.codex.Codex;
import com.nightbeam.tbos.codex.HallOfRecords;
import com.nightbeam.tbos.codex.Profiles;
import com.nightbeam.tbos.compat.RpgCompat;
import com.nightbeam.tbos.expedition.Attunement;
import com.nightbeam.tbos.expedition.Expeditions;
import com.nightbeam.tbos.expedition.InsightEffects;
import com.nightbeam.tbos.party.Parties;
import com.nightbeam.tbos.relic.RelicEffects;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * The one place the loaders call into for the 1.0 systems. Each loader entry point
 * forwards its lifecycle, tick, connection, death and command events here, so the
 * systems themselves stay loader- and version-neutral.
 */
public final class RpgEvents {
    private RpgEvents() {
    }

    /** Common init, from {@code Yesterglass.init()}: config, registries and network actions. */
    public static void init() {
        RpgConfig.load();
        RpgSounds.register();
        RpgItems.register();
        Codex.registerActions();
        Attunement.registerActions();
    }

    public static void onServerStarted(MinecraftServer server) {
        RpgConfig.load();
        Parties.load(server);
        HallOfRecords.load(server);
        Expeditions.load(server);
    }

    public static void onServerStopping(MinecraftServer server) {
        Profiles.saveAll(server);
        Parties.save(server);
        Expeditions.save(server);
        Profiles.clear();
        Parties.clear();
        HallOfRecords.clear();
        Expeditions.clear();
        RelicEffects.clear();
        Attunement.clear();
    }

    public static void onServerTick(MinecraftServer server) {
        Expeditions.tick(server);
        Parties.tick(server);
        RelicEffects.tick(server);
        InsightEffects.tick(server);
        Profiles.tick(server);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        Profiles.get(player);
        Codex.deliverPending(player);
        Codex.sync(player);
    }

    public static void onPlayerLeave(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        Profiles.unload(server, player.getUUID());
        RpgNet.forget(player.getUUID());
        RelicEffects.forget(player.getUUID());
        Attunement.forget(player.getUUID());
        Expeditions.forgetPlayer(player.getUUID());
    }

    /** After an entity has died. {@code source} names the killer, if any. */
    public static void onLivingDeath(LivingEntity entity, DamageSource source) {
        if (entity.level().isClientSide()) {
            return;
        }
        if (source != null && source.getEntity() instanceof ServerPlayer killer && killer != entity) {
            String id = RpgCompat.entityId(entity);
            if (Bestiary.tracked(id)) {
                com.nightbeam.tbos.codex.PlayerProfile profile = Profiles.get(killer);
                profile.credit(id);
                if (Bestiary.discovered(profile) >= Bestiary.ENTRIES.size()) {
                    RpgAdvancements.award(killer, RpgAdvancements.KNOW_THE_ARCHIVE);
                }
            }
            RelicEffects.onKill(killer, entity);
            InsightEffects.onKill(killer, entity);
        }
        Expeditions.onDeath(entity);
    }

    public static float onPlayerIncoming(ServerPlayer player, DamageSource source, float amount) {
        amount = RelicEffects.onIncoming(player, source, amount);
        return InsightEffects.onIncoming(player, source, amount);
    }

    public static void onPlayerAttack(ServerPlayer player, LivingEntity target, float amount) {
        RelicEffects.onAttack(player, target, amount);
        InsightEffects.onAttack(player, target, amount);
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        RpgCommands.register(dispatcher);
    }
}
