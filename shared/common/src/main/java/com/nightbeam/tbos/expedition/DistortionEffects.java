package com.nightbeam.tbos.expedition;

import com.nightbeam.tbos.compat.RpgCompat;
import com.nightbeam.tbos.compat.VanillaCompat;
import com.nightbeam.tbos.registry.ModEntities;
import com.nightbeam.tbos.rpg.RpgConfig;
import com.nightbeam.tbos.rpg.RpgSounds;
import com.nightbeam.tbos.run.ArchiveDimensions;
import com.nightbeam.tbos.run.ArchiveEncounterKind;
import com.nightbeam.tbos.run.ArchiveEncounterState;
import com.nightbeam.tbos.run.ArchiveRoomPlacer;
import com.nightbeam.tbos.run.ArchiveRun;
import com.nightbeam.tbos.run.ArchiveRunMember;
import com.nightbeam.tbos.run.ArchiveRunSavedData;
import com.nightbeam.tbos.run.ArchiveRunStatus;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The live half of the Distortions: Bolstering and Volatile react to deaths,
 * Haunted sends Shades into fights. The stat Distortions (Fortified, Tyrannical)
 * are applied with the tier scaling in {@link Expeditions}; Frailty, Hourglass and
 * Amnesia are rules rather than effects.
 */
final class DistortionEffects {
    private record Burst(ServerLevel level, Vec3 at, long when, float damage, double radius) {
    }

    private record Shade(UUID run, int room, long spawned) {
    }

    private static final List<Burst> BURSTS = new ArrayList<>();
    private static final Map<UUID, Shade> SHADES = new LinkedHashMap<>();
    private static final int SHADE_LIFETIME_TICKS = 20 * 30;

    private DistortionEffects() {
    }

