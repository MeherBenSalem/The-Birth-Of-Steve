package com.nightbeam.tbos.entity;

import com.nightbeam.tbos.registry.ModSounds;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;

/**
 * A quick Archive hunter whose shard shell lags its own motion. It fractures
 * apart, reassembles behind its quarry, and closes in melee.
 */
public final class ParallaxWraithEntity extends Monster {
    private static final EntityDataAccessor<Byte> DISPLACE_PHASE =
            SynchedEntityData.defineId(ParallaxWraithEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DISPLACE_PHASE_TICKS =
            SynchedEntityData.defineId(ParallaxWraithEntity.class, EntityDataSerializers.INT);

    private static final int INITIAL_DISPLACE_COOLDOWN = 60;
    private static final int DISPLACE_COOLDOWN = 125;
    private static final int ABORTED_DISPLACE_COOLDOWN = 40;
    private static final double MIN_DISPLACE_DISTANCE_SQR = 9.0D;
    private static final double MAX_DISPLACE_DISTANCE_SQR = 400.0D;
    private static final double LANDING_OFFSET = 2.5D;

    private int displaceCooldown = INITIAL_DISPLACE_COOLDOWN;

    public ParallaxWraithEntity(EntityType<? extends ParallaxWraithEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 7;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new ParallaxDisplacementGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15D, false));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DISPLACE_PHASE, (byte) DisplacePhase.IDLE.ordinal());
        entityData.define(DISPLACE_PHASE_TICKS, 0);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (getDisplacePhase() == DisplacePhase.IDLE && displaceCooldown > 0) {
            displaceCooldown--;
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        // A half-finished displacement is unsafe to resume after a server reload.
        setDisplacePhase(DisplacePhase.IDLE);
        displaceCooldown = INITIAL_DISPLACE_COOLDOWN;
    }

    public DisplacePhase getDisplacePhase() {
        int ordinal = entityData.get(DISPLACE_PHASE);
        DisplacePhase[] phases = DisplacePhase.values();
        return phases[Mth.clamp(ordinal, 0, phases.length - 1)];
    }

    public float getDisplaceProgress(float partialTick) {
        DisplacePhase phase = getDisplacePhase();
        if (phase == DisplacePhase.IDLE) {
            return 0.0F;
        }
        return Mth.clamp((entityData.get(DISPLACE_PHASE_TICKS) + partialTick) / phase.duration(), 0.0F, 1.0F);
    }

    public int getDisplaceCooldown() {
        return displaceCooldown;
    }

    private void setDisplacePhase(DisplacePhase phase) {
        entityData.set(DISPLACE_PHASE, (byte) phase.ordinal());
        entityData.set(DISPLACE_PHASE_TICKS, 0);
    }

    private int advanceDisplacePhase() {
        int ticks = entityData.get(DISPLACE_PHASE_TICKS) + 1;
        entityData.set(DISPLACE_PHASE_TICKS, ticks);
        return ticks;
    }

    public enum DisplacePhase {
        IDLE(1),
        FRACTURE(10),
        DISPLACED(6),
        REFORM(8);

        private final int duration;

        DisplacePhase(int duration) {
            this.duration = duration;
        }

        public int duration() {
            return duration;
        }
    }

    /**
     * Telegraphs a shard fracture, then reassembles the wraith behind its
     * target. The move never damages; it only buys a flanking position.
     */
    private static final class ParallaxDisplacementGoal extends Goal {
        private final ParallaxWraithEntity wraith;
        private LivingEntity target;

        private ParallaxDisplacementGoal(ParallaxWraithEntity wraith) {
            this.wraith = wraith;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity candidate = wraith.getTarget();
            if (candidate == null
                    || !candidate.isAlive()
                    || wraith.displaceCooldown > 0
                    || wraith.getDisplacePhase() != DisplacePhase.IDLE
                    || !wraith.getSensing().hasLineOfSight(candidate)) {
                return false;
            }
            double distance = wraith.distanceToSqr(candidate);
            return distance > MIN_DISPLACE_DISTANCE_SQR && distance <= MAX_DISPLACE_DISTANCE_SQR;
        }

        @Override
        public boolean canContinueToUse() {
            return wraith.getDisplacePhase() != DisplacePhase.IDLE;
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
            target = wraith.getTarget();
            wraith.getNavigation().stop();
            wraith.setDisplacePhase(DisplacePhase.FRACTURE);
            wraith.playSound(ModSounds.PARALLAX_WRAITH_FRACTURE.get(), 0.9F, 1.0F);
        }

        @Override
        public void stop() {
            if (wraith.getDisplacePhase() != DisplacePhase.IDLE) {
                wraith.setDisplacePhase(DisplacePhase.IDLE);
                wraith.displaceCooldown = Math.max(wraith.displaceCooldown, ABORTED_DISPLACE_COOLDOWN);
            }
            target = null;
        }

        @Override
        public void tick() {
            ServerLevel level = getServerLevel(wraith);
            switch (wraith.getDisplacePhase()) {
                case IDLE -> {
                }
                case FRACTURE -> tickFracture(level);
                case DISPLACED -> tickDisplaced(level);
                case REFORM -> tickReform();
            }
        }

        private void abort() {
            wraith.setDisplacePhase(DisplacePhase.IDLE);
            wraith.displaceCooldown = ABORTED_DISPLACE_COOLDOWN;
        }

        private void tickFracture(ServerLevel level) {
            if (target == null
                    || !target.isAlive()
                    || !wraith.getSensing().hasLineOfSight(target)) {
                abort();
                return;
            }
            wraith.getNavigation().stop();
            wraith.getLookControl().setLookAt(target, 40.0F, 40.0F);
            int ticks = wraith.advanceDisplacePhase();
            if (ticks % 3 == 1) {
                level.sendParticles(
                        ParticleTypes.REVERSE_PORTAL,
                        wraith.getX(),
                        wraith.getY() + wraith.getBbHeight() * 0.55D,
                        wraith.getZ(),
                        6,
                        0.35D,
                        0.55D,
                        0.35D,
                        0.02D);
            }
            if (ticks >= DisplacePhase.FRACTURE.duration()) {
                wraith.setDisplacePhase(DisplacePhase.DISPLACED);
            }
        }

        private void tickDisplaced(ServerLevel level) {
            wraith.getNavigation().stop();
            int ticks = wraith.advanceDisplacePhase();
            if (ticks < DisplacePhase.DISPLACED.duration()) {
                return;
            }
            if (target == null || !target.isAlive()) {
                abort();
                return;
            }

            BlockPos destination = flankingPosition(level, target);
            if (destination == null) {
                abort();
                return;
            }
            Vec3 origin = wraith.position();
            wraith.teleportTo(destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D);
            level.sendParticles(
                    ParticleTypes.PORTAL, origin.x, origin.y + 0.6D, origin.z, 20, 0.35D, 0.45D, 0.35D, 0.05D);
            level.sendParticles(
                    ParticleTypes.REVERSE_PORTAL,
                    wraith.getX(),
                    wraith.getY() + wraith.getBbHeight() * 0.5D,
                    wraith.getZ(),
                    18,
                    0.3D,
                    0.5D,
                    0.3D,
                    0.03D);
            wraith.playSound(ModSounds.PARALLAX_WRAITH_REFORM.get(), 0.85F, 1.0F);
            wraith.setDisplacePhase(DisplacePhase.REFORM);
        }

        /** The block behind the target, or null when nothing there can hold the wraith. */
        private BlockPos flankingPosition(ServerLevel level, LivingEntity quarry) {
            Vec3 behind = quarry.position()
                    .subtract(quarry.getLookAngle().multiply(LANDING_OFFSET, 0.0D, LANDING_OFFSET));
            BlockPos destination = BlockPos.containing(behind.x, quarry.getY(), behind.z);
            boolean standable = level.getBlockState(destination).isAir()
                    && level.getBlockState(destination.above()).isAir()
                    && !level.getBlockState(destination.below()).isAir();
            return standable ? destination : null;
        }

        private void tickReform() {
            wraith.getNavigation().stop();
            if (wraith.advanceDisplacePhase() >= DisplacePhase.REFORM.duration()) {
                wraith.setDisplacePhase(DisplacePhase.IDLE);
                wraith.displaceCooldown = DISPLACE_COOLDOWN;
            }
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // The wraith glides; it has no feet to land.
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.PARALLAX_WRAITH_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.PARALLAX_WRAITH_HURT.get();
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }
}
