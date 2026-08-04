package com.nightbeam.tbos.entity;

import net.minecraft.nbt.CompoundTag;

import com.nightbeam.tbos.registry.ModSounds;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.phys.Vec3;

/**
 * A heavy Archive defender used for guardian and puzzle-wave encounters. It
 * raises both mauls overhead and drives them down, throwing everyone nearby
 * clear of the impact.
 */
public final class MeridianSentinelEntity extends Monster {
    private static final EntityDataAccessor<Byte> SLAM_PHASE =
            SynchedEntityData.defineId(MeridianSentinelEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> SLAM_PHASE_TICKS =
            SynchedEntityData.defineId(MeridianSentinelEntity.class, EntityDataSerializers.INT);

    /** Radius of the ground wave, matching the six-block reach it replaced. */
    public static final double SLAM_RADIUS = 6.0D;

    private static final double SLAM_RADIUS_SQR = SLAM_RADIUS * SLAM_RADIUS;
    private static final int INITIAL_SLAM_COOLDOWN = 50;
    private static final int SLAM_COOLDOWN = 110;
    private static final int ABORTED_SLAM_COOLDOWN = 40;
    private static final float SLAM_DAMAGE = 3.0F;
    private static final double SLAM_PUSH_HORIZONTAL = 0.65D;
    private static final double SLAM_PUSH_VERTICAL = 0.32D;

    private int slamCooldown = INITIAL_SLAM_COOLDOWN;

    public MeridianSentinelEntity(EntityType<? extends MeridianSentinelEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 12;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new MeridianSlamGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, false));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.75D));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SLAM_PHASE, (byte) SlamPhase.IDLE.ordinal());
        this.entityData.define(SLAM_PHASE_TICKS, 0);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (getSlamPhase() == SlamPhase.IDLE && slamCooldown > 0) {
            slamCooldown--;
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag input) {
        super.readAdditionalSaveData(input);
        // A raised maul must not stay raised across a server reload.
        setSlamPhase(SlamPhase.IDLE);
        slamCooldown = INITIAL_SLAM_COOLDOWN;
    }

    public SlamPhase getSlamPhase() {
        int ordinal = entityData.get(SLAM_PHASE);
        SlamPhase[] phases = SlamPhase.values();
        return phases[Mth.clamp(ordinal, 0, phases.length - 1)];
    }

    public float getSlamProgress(float partialTick) {
        SlamPhase phase = getSlamPhase();
        if (phase == SlamPhase.IDLE) {
            return 0.0F;
        }
        return Mth.clamp((entityData.get(SLAM_PHASE_TICKS) + partialTick) / phase.duration(), 0.0F, 1.0F);
    }

    public int getSlamCooldown() {
        return slamCooldown;
    }

    private void setSlamPhase(SlamPhase phase) {
        entityData.set(SLAM_PHASE, (byte) phase.ordinal());
        entityData.set(SLAM_PHASE_TICKS, 0);
    }

    private int advanceSlamPhase() {
        int ticks = entityData.get(SLAM_PHASE_TICKS) + 1;
        entityData.set(SLAM_PHASE_TICKS, ticks);
        return ticks;
    }

    public enum SlamPhase {
        IDLE(1),
        RAISE(18),
        SLAM(4),
        SETTLE(12);

        private final int duration;

        SlamPhase(int duration) {
            this.duration = duration;
        }

        public int duration() {
            return duration;
        }
    }

    /**
     * Holds both mauls overhead for the full raise before releasing a single
     * radial wave, so the telegraph always precedes the damage.
     */
    private static final class MeridianSlamGoal extends Goal {
        private final MeridianSentinelEntity sentinel;
        private LivingEntity target;

        private MeridianSlamGoal(MeridianSentinelEntity sentinel) {
            this.sentinel = sentinel;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity candidate = sentinel.getTarget();
            if (candidate == null
                    || !candidate.isAlive()
                    || sentinel.slamCooldown > 0
                    || sentinel.getSlamPhase() != SlamPhase.IDLE
                    || !sentinel.getSensing().hasLineOfSight(candidate)) {
                return false;
            }
            return sentinel.distanceToSqr(candidate) <= SLAM_RADIUS_SQR;
        }

        @Override
        public boolean canContinueToUse() {
            return sentinel.getSlamPhase() != SlamPhase.IDLE;
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
            target = sentinel.getTarget();
            sentinel.getNavigation().stop();
            sentinel.setSlamPhase(SlamPhase.RAISE);
            sentinel.playSound(ModSounds.MERIDIAN_SENTINEL_WIND_UP.get(), 1.0F, 1.0F);
        }

        @Override
        public void stop() {
            if (sentinel.getSlamPhase() != SlamPhase.IDLE) {
                sentinel.setSlamPhase(SlamPhase.IDLE);
                sentinel.slamCooldown = Math.max(sentinel.slamCooldown, ABORTED_SLAM_COOLDOWN);
            }
            target = null;
        }

        @Override
        public void tick() {
            if (!(sentinel.level() instanceof ServerLevel level)) {
                return;
            }
            switch (sentinel.getSlamPhase()) {
                case IDLE -> {
                }
                case RAISE -> tickRaise(level);
                case SLAM -> tickSlam(level);
                case SETTLE -> tickSettle();
            }
        }

        private void tickRaise(ServerLevel level) {
            if (target == null || !target.isAlive()) {
                sentinel.setSlamPhase(SlamPhase.IDLE);
                sentinel.slamCooldown = ABORTED_SLAM_COOLDOWN;
                return;
            }
            sentinel.getNavigation().stop();
            sentinel.getLookControl().setLookAt(target, 30.0F, 30.0F);
            int ticks = sentinel.advanceSlamPhase();
            if (ticks % 6 == 1) {
                level.sendParticles(
                        ParticleTypes.SOUL,
                        sentinel.getX(),
                        sentinel.getY() + 0.2D,
                        sentinel.getZ(),
                        12,
                        1.2D,
                        0.1D,
                        1.2D,
                        0.01D);
            }
            if (ticks >= SlamPhase.RAISE.duration()) {
                sentinel.setSlamPhase(SlamPhase.SLAM);
            }
        }

        private void tickSlam(ServerLevel level) {
            int ticks = sentinel.advanceSlamPhase();
            if (ticks == 1) {
                release(level);
            }
            if (ticks >= SlamPhase.SLAM.duration()) {
                sentinel.setSlamPhase(SlamPhase.SETTLE);
            }
        }

        private void release(ServerLevel level) {
            sentinel.playSound(ModSounds.MERIDIAN_SENTINEL_SLAM.get(), 1.1F, 1.0F);
            level.sendParticles(
                    ParticleTypes.SONIC_BOOM, sentinel.getX(), sentinel.getY() + 0.25D, sentinel.getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            // Monsters are exempt so a wave of Archive enemies cannot shatter
            // its own formation; everything else standing on the floor is fair.
            List<LivingEntity> caught = level.getEntitiesOfClass(
                    LivingEntity.class,
                    sentinel.getBoundingBox().inflate(SLAM_RADIUS),
                    victim -> victim.isAlive()
                            && !(victim instanceof Monster)
                            && !victim.isSpectator()
                            && sentinel.distanceToSqr(victim) <= SLAM_RADIUS_SQR);
            for (LivingEntity victim : caught) {
                Vec3 push = victim.position().subtract(sentinel.position());
                double horizontal = Math.max(0.1D, Math.sqrt(push.x * push.x + push.z * push.z));
                victim.push(
                        push.x / horizontal * SLAM_PUSH_HORIZONTAL,
                        SLAM_PUSH_VERTICAL,
                        push.z / horizontal * SLAM_PUSH_HORIZONTAL);
                victim.hurt(sentinel.damageSources().indirectMagic(sentinel, sentinel),
                        SLAM_DAMAGE);
            }
        }

        private void tickSettle() {
            if (sentinel.advanceSlamPhase() >= SlamPhase.SETTLE.duration()) {
                sentinel.setSlamPhase(SlamPhase.IDLE);
                sentinel.slamCooldown = SLAM_COOLDOWN;
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.MERIDIAN_SENTINEL_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.MERIDIAN_SENTINEL_HURT.get();
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }
}