    static void onDeath(ServerLevel level, LivingEntity entity, ExpeditionState state) {
        SHADES.remove(entity.getUUID());
        int mask = state.key.distortions();
        RpgConfig.Distortions config = RpgConfig.get().distortions;
        if (Distortion.BOLSTERING.in(mask)) {
            bolster(level, entity, config);
        }
        if (Distortion.VOLATILE.in(mask)) {
            float damage = (float) (config.volatileDamage + state.key.tier() / 4.0);
            BURSTS.add(new Burst(level, entity.position(), level.getGameTime() + 30L, damage, config.volatileRadius));
            level.playSound(null, entity.blockPosition(), RpgSounds.DISTORTION_VOLATILE_HISS.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        }
    }

    private static void bolster(ServerLevel level, LivingEntity fallen, RpgConfig.Distortions config) {
        String runTag = null;
        for (String tag : RpgCompat.tags(fallen)) {
            if (tag.startsWith("tbos.run.")) {
                runTag = tag;
            }
        }
        if (runTag == null) {
            return;
        }
        boolean any = false;
        AABB area = fallen.getBoundingBox().inflate(config.bolsterRadius);
        for (Mob ally : level.getEntitiesOfClass(Mob.class, area)) {
            if (ally == fallen || !ally.isAlive() || !RpgCompat.tags(ally).contains(runTag)) {
                continue;
            }
            int amplifier = Math.min(2, RpgCompat.effectAmplifier(ally, RpgCompat.Effect.STRENGTH) + 1);
            RpgCompat.addEffect(ally, RpgCompat.Effect.STRENGTH, config.bolsterSeconds * 20, amplifier, true);
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, ally.getX(), ally.getY() + ally.getBbHeight() + 0.3,
                    ally.getZ(), 2, 0.25, 0.1, 0.25, 0.0);
            any = true;
        }
        if (any) {
            level.playSound(null, fallen.blockPosition(), RpgSounds.DISTORTION_BOLSTER.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        }
    }

    static void tickBursts(MinecraftServer server, long now) {
        for (Iterator<Burst> iterator = BURSTS.iterator(); iterator.hasNext(); ) {
            Burst burst = iterator.next();
            if (burst.level.getGameTime() >= burst.when) {
                iterator.remove();
                detonate(burst);
            } else if (burst.level.getGameTime() % 3 == 0) {
                burst.level.sendParticles(ParticleTypes.SMALL_FLAME, burst.at.x, burst.at.y + 0.4, burst.at.z, 4, 0.3, 0.2, 0.3, 0.01);
                burst.level.sendParticles(ParticleTypes.SMOKE, burst.at.x, burst.at.y + 0.3, burst.at.z, 2, 0.2, 0.1, 0.2, 0.01);
            }
        }
    }

    private static void detonate(Burst burst) {
        ServerLevel level = burst.level;
        level.sendParticles(ParticleTypes.EXPLOSION, burst.at.x, burst.at.y + 0.5, burst.at.z, 1, 0, 0, 0, 0);
        RpgCompat.dust(level, burst.at.x, burst.at.y + 0.5, burst.at.z, 0xFF6A3D, 1.6F, 40, burst.radius * 0.5, 0.05);
        level.playSound(null, BlockPos.containing(burst.at), RpgSounds.DISTORTION_VOLATILE_BURST.get(), SoundSource.HOSTILE, 1.2F, 1.0F);
        AABB box = new AABB(burst.at, burst.at).inflate(burst.radius);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box)) {
            if (!player.isSpectator() && player.position().distanceTo(burst.at) <= burst.radius) {
                RpgCompat.resetHurtCooldown(player);
                VanillaCompat.memoryDamage(level, player, null, burst.damage);
            }
        }
    }

    static void tickShades(MinecraftServer server, ArchiveRunSavedData storage, Map<UUID, ExpeditionState> states, long now) {
        ServerLevel archive = server.getLevel(ArchiveDimensions.FRACTURED_ARCHIVE);
        if (archive == null) {
            return;
        }
        // Retire Shades whose fight ended or whose time is up.
        for (Iterator<Map.Entry<UUID, Shade>> iterator = SHADES.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<UUID, Shade> entry = iterator.next();
            Shade shade = entry.getValue();
            Entity entity = archive.getEntity(entry.getKey());
            ArchiveRun run = storage.find(shade.run).orElse(null);
            boolean roomDone = run == null || shade.room < 0 || shade.room >= run.roomEncounterStates().size()
                    || run.roomEncounterStates().get(shade.room).complete();
            if (entity == null || !entity.isAlive() || roomDone || now - shade.spawned > SHADE_LIFETIME_TICKS) {
                if (entity != null && entity.isAlive()) {
                    archive.sendParticles(ParticleTypes.SOUL, entity.getX(), entity.getY() + 1, entity.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
                    entity.discard();
                }
                iterator.remove();
            }
        }
        int interval = Math.max(5, RpgConfig.get().distortions.hauntSeconds) * 20;
        for (ExpeditionState state : states.values()) {
            if (state.finalized || !Distortion.HAUNTED.in(state.key.distortions()) || now - state.lastHaunt < interval) {
                continue;
            }
            ArchiveRun run = storage.find(state.run).orElse(null);
            if (run == null || run.status() != ArchiveRunStatus.ACTIVE) {
                continue;
            }
            List<ServerPlayer> fighting = new ArrayList<>();
            List<Integer> rooms = new ArrayList<>();
            for (ArchiveRunMember member : run.members()) {
                ServerPlayer player = server.getPlayerList().getPlayer(member.playerId());
                if (player == null || player.level() != archive || !player.isAlive()) {
                    continue;
                }
                OptionalInt room = ArchiveRoomPlacer.roomContaining(run, player.blockPosition());
                if (room.isEmpty() || room.getAsInt() >= run.roomEncounterStates().size()) {
                    continue;
                }
                ArchiveEncounterState encounter = run.roomEncounterStates().get(room.getAsInt());
                ArchiveEncounterKind kind = run.rooms().get(room.getAsInt()).encounterKind();
                if (encounter.started() && !encounter.complete() && kind != ArchiveEncounterKind.EXPLORATION
                        && kind != ArchiveEncounterKind.REWARD) {
                    fighting.add(player);
                    rooms.add(room.getAsInt());
                }
            }
            if (fighting.isEmpty()) {
                continue;
            }
            state.lastHaunt = now;
            int pick = (int) Math.floorMod(now / interval, fighting.size());
            spawnShade(archive, run, state, fighting.get(pick), rooms.get(pick), now);
            Expeditions.markDirty();
        }
    }

    private static void spawnShade(ServerLevel level, ArchiveRun run, ExpeditionState state, ServerPlayer near, int room, long now) {
        Mob shade = RpgCompat.create(ModEntities.PARALLAX_WRAITH.get(), level);
        if (shade == null) {
            return;
        }
        double angle = (now % 360) * Math.PI / 180.0;
        Vec3 spot = near.position();
        for (int attempt = 0; attempt < 8; attempt++) {
            double a = angle + attempt * Math.PI / 4;
            Vec3 candidate = near.position().add(Math.cos(a) * 4.5, 0, Math.sin(a) * 4.5);
            BlockPos pos = BlockPos.containing(candidate);
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                    && !level.getBlockState(pos.below()).isAir()) {
                spot = candidate;
                break;
            }
        }
        shade.setPos(spot.x, spot.y, spot.z);
        shade.addTag("tbos.run." + run.runId());
        shade.addTag(Expeditions.SHADE_TAG);
        shade.addTag(Expeditions.TIERED_TAG);
        RpgCompat.setModifier(shade, RpgCompat.Stat.MAX_HEALTH, "shade_health", -0.5, true);
        RpgCompat.setModifier(shade, RpgCompat.Stat.ATTACK_DAMAGE, "tier_damage",
                RpgConfig.get().expeditions.damagePerTier * (state.key.tier() - 1), true);
        shade.setHealth(shade.getMaxHealth());
        shade.setTarget(near);
        level.addFreshEntity(shade);
        SHADES.put(shade.getUUID(), new Shade(run.runId(), room, now));
        level.sendParticles(ParticleTypes.SCULK_SOUL, spot.x, spot.y + 1, spot.z, 16, 0.4, 0.6, 0.4, 0.02);
        level.playSound(null, BlockPos.containing(spot), RpgSounds.DISTORTION_HAUNT.get(), SoundSource.HOSTILE, 1.2F, 1.0F);
    }

    static void clear() {
        BURSTS.clear();
        SHADES.clear();
    }
}
