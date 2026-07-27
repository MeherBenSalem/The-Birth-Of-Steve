package com.nightbeam.tbos.entity;

import com.nightbeam.tbos.block.ThemeHazardBlock;
import com.nightbeam.tbos.registry.ModBlocks;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
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

    private final ThemeExclusiveKind kind;
    private int abilityCooldown = 40;
    private boolean ashSplitDone;
    private BlockPos sealPos;
    private int sealTicks;

    public ThemeExclusiveEntity(EntityType<? extends ThemeExclusiveEntity> entityType, Level level) {
        super(entityType, level);
        kind = ThemeExclusiveKind.of(entityType);
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
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(ABILITY_PHASE, (byte) AbilityPhase.IDLE.ordinal());
        entityData.define(ABILITY_PHASE_TICKS, 0);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
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
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setAbilityPhase(AbilityPhase.IDLE);
        abilityCooldown = 40;
        ashSplitDone = false;
        sealPos = null;
        sealTicks = 0;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        float applied = amount;
        if (kind == ThemeExclusiveKind.PRISM_STALKER && getAbilityPhase() == AbilityPhase.IDLE) {
            applied *= 0.35F;
        }
        if (kind == ThemeExclusiveKind.WAKE_CUTTER && getAbilityPhase() == AbilityPhase.ACTIVE) {
            return false;
        }
        boolean hurt = super.hurtServer(level, source, applied);
        if (hurt
                && kind == ThemeExclusiveKind.ASH_CHORISTER
                && !ashSplitDone
                && getHealth() <= getMaxHealth() * 0.45F) {
            ashSplitDone = true;
            spawnAshWisp(level);
            spawnAshWisp(level);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource damageSource) {
        if (level() instanceof ServerLevel serverLevel && kind == ThemeExclusiveKind.SHARDLING_SWARM) {
            ThemeHazardBlock.shatterNearby(serverLevel, blockPosition(), 2);
        }
        super.die(damageSource);
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
        child.snapTo(getX() + (random.nextDouble() - 0.5D), getY(), getZ() + (random.nextDouble() - 0.5D), getYRot(), 0.0F);
        child.setHealth(Math.max(6.0F, getMaxHealth() * 0.35F));
        child.ashSplitDone = true;
        child.setTarget(getTarget());
        level.addFreshEntity(child);
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
                            ParticleTypes.ENCHANT,
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
            return switch (kind) {
                case METRONOME_HOUND -> 50;
                case GALLERY_MOTH, SHARDLING_SWARM -> 70;
                case LABYRINTH_USHER -> 100;
                default -> 80;
            };
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
                case PRISM_STALKER -> {
                    // Vulnerability window opens during ACTIVE; damage taken normally then.
                }
                case SHARDLING_SWARM -> pulse(level, 2.0D, 2.5F);
                case INDEX_WIGHT -> markTarget(level);
                case SHELF_CRAWLER -> dropOnTarget(level);
                case METRONOME_HOUND -> biteOnBeat(level);
                case LABYRINTH_USHER -> sealNearby(level);
                case BLANK_CHRONIST -> weaken(level);
                case HOUR_HAND_WRAITH -> sweep(level);
            }
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
                target.hurtServer(level, mob.damageSources().mobAttack(mob), 5.0F);
            }
        }

        private void lunge(ServerLevel level) {
            Vec3 delta = target.position().subtract(mob.position()).normalize().scale(1.1D);
            mob.setDeltaMovement(delta.x, 0.2D, delta.z);
            mob.hurtMarked = true;
            target.hurtServer(level, mob.damageSources().mobAttack(mob), 6.0F);
        }

        private void dive(ServerLevel level) {
            mob.teleportTo(target.getX(), target.getY(), target.getZ());
            pulse(level, 2.5D, 3.5F);
        }

        private void pulse(ServerLevel level, double radius, float damage) {
            AABB box = mob.getBoundingBox().inflate(radius);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, candidate ->
                    candidate.isAlive() && !(candidate instanceof Monster))) {
                entity.hurtServer(level, mob.damageSources().mobAttack(mob), damage);
            }
            level.sendParticles(ParticleTypes.CRIT, mob.getX(), mob.getY() + 0.2D, mob.getZ(), 18, radius * 0.35D, 0.1D, radius * 0.35D, 0.05D);
        }

        private void pulseSlow(ServerLevel level) {
            AABB box = mob.getBoundingBox().inflate(4.0D);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, candidate ->
                    candidate.isAlive() && !(candidate instanceof Monster))) {
                entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1, false, true, true));
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
            target.hurtServer(level, mob.damageSources().mobAttack(mob), 5.0F);
        }

        private void biteOnBeat(ServerLevel level) {
            if (mob.tickCount % 20 < 10) {
                target.hurtServer(level, mob.damageSources().mobAttack(mob), 6.0F);
            } else {
                mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 30, 0, false, false, true));
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
                entity.hurtServer(level, mob.damageSources().mobAttack(mob), 5.5F);
                entity.knockback(0.7D, mob.getX() - entity.getX(), mob.getZ() - entity.getZ());
            }
        }
    }

    /** Breaks the ModEntities compile cycle while ash wisps reuse this class. */
    private static final class ModEntitiesHolder {
        private static ThemeExclusiveEntity createAshChorister(ServerLevel level) {
            return com.nightbeam.tbos.registry.ModEntities.ASH_CHORISTER.get().create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        }
    }
}
