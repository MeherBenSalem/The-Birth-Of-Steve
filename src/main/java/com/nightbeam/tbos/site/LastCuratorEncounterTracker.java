package com.nightbeam.tbos.site;

import com.nightbeam.tbos.advancement.ModAdvancements;
import com.nightbeam.tbos.item.MemoryPlateItem;
import com.nightbeam.tbos.item.MemoryScene;
import com.nightbeam.tbos.registry.ModItems;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

public final class LastCuratorEncounterTracker {
    public static final String CURATOR_TAG = "tbos.last_curator";
    private static final int MAX_PENDING_ATTACKS = 6;
    private static final int MAX_AFTERIMAGES = 12;
    private static final Map<Key, Runtime> ACTIVE = new HashMap<>();

    private LastCuratorEncounterTracker() {
    }

    public static void startIfAbsent(ServerLevel level, TemporalSite site) {
        if (!site.definitionId().equals(BuiltInTemporalSites.GRAND_ORRERY_ID)
                || !LastCuratorProgress.isStarted(site.progressFlags())
                || LastCuratorProgress.isDefeated(site.progressFlags())) {
            return;
        }
        ACTIVE.computeIfAbsent(new Key(level.dimension(), site.siteId()), ignored -> new Runtime(site));
    }

    public static void tick(MinecraftServer server) {
        for (Key key : List.copyOf(ACTIVE.keySet())) {
            ServerLevel level = server.getLevel(key.dimension());
            if (level == null) {
                removeRuntime(key);
                continue;
            }
            TemporalSite site = TemporalSiteManager.data(level).find(key.siteId()).orElse(null);
            if (site == null
                    || !TemporalSiteManager.isSiteLoaded(level, site)
                    || !LastCuratorProgress.isStarted(site.progressFlags())
                    || LastCuratorProgress.isDefeated(site.progressFlags())) {
                removeRuntime(key);
                continue;
            }
            Runtime runtime = ACTIVE.get(key);
            if (!tickEncounter(level, site, runtime)) {
                removeRuntime(key);
            }
        }
    }

    public static void stop(ServerLevel level, TemporalSite site, boolean discardEntity) {
        Key key = new Key(level.dimension(), site.siteId());
        removeRuntime(key);
        if (discardEntity) {
            findCurator(level, site).ifPresent(IronGolem::discard);
        }
    }

    public static void clear() {
        for (Runtime runtime : ACTIVE.values()) {
            runtime.bossBar.removeAllPlayers();
        }
        ACTIVE.clear();
    }

    public static java.util.Optional<IronGolem> findCurator(ServerLevel level, TemporalSite site) {
        return level.getEntities(
                        EntityTypeTest.forClass(IronGolem.class),
                        bounds(site),
                        entity -> entity.entityTags().contains(CURATOR_TAG))
                .stream()
                .findFirst();
    }

    public static int rewardEntityCount(ServerLevel level, TemporalSite site) {
        return rewardEntityCount(level, site, entity -> entity.getItem().is(ModItems.CURATOR_CORE.get()));
    }

    public static int lanternRewardEntityCount(ServerLevel level, TemporalSite site) {
        return rewardEntityCount(level, site, entity -> entity.getItem().is(ModItems.MEMORY_LANTERN.get()));
    }

    public static int archiveFallPlateEntityCount(ServerLevel level, TemporalSite site) {
        return rewardEntityCount(level, site, entity -> entity.getItem().is(ModItems.MEMORY_PLATE.get())
                && MemoryPlateItem.scene(entity.getItem()) == MemoryScene.ARCHIVE_FALL);
    }

    private static int rewardEntityCount(
            ServerLevel level,
            TemporalSite site,
            java.util.function.Predicate<ItemEntity> predicate) {
        return level.getEntities(
                        EntityTypeTest.forClass(ItemEntity.class),
                        bounds(site),
                        predicate)
                .size();
    }

    public static void clearRewardEntities(ServerLevel level, TemporalSite site) {
        level.getEntities(
                        EntityTypeTest.forClass(ItemEntity.class),
                        bounds(site),
                        entity -> entity.getItem().is(ModItems.CURATOR_CORE.get())
                                || entity.getItem().is(ModItems.MEMORY_LANTERN.get())
                                || entity.getItem().is(ModItems.MEMORY_PLATE.get()))
                .forEach(ItemEntity::discard);
    }

