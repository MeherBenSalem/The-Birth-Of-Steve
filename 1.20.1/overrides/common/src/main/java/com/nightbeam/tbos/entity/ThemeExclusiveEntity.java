package com.nightbeam.tbos.entity;

import net.minecraft.nbt.CompoundTag;

import com.nightbeam.tbos.block.ThemeHazardBlock;
import com.nightbeam.tbos.compat.VanillaCompat;
import com.nightbeam.tbos.registry.ModBlocks;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.BossEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Shared host for the sixteen floor-theme exclusives. Each instance resolves a
 * {@link ThemeExclusiveKind} from its entity type and runs that kind's
 * server-owned signature phase machine.
 */
public final class ThemeExclusiveEntity extends Monster {
    private static final EntityDataAccessor<Byte> ABILITY_PHASE =
            SynchedEntityData.defineId(ThemeExclusiveEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ABILITY_PHASE_TICKS =
            SynchedEntityData.defineId(ThemeExclusiveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FINAL_BOSS =
            SynchedEntityData.defineId(ThemeExclusiveEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> BOSS_PHASE =
            SynchedEntityData.defineId(ThemeExclusiveEntity.class, EntityDataSerializers.BYTE);

    private final ThemeExclusiveKind kind;
    private final ServerBossEvent bossEvent;
    private int abilityCooldown = 40;
    private boolean ashSplitDone;
    private BlockPos sealPos;
    private int sealTicks;

    public ThemeExclusiveEntity(EntityType<? extends ThemeExclusiveEntity> entityType, Level level) {
        super(entityType, level);
        kind = ThemeExclusiveKind.of(entityType);
        bossEvent = new ServerBossEvent(Component.translatable(entityType.getDescriptionId()),
                BossEvent.BossBarColor.PURPLE,
                BossEvent.BossBarOverlay.NOTCHED_10);
        bossEvent.setVisible(false);
        this.xpReward = 7;
    }

    public ThemeExclusiveKind kind() {
        return kind;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new SignatureAbilityGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.05D, false));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ABILITY_PHASE, (byte) AbilityPhase.IDLE.ordinal());
        this.entityData.define(ABILITY_PHASE_TICKS, 0);
        this.entityData.define(FINAL_BOSS, false);
        this.entityData.define(BOSS_PHASE, (byte) 0);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (isFinalBoss()) {
            int phase = getHealth() <= getMaxHealth() * 0.5F ? 2 : 1;
            entityData.set(BOSS_PHASE, (byte) phase);
            bossEvent.setProgress(getHealth() / getMaxHealth());
            bossEvent.setName(getDisplayName());
            bossEvent.setVisible(true);
        }
        if (getAbilityPhase() == AbilityPhase.IDLE && abilityCooldown > 0) {
            abilityCooldown--;
        }
        if (sealTicks > 0 && sealPos != null) {
            sealTicks--;
            if (sealTicks == 0) {
                if (level.getBlockState(sealPos).is(ModBlocks.ARCHIVE_SEAL.get())) {
                    level.removeBlock(sealPos, false);
                }
                sealPos = null;
            }
        }
        if (kind == ThemeExclusiveKind.GALLERY_MOTH && tickCount % 20 == 0) {
            ThemeHazardBlock.dimAround(level, blockPosition(), 3.5D);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag input) {
        super.readAdditionalSaveData(input);
        setAbilityPhase(AbilityPhase.IDLE);
        abilityCooldown = 40;
        ashSplitDone = false;
        sealPos = input.contains("temporary_seal") ? BlockPos.of(input.getLong("temporary_seal")) : null;
        sealTicks = sealPos == null ? 0 : Mth.clamp(input.getInt("temporary_seal_ticks"), 0, 60);
        setFinalBoss(input.getBoolean("archive_final_boss"));
        entityData.set(BOSS_PHASE, input.getByte("archive_boss_phase"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("archive_final_boss", isFinalBoss());
        output.putByte("archive_boss_phase", entityData.get(BOSS_PHASE));
        if (sealPos != null && sealTicks > 0) {
            output.putLong("temporary_seal", sealPos.asLong());
            output.putInt("temporary_seal_ticks", sealTicks);
        }
    }

    public boolean isFinalBoss() {
        return entityData.get(FINAL_BOSS);
    }

    public int bossPhase() {
        return entityData.get(BOSS_PHASE);
    }

    public void setFinalBoss(boolean finalBoss) {
        if (finalBoss && !kind.bossEligible()) {
            throw new IllegalStateException(kind + " cannot be promoted to an Archive boss");
        }
        entityData.set(FINAL_BOSS, finalBoss);
        refreshDimensions();
        bossEvent.setVisible(finalBoss);
        if (finalBoss) {
            xpReward = 50;
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        EntityDimensions dimensions = super.getDimensions(pose);
        // LivingEntity asks for dimensions from its constructor, before this
        // subclass has resolved its kind or defined synchronized boss data.
        return kind != null && isFinalBoss() ? dimensions.scale(1.18F) : dimensions;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (isFinalBoss()) {
            bossEvent.addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        float applied = amount;
        if (kind == ThemeExclusiveKind.PRISM_STALKER && getAbilityPhase() == AbilityPhase.IDLE) {
            applied *= 0.35F;
        }
        if (kind == ThemeExclusiveKind.WAKE_CUTTER && getAbilityPhase() == AbilityPhase.ACTIVE) {
            return false;
        }
        boolean hurt = super.hurt(source, applied);
        if (hurt
                && kind == ThemeExclusiveKind.ASH_CHORISTER
                && !ashSplitDone
                && getHealth() <= getMaxHealth() * 0.45F
                && level() instanceof ServerLevel level) {
            ashSplitDone = true;
            spawnAshWisp(level);
            spawnAshWisp(level);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource damageSource) {
        if (level() instanceof ServerLevel serverLevel) {
            if (kind == ThemeExclusiveKind.SHARDLING_SWARM) {
                ThemeHazardBlock.shatterNearby(serverLevel, blockPosition(), 2);
            }
            clearTemporarySeal(serverLevel);
        }
        super.die(damageSource);
    }

    private void clearTemporarySeal(ServerLevel level) {
        if (sealPos != null && level.getBlockState(sealPos).is(ModBlocks.ARCHIVE_SEAL.get())) {
            level.removeBlock(sealPos, false);
        }
        sealPos = null;
        sealTicks = 0;
    }

    public AbilityPhase getAbilityPhase() {
        AbilityPhase[] phases = AbilityPhase.values();
        return phases[Mth.clamp(entityData.get(ABILITY_PHASE), 0, phases.length - 1)];
    }

    public float getAbilityProgress(float partialTick) {
        AbilityPhase phase = getAbilityPhase();
        if (phase == AbilityPhase.IDLE) {
            return 0.0F;
        }
        return Mth.clamp((entityData.get(ABILITY_PHASE_TICKS) + partialTick) / phase.duration(kind), 0.0F, 1.0F);
    }

    public int getAbilityCooldown() {
        return abilityCooldown;
    }

    private void setAbilityPhase(AbilityPhase phase) {
        entityData.set(ABILITY_PHASE, (byte) phase.ordinal());
        entityData.set(ABILITY_PHASE_TICKS, 0);
    }

    private int advanceAbilityPhase() {
        int ticks = entityData.get(ABILITY_PHASE_TICKS) + 1;
        entityData.set(ABILITY_PHASE_TICKS, ticks);
        return ticks;
    }

    private void spawnAshWisp(ServerLevel level) {
        ThemeExclusiveEntity child = ModEntitiesHolder.createAshChorister(level);
        if (child == null) {
            return;
        }
        child.moveTo(getX() + (random.nextDouble() - 0.5D), getY(), getZ() + (random.nextDouble() - 0.5D), getYRot(), 0.0F);
        child.setHealth(Math.max(6.0F, getMaxHealth() * 0.35F));
        child.ashSplitDone = true;
        child.setTarget(getTarget());
        level.addFreshEntity(child);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return kind.ambientSound();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return kind.hurtSound();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return kind.deathSound();
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    public enum AbilityPhase {
        IDLE,
        WINDUP,
        ACTIVE,
        RECOVERY;

        int duration(ThemeExclusiveKind kind) {
            return switch (this) {
                case IDLE -> 1;
                case WINDUP -> switch (kind) {
                    case NULL_PORTRAIT -> 8;
                    case GNOMON_KNIGHT, HOUR_HAND_WRAITH -> 16;
                    case METRONOME_HOUND -> 10;
                    default -> 12;
                };
                case ACTIVE -> switch (kind) {
                    case WAKE_CUTTER -> 10;
                    case SHARD_DRIFTER -> 6;
                    case ARMILLARY_SCOUT -> 14;
                    default -> 8;
                };
                case RECOVERY -> 8;
            };
        }
    }

    private static final class SignatureAbilityGoal extends Goal {
        private final ThemeExclusiveEntity mob;
        private LivingEntity target;

        private SignatureAbilityGoal(ThemeExclusiveEntity mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity candidate = mob.getTarget();
            if (candidate == null
                    || !candidate.isAlive()
                    || mob.abilityCooldown > 0
                    || mob.getAbilityPhase() != AbilityPhase.IDLE) {
                return false;
            }
            double distance = mob.distanceToSqr(candidate);
            return switch (mob.kind) {
                case NULL_PORTRAIT -> mob.getSensing().hasLineOfSight(candidate) && distance <= 64.0D;
                case SHELF_CRAWLER -> distance <= 36.0D;
                case SHARD_DRIFTER, WAKE_CUTTER -> distance >= 9.0D && distance <= 81.0D;
                default -> distance <= 100.0D && mob.getSensing().hasLineOfSight(candidate);
            };
        }

        @Override
        public boolean canContinueToUse() {
            return mob.getAbilityPhase() != AbilityPhase.IDLE;
        }

        @Override
        public boolean isInterruptable() {
            return false;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            target = mob.getTarget();
            mob.getNavigation().stop();
            mob.setAbilityPhase(AbilityPhase.WINDUP);
            mob.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.55F, 1.2F);
        }

        @Override
        public void stop() {
            if (mob.getAbilityPhase() != AbilityPhase.IDLE) {
                mob.setAbilityPhase(AbilityPhase.IDLE);
                mob.abilityCooldown = Math.max(mob.abilityCooldown, 40);
            }
            target = null;
        }

        @Override
        public void tick() {
            if (!(mob.level() instanceof ServerLevel level) || target == null || !target.isAlive()) {
                mob.setAbilityPhase(AbilityPhase.IDLE);
                mob.abilityCooldown = 40;
                return;
            }
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            int ticks = mob.advanceAbilityPhase();
            AbilityPhase phase = mob.getAbilityPhase();
            int duration = phase.duration(mob.kind);
            if (ticks < duration) {
                if (phase == AbilityPhase.WINDUP) {
                    level.sendParticles(
                            mob.kind.telegraph(),
                            mob.getX(),
                            mob.getY() + mob.getBbHeight() * 0.6D,
                            mob.getZ(),
                            2,
                            0.15D,
                            0.2D,
                            0.15D,
                            0.0D);
                }
                return;
            }
            switch (phase) {
                case WINDUP -> {
                    mob.setAbilityPhase(AbilityPhase.ACTIVE);
                    resolveActive(level);
                }
                case ACTIVE -> mob.setAbilityPhase(AbilityPhase.RECOVERY);
                case RECOVERY -> {
                    mob.setAbilityPhase(AbilityPhase.IDLE);
                    mob.abilityCooldown = cooldownFor(mob.kind);
                }
                case IDLE -> {
                }
            }
        }

        private int cooldownFor(ThemeExclusiveKind kind) {
            int base = switch (kind) {
                case METRONOME_HOUND -> 50;
                case GALLERY_MOTH, SHARDLING_SWARM -> 70;
                case LABYRINTH_USHER -> 100;
                default -> 80;
            };
            return mob.isFinalBoss() && mob.bossPhase() >= 2 ? Math.max(35, base / 2) : base;
        }

        private void resolveActive(ServerLevel level) {
            switch (mob.kind) {
                case SHARD_DRIFTER -> blinkBehind(level);
                case WAKE_CUTTER -> dashThrough(level);
                case NULL_PORTRAIT -> lunge(level);
                case GALLERY_MOTH -> ThemeHazardBlock.dimAround(level, mob.blockPosition(), 4.0D);
                case GNOMON_KNIGHT -> pulse(level, 4.5D, 4.0F);
                case ARMILLARY_SCOUT -> dive(level);
                case DUST_CANTORILE -> pulseSlow(level);
                case ASH_CHORISTER -> pulse(level, 3.0D, 2.0F);
                case PRISM_STALKER -> openPrism(level);
                case SHARDLING_SWARM -> pulse(level, 2.0D, 2.5F);
                case INDEX_WIGHT -> markTarget(level);
                case SHELF_CRAWLER -> dropOnTarget(level);
                case METRONOME_HOUND -> biteOnBeat(level);
                case LABYRINTH_USHER -> sealNearby(level);
                case BLANK_CHRONIST -> weaken(level);
                case HOUR_HAND_WRAITH -> sweep(level);
            }
            if (mob.isFinalBoss()) {
                resolveBossFollowup(level);
            }
        }

        private void resolveBossFollowup(ServerLevel level) {
            double radius = mob.bossPhase() >= 2 ? 6.0D : 4.5D;
            switch (mob.kind) {
                case WAKE_CUTTER -> {
                    Vec3 trail = target.position().subtract(mob.position());
                    int steps = Math.max(4, (int) trail.length() * 2);
                    for (int step = 0; step <= steps; step++) {
                        Vec3 point = mob.position().add(trail.scale((double) step / steps));
                        level.sendParticles(ParticleTypes.CRIT, point.x, point.y + 0.2D, point.z,
                                2, 0.1D, 0.1D, 0.1D, 0.01D);
                    }
                }
                case NULL_PORTRAIT -> {
                    target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 45, 0, false, true, true));
                    level.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY() + 1.0D, mob.getZ(),
                            mob.bossPhase() >= 2 ? 30 : 18, 2.5D, 0.8D, 2.5D, 0.02D);
                }
                case GNOMON_KNIGHT -> radialBossPulse(level, radius, 5.0F, ParticleTypes.ELECTRIC_SPARK);
                case DUST_CANTORILE ->
                        radialBossPulse(level, radius, 3.5F, ParticleTypes.WHITE_ASH);
                case PRISM_STALKER -> {
                    radialBossPulse(level, radius, 4.0F, ParticleTypes.END_ROD);
                    mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 1, false, true, true));
                }
                case INDEX_WIGHT -> {
                    target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, true, true));
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 0, false, true, true));
                    level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + 1.0D, target.getZ(),
                            20, 0.6D, 0.8D, 0.6D, 0.02D);
                }
                case HOUR_HAND_WRAITH -> {
                    radialBossPulse(level, radius, 5.5F, ParticleTypes.SOUL_FIRE_FLAME);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true, true));
                }
                default -> {
                }
            }
        }

        private void radialBossPulse(
                ServerLevel level,
                double radius,
                float damage,
                net.minecraft.core.particles.ParticleOptions particle) {
            AABB box = mob.getBoundingBox().inflate(radius, 1.5D, radius);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, candidate ->
                    candidate.isAlive() && !(candidate instanceof Monster))) {
                entity.hurt(mob.damageSources().mobAttack(mob), damage);
                VanillaCompat.knockback(
                        entity,
                        0.55D,
                        mob.getX() - entity.getX(),
                        mob.getZ() - entity.getZ(),
                        mob.damageSources().mobAttack(mob),
                        damage);
            }
            level.sendParticles(particle, mob.getX(), mob.getY() + 0.2D, mob.getZ(),
                    mob.bossPhase() >= 2 ? 32 : 20, radius * 0.45D, 0.2D, radius * 0.45D, 0.04D);
        }

        /**
         * The Prism Stalker's shells part and it takes full damage until they
         * close again. The window was previously enforced only by a silent
         * check in {@link ThemeExclusiveEntity#hurtServer}, so a player had no
         * way to know when hitting it stopped being a waste of a swing. The
         * model opens on the same phase; this is the audible half of that tell.
         */
        private void openPrism(ServerLevel level) {
            level.sendParticles(
                    ParticleTypes.END_ROD,
                    mob.getX(),
                    mob.getY() + mob.getBbHeight() * 0.5D,
                    mob.getZ(),
                    14,
                    0.35D,
                    0.45D,
                    0.35D,
                    0.03D);
            mob.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.9F, 0.7F);
        }

        private void blinkBehind(ServerLevel level) {
            Vec3 behind = target.position().subtract(target.getLookAngle().scale(2.0D));
            mob.teleportTo(behind.x, target.getY(), behind.z);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, mob.getX(), mob.getY() + 1.0D, mob.getZ(), 12, 0.3D, 0.4D, 0.3D, 0.02D);
        }

        private void dashThrough(ServerLevel level) {
            Vec3 delta = target.position().subtract(mob.position()).normalize().scale(1.35D);
            mob.setDeltaMovement(delta.x, 0.15D, delta.z);
            mob.hurtMarked = true;
            if (mob.distanceToSqr(target) <= 6.25D) {
                target.hurt(mob.damageSources().mobAttack(mob), 5.0F);
            }
        }

        private void lunge(ServerLevel level) {
            Vec3 delta = target.position().subtract(mob.position()).normalize().scale(1.1D);
            mob.setDeltaMovement(delta.x, 0.2D, delta.z);
            mob.hurtMarked = true;
            target.hurt(mob.damageSources().mobAttack(mob), 6.0F);
        }

        private void dive(ServerLevel level) {
            mob.teleportTo(target.getX(), target.getY(), target.getZ());
            pulse(level, 2.5D, 3.5F);
        }

        private void pulse(ServerLevel level, double radius, float damage) {
            AABB box = mob.getBoundingBox().inflate(radius);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, candidate ->
                    candidate.isAlive() && !(candidate instanceof Monster))) {
                entity.hurt(mob.damageSources().mobAttack(mob), damage);
            }
            level.sendParticles(ParticleTypes.CRIT, mob.getX(), mob.getY() + 0.2D, mob.getZ(), 18, radius * 0.35D, 0.1D, radius * 0.35D, 0.05D);
        }

        private void pulseSlow(ServerLevel level) {
            AABB box = mob.getBoundingBox().inflate(4.0D);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, candidate ->
                    candidate.isAlive() && !(candidate instanceof Monster))) {
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true, true));
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, true, true));
            }
        }

        private void markTarget(ServerLevel level) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, true, true));
            target.addTag("tbos.index_mark");
            level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + 1.0D, target.getZ(), 10, 0.2D, 0.4D, 0.2D, 0.01D);
        }

        private void dropOnTarget(ServerLevel level) {
            mob.teleportTo(target.getX(), target.getY() + 1.1D, target.getZ());
            target.hurt(mob.damageSources().mobAttack(mob), 5.0F);
        }

        private void biteOnBeat(ServerLevel level) {
            if (mob.tickCount % 20 < 10) {
                target.hurt(mob.damageSources().mobAttack(mob), 6.0F);
            } else {
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 0, false, false, true));
            }
        }

        private void sealNearby(ServerLevel level) {
            BlockPos base = mob.blockPosition().relative(mob.getDirection(), 2).above();
            if (level.getBlockState(base).isAir()) {
                level.setBlock(base, ModBlocks.ARCHIVE_SEAL.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                mob.sealPos = base.immutable();
                mob.sealTicks = 60;
            }
            pulse(level, 2.0D, 2.0F);
        }

        private void weaken(ServerLevel level) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, true, true));
            level.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY() + 1.0D, target.getZ(), 8, 0.2D, 0.3D, 0.2D, 0.01D);
        }

        private void sweep(ServerLevel level) {
            AABB box = mob.getBoundingBox().inflate(5.0D, 1.0D, 5.0D);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, candidate ->
                    candidate.isAlive() && !(candidate instanceof Monster))) {
                entity.hurt(mob.damageSources().mobAttack(mob), 5.5F);
                VanillaCompat.knockback(
                        entity,
                        0.7D,
                        mob.getX() - entity.getX(),
                        mob.getZ() - entity.getZ(),
                        mob.damageSources().mobAttack(mob),
                        5.5F);
            }
        }
    }

    /** Breaks the ModEntities compile cycle while ash wisps reuse this class. */
    private static final class ModEntitiesHolder {
        private static ThemeExclusiveEntity createAshChorister(ServerLevel level) {
            return com.nightbeam.tbos.registry.ModEntities.ASH_CHORISTER.get().create(level);
        }
    }
}
