package com.nightbeam.tbos.entity;

import net.minecraft.nbt.CompoundTag;

import com.nightbeam.tbos.registry.ModSounds;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class LenswardEntity extends Monster {
    private static final EntityDataAccessor<Byte> BEAM_PHASE =
            SynchedEntityData.defineId(LenswardEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> BEAM_PHASE_TICKS =
            SynchedEntityData.defineId(LenswardEntity.class, EntityDataSerializers.INT);

    public static final double LEASH_RADIUS = 10.0D;

    private static final double LEASH_RADIUS_SQR = LEASH_RADIUS * LEASH_RADIUS;
    private static final double PREFERRED_RANGE = 6.0D;
    private static final int INITIAL_BEAM_COOLDOWN = 60;
    private static final int BEAM_COOLDOWN = 100;
    private static final int ABORTED_BEAM_COOLDOWN = 40;
    private static final double MIN_BEAM_DISTANCE_SQR = 4.0D;
    private static final float BEAM_DAMAGE = 6.0F;

    private BlockPos anchor;
    private int beamCooldown = INITIAL_BEAM_COOLDOWN;

    public LenswardEntity(EntityType<? extends LenswardEntity> entityType, Level level) {
        super(entityType, level);
        moveControl = new HoverMoveControl(this);
        setNoGravity(true);
        this.xpReward = 10;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FocusedBeamGoal(this));
        goalSelector.addGoal(2, new GuardPostGoal(this));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, Player.class, 10, true, false, this::withinWard));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(BEAM_PHASE, (byte) BeamPhase.IDLE.ordinal());
        this.entityData.define(BEAM_PHASE_TICKS, 0);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (anchor == null) {
            anchor = blockPosition();
        }
        if (getBeamPhase() == BeamPhase.IDLE && beamCooldown > 0) {
            beamCooldown--;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag output) {
        super.addAdditionalSaveData(output);
        if (anchor != null) {
            output.putLong("anchor", anchor.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag input) {
        super.readAdditionalSaveData(input);
        anchor = input.contains("anchor") ? BlockPos.of(input.getLong("anchor")) : null;
        setBeamPhase(BeamPhase.IDLE);
        beamCooldown = INITIAL_BEAM_COOLDOWN;
    }

    public BlockPos anchor() {
        return anchor == null ? blockPosition() : anchor;
    }

    public void setAnchor(BlockPos value) {
        anchor = value;
    }

    public boolean withinWard(LivingEntity candidate) {
        return candidate.distanceToSqr(Vec3.atCenterOf(anchor())) <= LEASH_RADIUS_SQR;
    }

    public BeamPhase getBeamPhase() {
        int ordinal = entityData.get(BEAM_PHASE);
        BeamPhase[] phases = BeamPhase.values();
        return phases[Mth.clamp(ordinal, 0, phases.length - 1)];
    }

    public float getBeamProgress(float partialTick) {
        BeamPhase phase = getBeamPhase();
        if (phase == BeamPhase.IDLE) {
            return 0.0F;
        }
        return Mth.clamp((entityData.get(BEAM_PHASE_TICKS) + partialTick) / phase.duration(), 0.0F, 1.0F);
    }

    public int getBeamCooldown() {
        return beamCooldown;
    }

    private void setBeamPhase(BeamPhase phase) {
        entityData.set(BEAM_PHASE, (byte) phase.ordinal());
        entityData.set(BEAM_PHASE_TICKS, 0);
    }

    private int advanceBeamPhase() {
        int ticks = entityData.get(BEAM_PHASE_TICKS) + 1;
        entityData.set(BEAM_PHASE_TICKS, ticks);
        return ticks;
    }

    private Vec3 lensPosition() {
        return new Vec3(getX(), getY() + getBbHeight() * 0.6D, getZ());
    }

    public enum BeamPhase {
        IDLE(1),
        CHARGING(30),
        FIRING(12),
        RECOVERY(10);

        private final int duration;

        BeamPhase(int duration) {
            this.duration = duration;
        }

        public int duration() {
            return duration;
        }
    }

    private static final class HoverMoveControl extends MoveControl {
        private final LenswardEntity lensward;

        private HoverMoveControl(LenswardEntity lensward) {
            super(lensward);
            this.lensward = lensward;
        }

        @Override
        public void tick() {
            if (operation != Operation.MOVE_TO) {
                return;
            }
            Vec3 delta = new Vec3(
                    wantedX - lensward.getX(),
                    wantedY - lensward.getY(),
                    wantedZ - lensward.getZ());
            double length = delta.length();
            if (length < lensward.getBoundingBox().getSize()) {
                operation = Operation.WAIT;
                lensward.setDeltaMovement(lensward.getDeltaMovement().scale(0.5D));
                return;
            }
            lensward.setDeltaMovement(
                    lensward.getDeltaMovement().add(delta.scale(speedModifier * 0.045D / length)));
            LivingEntity target = lensward.getTarget();
            double facingX = target == null ? delta.x : target.getX() - lensward.getX();
            double facingZ = target == null ? delta.z : target.getZ() - lensward.getZ();
            lensward.setYRot(-((float) Mth.atan2(facingX, facingZ)) * (180.0F / (float) Math.PI));
            lensward.yBodyRot = lensward.getYRot();
        }
    }

    private static final class GuardPostGoal extends Goal {
        private final LenswardEntity lensward;

        private GuardPostGoal(LenswardEntity lensward) {
            this.lensward = lensward;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return lensward.getBeamPhase() == BeamPhase.IDLE
                    && !lensward.getMoveControl().hasWanted()
                    && lensward.random.nextInt(reducedTickDelay(8)) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return lensward.getMoveControl().hasWanted()
                    && lensward.getBeamPhase() == BeamPhase.IDLE;
        }

        @Override
        public void start() {
            Vec3 anchorCenter = Vec3.atCenterOf(lensward.anchor());
            LivingEntity target = lensward.getTarget();
            Vec3 destination;
            if (target != null && lensward.withinWard(target)) {
                Vec3 away = lensward.position().subtract(target.position());
                if (away.lengthSqr() < 1.0E-4D) {
                    away = new Vec3(1.0D, 0.0D, 0.0D);
                }
                destination = target.position()
                        .add(away.normalize().scale(PREFERRED_RANGE))
                        .add(0.0D, 1.2D, 0.0D);
                Vec3 fromAnchor = destination.subtract(anchorCenter);
                if (fromAnchor.lengthSqr() > LEASH_RADIUS_SQR) {
                    destination = anchorCenter.add(fromAnchor.normalize().scale(LEASH_RADIUS));
                }
            } else {
                double drift = (lensward.random.nextDouble() - 0.5D) * 2.0D;
                destination = anchorCenter.add(drift, 0.4D + lensward.random.nextDouble() * 0.6D, drift);
            }
            lensward.getMoveControl().setWantedPosition(
                    destination.x, destination.y, destination.z, 0.8D);
        }
    }

    private static final class FocusedBeamGoal extends Goal {
        private final LenswardEntity lensward;
        private LivingEntity target;

        private FocusedBeamGoal(LenswardEntity lensward) {
            this.lensward = lensward;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity candidate = lensward.getTarget();
            if (candidate == null
                    || !candidate.isAlive()
                    || lensward.beamCooldown > 0
                    || lensward.getBeamPhase() != BeamPhase.IDLE
                    || !lensward.withinWard(candidate)
                    || !lensward.getSensing().hasLineOfSight(candidate)) {
                return false;
            }
            return lensward.distanceToSqr(candidate) >= MIN_BEAM_DISTANCE_SQR;
        }

        @Override
        public boolean canContinueToUse() {
            return lensward.getBeamPhase() != BeamPhase.IDLE;
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
            target = lensward.getTarget();
            lensward.getMoveControl().setWantedPosition(
                    lensward.getX(), lensward.getY(), lensward.getZ(), 0.0D);
            lensward.setBeamPhase(BeamPhase.CHARGING);
            lensward.playSound(ModSounds.LENSWARD_CHARGE.get(), 1.0F, 1.0F);
        }

        @Override
        public void stop() {
            if (lensward.getBeamPhase() != BeamPhase.IDLE) {
                lensward.setBeamPhase(BeamPhase.IDLE);
                lensward.beamCooldown = Math.max(lensward.beamCooldown, ABORTED_BEAM_COOLDOWN);
            }
            target = null;
        }

        @Override
        public void tick() {
            if (!(lensward.level() instanceof ServerLevel level)) {
                return;
            }
            switch (lensward.getBeamPhase()) {
                case IDLE -> {
                }
                case CHARGING -> tickCharging(level);
                case FIRING -> tickFiring(level);
                case RECOVERY -> tickRecovery();
            }
        }

        private void abort() {
            lensward.setBeamPhase(BeamPhase.IDLE);
            lensward.beamCooldown = ABORTED_BEAM_COOLDOWN;
        }

        private void tickCharging(ServerLevel level) {
            if (target == null
                    || !target.isAlive()
                    || !lensward.withinWard(target)
                    || !lensward.getSensing().hasLineOfSight(target)) {
                abort();
                return;
            }
            lensward.getLookControl().setLookAt(target, 45.0F, 45.0F);
            int ticks = lensward.advanceBeamPhase();
            if (ticks % 4 == 0) {
                Vec3 lens = lensward.lensPosition();
                level.sendParticles(
                        ParticleTypes.WITCH,
                        lens.x,
                        lens.y,
                        lens.z,
                        4,
                        0.45D,
                        0.45D,
                        0.45D,
                        0.01D);
            }
            if (ticks >= BeamPhase.CHARGING.duration()) {
                lensward.setBeamPhase(BeamPhase.FIRING);
                lensward.playSound(ModSounds.LENSWARD_FIRE.get(), 1.2F, 1.0F);
            }
        }

        private void tickFiring(ServerLevel level) {
            int ticks = lensward.advanceBeamPhase();
            Vec3 lens = lensward.lensPosition();
            if (target != null && target.isAlive()) {
                lensward.getLookControl().setLookAt(target, 45.0F, 45.0F);
                Vec3 aim = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
                Vec3 span = aim.subtract(lens);
                int steps = Math.max(1, (int) (span.length() * 2.0D));
                for (int step = 0; step <= steps; step++) {
                    Vec3 point = lens.add(span.scale((double) step / steps));
                    level.sendParticles(
                            ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
            if (ticks == 1 && target != null && target.isAlive()) {
                target.hurt(lensward.damageSources().indirectMagic(lensward, lensward),
                        BEAM_DAMAGE);
            }
            if (ticks >= BeamPhase.FIRING.duration()) {
                lensward.setBeamPhase(BeamPhase.RECOVERY);
            }
        }

        private void tickRecovery() {
            if (lensward.advanceBeamPhase() >= BeamPhase.RECOVERY.duration()) {
                lensward.setBeamPhase(BeamPhase.IDLE);
                lensward.beamCooldown = BEAM_COOLDOWN;
            }
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return ModSounds.LENSWARD_AMBIENT.get();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(
            net.minecraft.world.damagesource.DamageSource source) {
        return ModSounds.LENSWARD_HURT.get();
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }
}