    private static boolean tickEncounter(ServerLevel level, TemporalSite site, Runtime runtime) {
        IronGolem curator = findCurator(level, site).orElse(null);
        if (curator == null) {
            curator = spawnCurator(level, site);
        }
        if (curator == null) {
            return true;
        }

        boolean vulnerable = LastCuratorProgress.isVulnerable(site.progressFlags(), site.state());
        if (!curator.isAlive() || curator.getHealth() <= 0.0F) {
            if (vulnerable) {
                defeat(level, site, curator, runtime);
                return false;
            }
            curator.discard();
            return true;
        }

        int storedHealth = LastCuratorProgress.health(site.progressFlags());
        int observedHealth = Math.max(1, (int) Math.ceil(curator.getHealth()));
        if (observedHealth < storedHealth) {
            if (vulnerable) {
                TemporalSite updated = site.withProgressFlags(
                        LastCuratorProgress.recordHealth(site.progressFlags(), observedHealth));
                TemporalSiteManager.data(level).replace(updated);
                TemporalSiteManager.broadcastSnapshot(level, updated);
                site = updated;
                storedHealth = observedHealth;
            } else {
                curator.setHealth(storedHealth);
            }
        } else if (observedHealth > storedHealth) {
            curator.setHealth(storedHealth);
        }

        LastCuratorProgress.Phase phase = LastCuratorProgress.phase(site.progressFlags());
        applyFormAndVulnerability(level, site, curator, phase);
        updateBossBar(level, site, curator, runtime, phase);
        announcePhaseChange(level, site, runtime, phase);
        handleStateChange(level, site, runtime);
        tickHalo(level, site, curator);

        List<ServerPlayer> players = playersInArena(level, site);
        if (!players.isEmpty()) {
            ServerPlayer nearest = players.stream()
                    .min(java.util.Comparator.comparingDouble(curator::distanceToSqr))
                    .orElse(null);
            curator.setTarget(nearest);
            leashToArena(site, curator);
            processPendingAttacks(level, players, runtime);
            if (!site.isTransitioning()) {
                scheduleAttacks(level, site, players, runtime, phase);
            }
        } else {
            curator.setTarget(null);
        }
        return true;
    }

