package com.nightbeam.tbos.entity;

import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;

/**
 * An elite Archive parasite that telegraphs a lunging bite, weakens its
 * victim, and converts stolen recollection into health.
 */
public final class MemoryLeechEntity extends Monster {
    private static final EntityDataAccessor<Byte> POUNCE_PHASE =
            SynchedEntityData.defineId(MemoryLeechEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> POUNCE_PHASE_TICKS =
            SynchedEntityData.defineId(MemoryLeechEntity.class, EntityDataSerializers.INT);

    private static final int INITIAL_POUNCE_COOLDOWN = 40;
    private static final int POUNCE_COOLDOWN = 80;
    private static final double MIN_POUNCE_DISTANCE_SQR = 16.0D;
    private static final double MAX_POUNCE_DISTANCE_SQR = 81.0D;

    private int pounceCooldown = INITIAL_POUNCE_COOLDOWN;

    public MemoryLeechEntity(EntityType<? extends MemoryLeechEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 8;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new SiphoningPounceGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1D, false));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(POUNCE_PHASE, (byte) PouncePhase.IDLE.ordinal());
        entityData.define(POUNCE_PHASE_TICKS, 0);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (getPouncePhase() == PouncePhase.IDLE && pounceCooldown > 0) {
            pounceCooldown--;
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        // A partially completed leap is unsafe to resume after a server reload.
        setPouncePhase(PouncePhase.IDLE);
        pounceCooldown = INITIAL_POUNCE_COOLDOWN;
    }

    public PouncePhase getPouncePhase() {
        int ordinal = entityData.get(POUNCE_PHASE);
        PouncePhase[] phases = PouncePhase.values();
        return phases[Mth.clamp(ordinal, 0, phases.length - 1)];
    }

    public float getPounceProgress(float partialTick) {
        PouncePhase phase = getPouncePhase();
        if (phase == PouncePhase.IDLE) {
            return 0.0F;
        }
        return Mth.clamp((entityData.get(POUNCE_PHASE_TICKS) + partialTick) / phase.duration(), 0.0F, 1.0F);
    }

    public int getPounceCooldown() {
        return pounceCooldown;
    }

    private void setPouncePhase(PouncePhase phase) {
        entityData.set(POUNCE_PHASE, (byte) phase.ordinal());
        entityData.set(POUNCE_PHASE_TICKS, 0);
    }

    private int advancePouncePhase() {
        int ticks = entityData.get(POUNCE_PHASE_TICKS) + 1;
        entityData.set(POUNCE_PHASE_TICKS, ticks);
        return ticks;
    }

    public enum PouncePhase {
        IDLE(1),
        WINDUP(12),
        AIRBORNE(16),
        RECOVERY(8);

        private final int duration;

        PouncePhase(int duration) {
            this.duration = duration;
        }

        public int duration() {
            return duration;
        }
    }

    private static final class SiphoningPounceGoal extends Goal {
        private final MemoryLeechEntity leech;
        private LivingEntity target;
        private boolean connected;

        private SiphoningPounceGoal(MemoryLeechEntity leech) {
            this.leech = leech;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity candidate = leech.getTarget();
            if (candidate == null
                    || !candidate.isAlive()
                    || leech.pounceCooldown > 0
                    || leech.getPouncePhase() != PouncePhase.IDLE
                    || !leech.onGround()
                    || !leech.getSensing().hasLineOfSight(candidate)) {
                return false;
            }
            double distance = leech.distanceToSqr(candidate);
            return distance >= MIN_POUNCE_DISTANCE_SQR && distance <= MAX_POUNCE_DISTANCE_SQR;
        }

        @Override
        public boolean canContinueToUse() {
            return leech.getPouncePhase() != PouncePhase.IDLE;
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
            target = leech.getTarget();
            connected = false;
            leech.getNavigation().stop();
            leech.setPouncePhase(PouncePhase.WINDUP);
            leech.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 1.55F);
        }

