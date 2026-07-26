package com.nightbeam.tbos.entity;

import com.nightbeam.tbos.registry.ModSounds;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;

/**
 * The Archive run boss. It conducts a four-beat refrain that damages and slows
 * everyone still inside the hall, and conducts it faster once wounded.
 */
public final class HourCantorEntity extends Monster {
    private static final EntityDataAccessor<Byte> REFRAIN_PHASE =
            SynchedEntityData.defineId(HourCantorEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> REFRAIN_PHASE_TICKS =
            SynchedEntityData.defineId(HourCantorEntity.class, EntityDataSerializers.INT);

    /** Reach of the refrain, matching the twelve-block hall it is fought in. */
    public static final double REFRAIN_RADIUS = 12.0D;

    /** Below this fraction of maximum health the refrain's intone shortens. */
    public static final float ESCALATION_THRESHOLD = 0.5F;

    private static final double REFRAIN_RADIUS_SQR = REFRAIN_RADIUS * REFRAIN_RADIUS;
    private static final int INITIAL_REFRAIN_COOLDOWN = 60;
    private static final int REFRAIN_COOLDOWN = 80;
    private static final int ABORTED_REFRAIN_COOLDOWN = 40;
    private static final int ESCALATED_INTONE_TICKS = 18;
    private static final float REFRAIN_DAMAGE = 2.5F;
    private static final int REFRAIN_SLOW_TICKS = 50;
    private static final int REFRAIN_SLOW_AMPLIFIER = 1;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            getUUID(),
            Component.translatable("entity.tbos.hour_cantor"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_10);

    private int refrainCooldown = INITIAL_REFRAIN_COOLDOWN;

    public HourCantorEntity(EntityType<? extends HourCantorEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 50;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new RefrainGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, false));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(REFRAIN_PHASE, (byte) RefrainPhase.IDLE.ordinal());
        entityData.define(REFRAIN_PHASE_TICKS, 0);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (getRefrainPhase() == RefrainPhase.IDLE && refrainCooldown > 0) {
            refrainCooldown--;
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        // An interrupted refrain must restart rather than resolve on load.
        setRefrainPhase(RefrainPhase.IDLE);
        refrainCooldown = INITIAL_REFRAIN_COOLDOWN;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    public RefrainPhase getRefrainPhase() {
        int ordinal = entityData.get(REFRAIN_PHASE);
        RefrainPhase[] phases = RefrainPhase.values();
        return phases[Mth.clamp(ordinal, 0, phases.length - 1)];
    }

    public float getRefrainProgress(float partialTick) {
        RefrainPhase phase = getRefrainPhase();
        if (phase == RefrainPhase.IDLE) {
            return 0.0F;
        }
        return Mth.clamp((entityData.get(REFRAIN_PHASE_TICKS) + partialTick) / phaseDuration(phase), 0.0F, 1.0F);
    }

    public int getRefrainCooldown() {
        return refrainCooldown;
    }

    /** True once the Cantor is wounded enough to tighten its cadence. */
    public boolean isEscalated() {
        return getHealth() <= getMaxHealth() * ESCALATION_THRESHOLD;
    }

    /** The intone shortens when wounded; every other phase keeps its length. */
    private int phaseDuration(RefrainPhase phase) {
        if (phase == RefrainPhase.INTONE && isEscalated()) {
            return ESCALATED_INTONE_TICKS;
        }
        return phase.duration();
    }

    private void setRefrainPhase(RefrainPhase phase) {
        entityData.set(REFRAIN_PHASE, (byte) phase.ordinal());
        entityData.set(REFRAIN_PHASE_TICKS, 0);
    }

    private int advanceRefrainPhase() {
        int ticks = entityData.get(REFRAIN_PHASE_TICKS) + 1;
        entityData.set(REFRAIN_PHASE_TICKS, ticks);
        return ticks;
    }

    public enum RefrainPhase {
        IDLE(1),
        INTONE(30),
        REFRAIN(10),
        REST(20);

        private final int duration;

        RefrainPhase(int duration) {
            this.duration = duration;
        }

        public int duration() {
            return duration;
        }
    }

    /**
     * Conducts the four-beat refrain. The intone is the whole telegraph; the
     * damage and slow land on the first tick of the release.
     */
    private static final class RefrainGoal extends Goal {
        private final HourCantorEntity cantor;
        private LivingEntity target;

        private RefrainGoal(HourCantorEntity cantor) {
            this.cantor = cantor;
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity candidate = cantor.getTarget();
            if (candidate == null
                    || !candidate.isAlive()
                    || cantor.refrainCooldown > 0
                    || cantor.getRefrainPhase() != RefrainPhase.IDLE) {
                return false;
            }
            return cantor.distanceToSqr(candidate) <= REFRAIN_RADIUS_SQR;
        }

        @Override
        public boolean canContinueToUse() {
            return cantor.getRefrainPhase() != RefrainPhase.IDLE;
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
            target = cantor.getTarget();
            cantor.setRefrainPhase(RefrainPhase.INTONE);
            cantor.playSound(ModSounds.HOUR_CANTOR_INTONE.get(), 1.2F, 1.0F);
        }

        @Override
        public void stop() {
            if (cantor.getRefrainPhase() != RefrainPhase.IDLE) {
                cantor.setRefrainPhase(RefrainPhase.IDLE);
                cantor.refrainCooldown = Math.max(cantor.refrainCooldown, ABORTED_REFRAIN_COOLDOWN);
            }
            target = null;
        }

        @Override
        public void tick() {
            ServerLevel level = getServerLevel(cantor);
            switch (cantor.getRefrainPhase()) {
                case IDLE -> {
                }
                case INTONE -> tickIntone(level);
                case REFRAIN -> tickRefrain(level);
                case REST -> tickRest();
            }
        }

        private void tickIntone(ServerLevel level) {
            if (target == null || !target.isAlive()) {
                cantor.setRefrainPhase(RefrainPhase.IDLE);
                cantor.refrainCooldown = ABORTED_REFRAIN_COOLDOWN;
                return;
            }
            cantor.getLookControl().setLookAt(target, 45.0F, 45.0F);
            int ticks = cantor.advanceRefrainPhase();
            if (ticks % 5 == 1) {
                level.sendParticles(
                        ParticleTypes.ENCHANT,
                        cantor.getX(),
                        cantor.getY() + cantor.getBbHeight() * 0.7D,
                        cantor.getZ(),
                        10,
                        0.7D,
                        0.5D,
                        0.7D,
                        0.02D);
            }
            if (ticks >= cantor.phaseDuration(RefrainPhase.INTONE)) {
                cantor.setRefrainPhase(RefrainPhase.REFRAIN);
                cantor.playSound(ModSounds.HOUR_CANTOR_REFRAIN.get(), 1.4F, 1.0F);
            }
        }

        private void tickRefrain(ServerLevel level) {
            int ticks = cantor.advanceRefrainPhase();
            if (ticks == 1) {
                release(level);
            }
            if (ticks >= RefrainPhase.REFRAIN.duration()) {
                cantor.setRefrainPhase(RefrainPhase.REST);
            }
        }

        private void release(ServerLevel level) {
            Vec3 origin = cantor.position().add(0.0D, cantor.getBbHeight() * 0.6D, 0.0D);
            level.sendParticles(
                    ParticleTypes.SONIC_BOOM, origin.x, origin.y, origin.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            // Monsters are exempt so the refrain never slows the Cantor's own hall.
            List<LivingEntity> audience = level.getEntitiesOfClass(
                    LivingEntity.class,
                    cantor.getBoundingBox().inflate(REFRAIN_RADIUS),
                    victim -> victim.isAlive()
                            && !(victim instanceof Monster)
                            && !victim.isSpectator()
                            && cantor.distanceToSqr(victim) <= REFRAIN_RADIUS_SQR);
            for (LivingEntity victim : audience) {
                Vec3 chest = victim.position().add(0.0D, victim.getBbHeight() * 0.5D, 0.0D);
                particleLine(level, origin, chest);
                victim.hurtServer(
                        level,
                        cantor.damageSources().indirectMagic(cantor, cantor),
                        REFRAIN_DAMAGE);
                victim.addEffect(new MobEffectInstance(
                        MobEffects.SLOWNESS, REFRAIN_SLOW_TICKS, REFRAIN_SLOW_AMPLIFIER));
            }
        }

        private void particleLine(ServerLevel level, Vec3 from, Vec3 to) {
            Vec3 span = to.subtract(from);
            int steps = Math.max(1, (int) (span.length() * 2.0D));
            for (int step = 0; step <= steps; step++) {
                Vec3 point = from.add(span.scale((double) step / steps));
                level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }

        private void tickRest() {
            if (cantor.advanceRefrainPhase() >= RefrainPhase.REST.duration()) {
                cantor.setRefrainPhase(RefrainPhase.IDLE);
                cantor.refrainCooldown = REFRAIN_COOLDOWN;
            }
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // The Cantor hovers above the floor it conducts over.
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.HOUR_CANTOR_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.HOUR_CANTOR_HURT.get();
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }
}