    private static IronGolem spawnCurator(ServerLevel level, TemporalSite site) {
        IronGolem curator = EntityType.IRON_GOLEM.create(level, EntitySpawnReason.EVENT);
        if (curator == null) {
            return null;
        }
        BlockPos spawn = orrery(site).bossSpawn();
        BlockPos worldSpawn = definition(site).worldPosition(site.origin(), spawn, site.rotation());
        curator.snapTo(worldSpawn.getX() + 0.5D, worldSpawn.getY(), worldSpawn.getZ() + 0.5D, 180.0F, 0.0F);
        curator.addTag(CURATOR_TAG);
        curator.setPlayerCreated(false);
        curator.setPersistenceRequired();
        curator.setCustomNameVisible(true);
        AttributeInstance maxHealth = curator.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(LastCuratorProgress.MAX_HEALTH);
        }
        curator.setHealth(LastCuratorProgress.health(site.progressFlags()));
        level.addFreshEntity(curator);
        return curator;
    }

    private static void applyFormAndVulnerability(
            ServerLevel level,
            TemporalSite site,
            IronGolem curator,
            LastCuratorProgress.Phase phase) {
        boolean remembered = site.state().targetStableState() == TemporalState.REMEMBERED;
        boolean vulnerable = LastCuratorProgress.isVulnerable(site.progressFlags(), site.state());
        curator.setInvulnerable(!vulnerable);
        curator.setGlowingTag(remembered);
        curator.setCustomName(Component.translatable(
                "boss.tbos.last_curator.name",
                phaseComponent(phase),
                Component.translatable(remembered
                        ? "boss.tbos.last_curator.form.remembered"
                        : "boss.tbos.last_curator.form.ruin")));
        AttributeInstance movement = curator.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) {
            movement.setBaseValue(switch (phase) {
                case CATALOGUE -> remembered ? 0.27D : 0.18D;
                case REVISION -> remembered ? 0.34D : 0.24D;
                case ERASURE -> 0.36D;
            });
        }
    }

    private static void updateBossBar(
            ServerLevel level,
            TemporalSite site,
            IronGolem curator,
            Runtime runtime,
            LastCuratorProgress.Phase phase) {
        runtime.bossBar.setProgress(Math.max(0.0F, curator.getHealth() / LastCuratorProgress.MAX_HEALTH));
        runtime.bossBar.setColor(switch (phase) {
            case CATALOGUE -> BossEvent.BossBarColor.WHITE;
            case REVISION -> BossEvent.BossBarColor.BLUE;
            case ERASURE -> BossEvent.BossBarColor.PURPLE;
        });
        runtime.bossBar.setName(curator.getCustomName());
        Set<ServerPlayer> current = new HashSet<>(playersInArena(level, site));
        for (ServerPlayer existing : List.copyOf(runtime.bossBar.getPlayers())) {
            if (!current.contains(existing)) {
                runtime.bossBar.removePlayer(existing);
            }
        }
        for (ServerPlayer player : current) {
            if (!runtime.bossBar.getPlayers().contains(player)) {
                runtime.bossBar.addPlayer(player);
            }
        }
    }

    private static void announcePhaseChange(
            ServerLevel level,
            TemporalSite site,
            Runtime runtime,
            LastCuratorProgress.Phase phase) {
        if (runtime.lastPhase == phase) {
            return;
        }
        runtime.lastPhase = phase;
        TemporalSiteManager.notifyNearby(level, site, Component.translatable(switch (phase) {
            case CATALOGUE -> "message.tbos.curator.phase_catalogue";
            case REVISION -> "message.tbos.curator.phase_revision";
            case ERASURE -> "message.tbos.curator.phase_erasure";
        }));
        level.playSound(
                null,
                definition(site).transitionCenter(site.origin(), site.rotation()),
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.HOSTILE,
                1.5F,
                switch (phase) {
                    case CATALOGUE -> 0.55F;
                    case REVISION -> 0.85F;
                    case ERASURE -> 1.25F;
                });
    }

    private static void handleStateChange(ServerLevel level, TemporalSite site, Runtime runtime) {
        if (!site.state().isStable() || runtime.lastStableState == site.state()) {
            return;
        }
        runtime.lastStableState = site.state();
        if (!runtime.afterimages.isEmpty()) {
            List<BlockPos> cells = runtime.afterimages.stream().map(Afterimage::position).toList();
            detonate(level, playersInArena(level, site), new PendingAttack(cells, 0L, 7.0F));
            runtime.afterimages.clear();
            TemporalSiteManager.notifyNearbyOverlay(
                    level,
                    site,
                    Component.translatable("message.tbos.curator.afterimages_detonate"));
        }
        TemporalSiteManager.notifyNearby(level, site, Component.translatable(
                LastCuratorProgress.isVulnerable(site.progressFlags(), site.state())
                        ? "message.tbos.curator.core_exposed"
                        : "message.tbos.curator.core_sealed"));
    }

    private static void scheduleAttacks(
            ServerLevel level,
            TemporalSite site,
            List<ServerPlayer> players,
            Runtime runtime,
            LastCuratorProgress.Phase phase) {
        long gameTime = level.getGameTime();
        if (gameTime < runtime.nextAttackTick || runtime.pending.size() >= MAX_PENDING_ATTACKS) {
            return;
        }
        switch (phase) {
            case CATALOGUE -> {
                ServerPlayer target = players.get((int) Math.floorMod(gameTime, players.size()));
                List<BlockPos> line = debrisLine(site, target.blockPosition(), (gameTime / 80L & 1L) == 0L);
                telegraph(level, site, line, "message.tbos.curator.telegraph_catalogue");
                runtime.pending.addLast(new PendingAttack(line, gameTime + 22L, 6.0F));
                runtime.nextAttackTick = gameTime + 80L;
            }
            case REVISION -> {
                List<BlockPos> arc = rotatingArc(site, runtime.arcStep++);
                telegraph(level, site, arc, "message.tbos.curator.telegraph_revision");
                runtime.pending.addLast(new PendingAttack(arc, gameTime + 16L, 5.0F));
                runtime.nextAttackTick = gameTime + 52L;
            }
            case ERASURE -> {
                for (ServerPlayer player : players) {
                    if (runtime.afterimages.size() >= MAX_AFTERIMAGES) {
                        runtime.afterimages.removeFirst();
                    }
                    runtime.afterimages.addLast(new Afterimage(player.blockPosition().immutable(), site.state()));
                    level.sendParticles(
                            ParticleTypes.WITCH,
                            player.getX(),
                            player.getY() + 0.2D,
                            player.getZ(),
                            8,
                            0.25D,
                            0.05D,
                            0.25D,
                            0.01D);
                }
                TemporalSiteManager.notifyNearbyOverlay(
                        level,
                        site,
                        Component.translatable("message.tbos.curator.telegraph_erasure"));
                runtime.nextAttackTick = gameTime + 55L;
                if (!site.isTransitioning() && gameTime >= runtime.nextAutomaticShiftTick) {
                    TemporalSiteManager.beginAutomaticCuratorTransition(level, site);
                    runtime.nextAutomaticShiftTick = gameTime + 140L;
                }
            }
        }
    }

    private static void processPendingAttacks(
            ServerLevel level,
            List<ServerPlayer> players,
            Runtime runtime) {
        long gameTime = level.getGameTime();
        while (!runtime.pending.isEmpty() && runtime.pending.getFirst().detonateTick() <= gameTime) {
            detonate(level, players, runtime.pending.removeFirst());
        }
    }

    private static void telegraph(ServerLevel level, TemporalSite site, List<BlockPos> cells, String messageKey) {
        for (BlockPos cell : cells) {
            level.sendParticles(
                    ParticleTypes.SMOKE,
                    cell.getX() + 0.5D,
                    cell.getY() + 0.15D,
                    cell.getZ() + 0.5D,
                    1,
                    0.05D,
                    0.02D,
                    0.05D,
                    0.0D);
        }
        TemporalSiteManager.notifyNearbyOverlay(level, site, Component.translatable(messageKey));
    }

    private static void detonate(ServerLevel level, List<ServerPlayer> players, PendingAttack attack) {
        for (int index = 0; index < attack.cells().size(); index += 2) {
            BlockPos cell = attack.cells().get(index);
            level.sendParticles(
                    ParticleTypes.EXPLOSION,
                    cell.getX() + 0.5D,
                    cell.getY() + 0.2D,
                    cell.getZ() + 0.5D,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D);
        }
        for (ServerPlayer player : players) {
            boolean hit = attack.cells().stream().anyMatch(cell -> {
                double dx = player.getX() - (cell.getX() + 0.5D);
                double dz = player.getZ() - (cell.getZ() + 0.5D);
                return dx * dx + dz * dz <= 2.25D && Math.abs(player.getY() - cell.getY()) <= 3.0D;
            });
            if (hit) {
                player.hurtServer(level, level.damageSources().magic(), attack.damage());
            }
        }
    }

    private static List<BlockPos> debrisLine(TemporalSite site, BlockPos target, boolean alongX) {
        List<BlockPos> cells = new ArrayList<>();
        int y = site.origin().getY() + 1;
        for (int offset = -9; offset <= 9; offset++) {
            BlockPos cell = alongX
                    ? new BlockPos(target.getX() + offset, y, target.getZ())
                    : new BlockPos(target.getX(), y, target.getZ() + offset);
            if (site.contains(cell)) {
                cells.add(cell);
            }
        }
        return List.copyOf(cells);
    }

    private static List<BlockPos> rotatingArc(TemporalSite site, int arcStep) {
        BlockPos center = definition(site).transitionCenter(site.origin(), site.rotation());
        double angle = arcStep * Math.PI / 8.0D;
        List<BlockPos> cells = new ArrayList<>();
        for (int radius = 3; radius <= 13; radius++) {
            cells.add(new BlockPos(
                    center.getX() + (int) Math.round(Math.cos(angle) * radius),
                    site.origin().getY() + 1,
                    center.getZ() + (int) Math.round(Math.sin(angle) * radius)));
        }
        return cells.stream().distinct().toList();
    }

    private static void tickHalo(ServerLevel level, TemporalSite site, IronGolem curator) {
        if (level.getGameTime() % 5L != 0L) {
            return;
        }
        boolean remembered = site.state().targetStableState() == TemporalState.REMEMBERED;
        int points = remembered ? 8 : 4;
        for (int index = 0; index < points; index++) {
            double angle = Math.PI * 2.0D * index / 8.0D + level.getGameTime() * 0.04D;
            double x = curator.getX() + Math.cos(angle) * 1.45D;
            double z = curator.getZ() + Math.sin(angle) * 1.45D;
            level.sendParticles(
                    remembered ? ParticleTypes.END_ROD : ParticleTypes.ASH,
                    x,
                    curator.getY() + 2.4D,
                    z,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D);
        }
    }

    private static void leashToArena(TemporalSite site, IronGolem curator) {
        BlockPos center = definition(site).transitionCenter(site.origin(), site.rotation());
        double dx = curator.getX() - (center.getX() + 0.5D);
        double dz = curator.getZ() - (center.getZ() + 0.5D);
        if (dx * dx + dz * dz > 14.0D * 14.0D) {
            BlockPos spawn = definition(site).worldPosition(site.origin(), orrery(site).bossSpawn(), site.rotation());
            curator.snapTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        }
    }

    private static void defeat(ServerLevel level, TemporalSite site, IronGolem curator, Runtime runtime) {
        curator.discard();
        int flags = LastCuratorProgress.recordHealth(site.progressFlags(), 0);
        if (!LastCuratorProgress.isRewardGranted(flags)) {
            BlockPos rewardPos = definition(site).worldPosition(site.origin(), orrery(site).archiveCore(), site.rotation());
            Block.popResource(level, rewardPos.above(), new ItemStack(ModItems.CURATOR_CORE.get()));
            Block.popResource(level, rewardPos.above(), new ItemStack(ModItems.MEMORY_LANTERN.get()));
            Block.popResource(level, rewardPos.above(), MemoryPlateItem.forScene(MemoryScene.ARCHIVE_FALL));
            flags = LastCuratorProgress.markRewardGranted(flags);
        }
        TemporalSite defeated = site.withProgressFlags(flags).stable(TemporalState.RUIN);
        TemporalSiteManager.data(level).replace(defeated);
        TemporalSiteManager.applyPhaseGeometry(level, defeated);
        TemporalSiteManager.broadcastSnapshot(level, defeated);
        TemporalSiteManager.notifyNearby(level, defeated, Component.translatable("message.tbos.curator.defeated"));
        for (ServerPlayer player : playersInArena(level, defeated)) {
            ModAdvancements.awardLastCurator(player);
        }
        level.playSound(
                null,
                definition(site).transitionCenter(site.origin(), site.rotation()),
                SoundEvents.TOTEM_USE,
                SoundSource.HOSTILE,
                1.5F,
                0.7F);
        runtime.bossBar.removeAllPlayers();
    }

    private static List<ServerPlayer> playersInArena(ServerLevel level, TemporalSite site) {
        AABB bounds = bounds(site);
        return level.getPlayers(player -> !player.isSpectator() && bounds.contains(player.position()));
    }

    private static AABB bounds(TemporalSite site) {
        TemporalSiteDefinition definition = definition(site);
        BlockPos first = definition.worldPosition(site.origin(), BlockPos.ZERO, site.rotation());
        BlockPos second = definition.worldPosition(
                site.origin(),
                new BlockPos(definition.sizeX() - 1, 0, definition.sizeZ() - 1),
                site.rotation());
        return new AABB(
                Math.min(first.getX(), second.getX()),
                site.origin().getY() + definition.minY(),
                Math.min(first.getZ(), second.getZ()),
                Math.max(first.getX(), second.getX()) + 1.0D,
                site.origin().getY() + definition.maxY() + 1.0D,
                Math.max(first.getZ(), second.getZ()) + 1.0D);
    }

    private static TemporalSiteDefinition definition(TemporalSite site) {
        return BuiltInTemporalSites.require(site.definitionId());
    }

    private static OrreryDefinition orrery(TemporalSite site) {
        return definition(site).orreries().getFirst();
    }

    private static Component phaseComponent(LastCuratorProgress.Phase phase) {
        return Component.translatable(switch (phase) {
            case CATALOGUE -> "boss.tbos.last_curator.phase.catalogue";
            case REVISION -> "boss.tbos.last_curator.phase.revision";
            case ERASURE -> "boss.tbos.last_curator.phase.erasure";
        });
    }

    private static void removeRuntime(Key key) {
        Runtime removed = ACTIVE.remove(key);
        if (removed != null) {
            removed.bossBar.removeAllPlayers();
        }
    }

    private record Key(ResourceKey<Level> dimension, UUID siteId) {
    }

    private record PendingAttack(List<BlockPos> cells, long detonateTick, float damage) {
        private PendingAttack {
            cells = List.copyOf(cells);
        }
    }

    private record Afterimage(BlockPos position, TemporalState createdState) {
    }

    private static final class Runtime {
        private final ServerBossEvent bossBar;
        private final ArrayDeque<PendingAttack> pending = new ArrayDeque<>();
        private final ArrayDeque<Afterimage> afterimages = new ArrayDeque<>();
        private TemporalState lastStableState;
        private LastCuratorProgress.Phase lastPhase;
        private long nextAttackTick;
        private long nextAutomaticShiftTick;
        private int arcStep;

        private Runtime(TemporalSite site) {
            UUID barId = UUID.nameUUIDFromBytes(
                    ("tbos-curator-bar:" + site.siteId()).getBytes(StandardCharsets.UTF_8));
            bossBar = new ServerBossEvent(
                    barId,
                    Component.translatable("boss.tbos.last_curator.title"),
                    BossEvent.BossBarColor.WHITE,
                    BossEvent.BossBarOverlay.NOTCHED_10);
            bossBar.setDarkenScreen(true);
            bossBar.setCreateWorldFog(true);
            bossBar.setVisible(true);
            lastStableState = site.state().isStable() ? site.state() : site.state().previousStableState();
            nextAutomaticShiftTick = site.transitionStartTick() + 140L;
        }
    }
}