        @Override
        public void stop() {
            if (leech.getPouncePhase() != PouncePhase.IDLE) {
                leech.setPouncePhase(PouncePhase.IDLE);
                leech.pounceCooldown = Math.max(leech.pounceCooldown, INITIAL_POUNCE_COOLDOWN);
            }
            target = null;
            connected = false;
        }

        @Override
        public void tick() {
            ServerLevel level = getServerLevel(leech);
            switch (leech.getPouncePhase()) {
                case IDLE -> {
                }
                case WINDUP -> tickWindup(level);
                case AIRBORNE -> tickAirborne(level);
                case RECOVERY -> tickRecovery();
            }
        }

        private void tickWindup(ServerLevel level) {
            if (target == null
                    || !target.isAlive()
                    || !leech.getSensing().hasLineOfSight(target)) {
                leech.setPouncePhase(PouncePhase.IDLE);
                leech.pounceCooldown = INITIAL_POUNCE_COOLDOWN;
                return;
            }
            leech.getNavigation().stop();
            leech.getLookControl().setLookAt(target, 35.0F, 35.0F);
            int ticks = leech.advancePouncePhase();
            if (ticks == 1 || ticks == 4 || ticks == 7 || ticks == 10) {
                level.sendParticles(
                        ParticleTypes.WITCH,
                        leech.getX(),
                        leech.getY() + leech.getBbHeight() * 0.55D,
                        leech.getZ(),
                        5,
                        0.35D,
                        0.18D,
                        0.35D,
                        0.01D);
            }
            if (ticks < PouncePhase.WINDUP.duration()) {
                return;
            }

            Vec3 direction = target.position().subtract(leech.position());
            Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
            if (horizontal.lengthSqr() < 1.0E-5D) {
                leech.setPouncePhase(PouncePhase.IDLE);
                leech.pounceCooldown = INITIAL_POUNCE_COOLDOWN;
                return;
            }
            horizontal = horizontal.normalize();
            leech.setDeltaMovement(horizontal.x * 0.9D, 0.32D, horizontal.z * 0.9D);
            leech.setPouncePhase(PouncePhase.AIRBORNE);
            leech.playSound(SoundEvents.AMETHYST_CLUSTER_HIT, 1.0F, 0.75F);
        }

        private void tickAirborne(ServerLevel level) {
            int ticks = leech.advancePouncePhase();
            if (!connected
                    && target != null
                    && target.isAlive()
                    && isPounceContact(target)) {
                leech.swing(InteractionHand.MAIN_HAND);
                if (leech.doHurtTarget(level, target)) {
                    connected = true;
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                    leech.heal(4.0F);
                    level.sendParticles(
                            ParticleTypes.ENCHANT,
                            target.getX(),
                            target.getY() + target.getBbHeight() * 0.5D,
                            target.getZ(),
                            18,
                            target.getBbWidth() * 0.35D,
                            target.getBbHeight() * 0.25D,
                            target.getBbWidth() * 0.35D,
                            0.02D);
                }
            }

            if (connected || ticks >= PouncePhase.AIRBORNE.duration() || (ticks > 2 && leech.onGround())) {
                Vec3 movement = leech.getDeltaMovement();
                leech.setDeltaMovement(movement.x * 0.55D, movement.y, movement.z * 0.55D);
                leech.setPouncePhase(PouncePhase.RECOVERY);
            }
        }

        private boolean isPounceContact(LivingEntity target) {
            return leech.isWithinMeleeAttackRange(target)
                    || leech.getBoundingBox().inflate(0.35D).intersects(target.getBoundingBox())
                    || leech.distanceToSqr(target) <= 2.25D;
        }

        private void tickRecovery() {
            leech.getNavigation().stop();
            if (leech.advancePouncePhase() >= PouncePhase.RECOVERY.duration()) {
                leech.setPouncePhase(PouncePhase.IDLE);
                leech.pounceCooldown = POUNCE_COOLDOWN;
            }
        }
    }
}
