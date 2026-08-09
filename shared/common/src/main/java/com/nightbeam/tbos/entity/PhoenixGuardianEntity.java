package com.nightbeam.tbos.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
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
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * The Last Curator: the overworld campaign's closing boss.
 *
 * <p>It fights on a two-beat rhythm rather than a single melee loop. At range it
 * {@linkplain Phase#DASH dashes} the gap shut; in close it winds up a
 * {@linkplain Phase#CHARGE charge} and detonates an {@linkplain Phase#EMBER_BURST
 * ember burst} that ignites everything inside {@link #BURST_RADIUS}. The wind-up
 * is the whole fight's tell — the wing-arms fan wide well before the blast lands,
 * so the burst is dodgeable by anyone reading the model rather than the health bar.
 *
 * <p>Its one structural trick is the rebirth. The first killing blow does not
 * kill: {@link #die} intercepts it, restores half health, and flips the boss bar
 * red for a second, angrier pass. The second killing blow is final. Because that
 * seam is {@code die} rather than damage prediction, it holds regardless of how
 * the blow was dealt — armour, magic, or a single overwhelming hit.
 */
public final class PhoenixGuardianEntity extends Monster {
    private static final EntityDataAccessor<Byte> PHASE =
            SynchedEntityData.defineId(PhoenixGuardianEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> PHASE_TICKS =
            SynchedEntityData.defineId(PhoenixGuardianEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RISEN =
            SynchedEntityData.defineId(PhoenixGuardianEntity.class, EntityDataSerializers.BOOLEAN);

    /** Reach of the ember burst, and the radius the arena is built to. */
    public static final double BURST_RADIUS = 7.0D;

    /** Health the Curator returns with after its rebirth. */
    public static final float REBIRTH_HEALTH_FRACTION = 0.5F;

    private static final double BURST_RADIUS_SQR = BURST_RADIUS * BURST_RADIUS;
    private static final float BURST_DAMAGE = 7.0F;
    private static final float BURST_FIRE_SECONDS = 4.0F;
    private static final int CHARGE_TICKS = 30;
    private static final int DASH_TICKS = 16;
    private static final int STRIKE_TICKS = 14;
    private static final int BURST_TICKS = 40;

    /** The blast lands early in the phase so the flare reads as cause, not echo. */
    private static final int BURST_IMPACT_TICK = 8;

    private static final int BURST_COOLDOWN = 160;
    private static final int INITIAL_BURST_COOLDOWN = 80;
    private static final int DASH_COOLDOWN = 90;
    private static final int INITIAL_DASH_COOLDOWN = 60;
    private static final int REBIRTH_TICKS = 60;

    /** Dash only from beyond melee, and only while the target is still worth crossing to. */
    private static final double DASH_MIN_RANGE_SQR = 25.0D;
    private static final double DASH_MAX_RANGE_SQR = 400.0D;
    private static final double DASH_SPEED = 1.15D;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            getUUID(),
            Component.translatable("entity.tbos.phoenix_guardian"),
            BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.NOTCHED_10);

    private int burstCooldown = INITIAL_BURST_COOLDOWN;
    private int dashCooldown = INITIAL_DASH_COOLDOWN;
    private int rebirthTicks;
    private boolean siteManaged;

    public PhoenixGuardianEntity(EntityType<? extends PhoenixGuardianEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 75;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 20.0F));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(PHASE, (byte) Phase.IDLE.ordinal());
        entityData.define(PHASE_TICKS, 0);
        entityData.define(RISEN, false);
    }

    /**
     * Drives the attack rhythm.
     *
     * <p>This is a plain state machine rather than a set of goals because every
     * phase is mutually exclusive and timed: expressing it as competing goals
     * would let the dash and the burst interleave, and the wind-up is only a fair
     * tell if nothing can cut it short.
     */
    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        bossEvent.setProgress(getHealth() / getMaxHealth());

        if (rebirthTicks > 0) {
            tickRebirth(level);
            return;
        }

        Phase phase = getPhase();
        if (phase == Phase.IDLE) {
            tickIdle();
            return;
        }

        int elapsed = getPhaseTicks() + 1;
        setPhaseTicks(elapsed);
        switch (phase) {
            case CHARGE -> {
                if (elapsed >= CHARGE_TICKS) {
                    beginPhase(Phase.EMBER_BURST);
                }
            }
            case EMBER_BURST -> {
                if (elapsed == BURST_IMPACT_TICK) {
                    detonate(level);
                }
                if (elapsed >= BURST_TICKS) {
                    burstCooldown = BURST_COOLDOWN;
                    beginPhase(Phase.IDLE);
                }
            }
            case DASH -> {
                if (elapsed >= DASH_TICKS) {
                    dashCooldown = DASH_COOLDOWN;
                    beginPhase(Phase.IDLE);
                }
            }
            case STRIKE -> {
                if (elapsed >= STRIKE_TICKS) {
                    beginPhase(Phase.IDLE);
                }
            }
            default -> beginPhase(Phase.IDLE);
        }
    }

    private void tickIdle() {
        if (burstCooldown > 0) {
            burstCooldown--;
        }
        if (dashCooldown > 0) {
            dashCooldown--;
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        double range = distanceToSqr(target);
        if (burstCooldown <= 0 && range <= BURST_RADIUS_SQR) {
            beginPhase(Phase.CHARGE);
            playSound(SoundEvents.BLAZE_SHOOT, 1.4F, 0.7F);
        } else if (dashCooldown <= 0 && range > DASH_MIN_RANGE_SQR && range < DASH_MAX_RANGE_SQR) {
            beginPhase(Phase.DASH);
            launchDash(target);
        }
    }

    private void launchDash(LivingEntity target) {
        Vec3 toTarget = target.position().subtract(position());
        // Flattened, then given a fixed lift: a dash that tracked the target's Y
        // would read as flight and let the Curator drift off the arena floor.
        Vec3 flat = new Vec3(toTarget.x, 0.0D, toTarget.z);
        if (flat.lengthSqr() < 1.0E-4D) {
            return;
        }
        setDeltaMovement(flat.normalize().scale(DASH_SPEED).add(0.0D, 0.32D, 0.0D));
        hurtMarked = true;
        playSound(SoundEvents.PHANTOM_FLAP, 1.5F, 0.6F);
    }

    private void detonate(ServerLevel level) {
        for (LivingEntity victim :
                level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(BURST_RADIUS))) {
            if (victim == this || !victim.isAlive()) {
                continue;
            }
            if (distanceToSqr(victim) > BURST_RADIUS_SQR) {
                continue;
            }
            victim.hurtServer(level, damageSources().mobAttack(this), BURST_DAMAGE);
            victim.igniteForSeconds(BURST_FIRE_SECONDS);
        }

        level.sendParticles(
                ParticleTypes.FLAME, getX(), getY() + 1.5D, getZ(), 90, 1.6D, 0.9D, 1.6D, 0.14D);
        level.sendParticles(
                ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.2D, getZ(), 40, 1.4D, 0.6D, 1.4D, 0.05D);
        level.playSound(
                null, getX(), getY(), getZ(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5F, 0.9F);
    }

    private void tickRebirth(ServerLevel level) {
        rebirthTicks--;
        // Anchored for the whole rebirth: it is a scripted beat, and drifting
        // through it would let knockback carry the Curator out of its arena.
        setDeltaMovement(Vec3.ZERO);
        level.sendParticles(
                ParticleTypes.FLAME, getX(), getY() + 1.4D, getZ(), 12, 0.9D, 1.1D, 0.9D, 0.06D);
        if (rebirthTicks == 0) {
            level.playSound(
                    null, getX(), getY(), getZ(),
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.6F, 1.4F);
            level.sendParticles(
                    ParticleTypes.FLAME, getX(), getY() + 1.4D, getZ(), 140, 2.0D, 1.4D, 2.0D, 0.22D);
            beginPhase(Phase.IDLE);
        }
    }

    /**
     * Turns the first killing blow into the rebirth.
     *
     * <p>Returning without calling {@code super} is what keeps the entity alive:
     * vanilla sets {@code dead} inside {@link LivingEntity#die}, so skipping it
     * leaves the Curator merely at zero health, which the restore below undoes in
     * the same tick.
     */
    @Override
    public void die(DamageSource damageSource) {
        if (!siteManaged && !hasRisen() && level() instanceof ServerLevel level) {
            setRisen(true);
            setHealth(getMaxHealth() * REBIRTH_HEALTH_FRACTION);
            rebirthTicks = REBIRTH_TICKS;
            burstCooldown = INITIAL_BURST_COOLDOWN;
            dashCooldown = INITIAL_DASH_COOLDOWN;
            // Idle for the duration: the phase machine is paused while
            // rebirthTicks runs, so the curl-and-flare comes off the rebirth
            // progress instead of a phase that would never advance.
            beginPhase(Phase.IDLE);
            bossEvent.setColor(BossEvent.BossBarColor.RED);
            level.playSound(
                    null, getX(), getY(), getZ(),
                    SoundEvents.BLAZE_DEATH, SoundSource.HOSTILE, 1.8F, 0.6F);
            return;
        }
        super.die(damageSource);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // The rebirth is a scripted beat; letting damage land during it would
        // make the second phase winnable by simply out-DPSing the animation.
        if (rebirthTicks > 0) {
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hurt = super.doHurtTarget(level, target);
        if (hurt && getPhase() == Phase.IDLE) {
            beginPhase(Phase.STRIKE);
        }
        return hurt;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setRisen(input.getBooleanOr("curator_risen", false));
        setSiteManaged(input.getBooleanOr("curator_site_managed", false));
        // An interrupted phase must restart rather than resolve on load, and a
        // rebirth caught mid-flight would otherwise leave the Curator anchored.
        rebirthTicks = 0;
        burstCooldown = INITIAL_BURST_COOLDOWN;
        dashCooldown = INITIAL_DASH_COOLDOWN;
        beginPhase(Phase.IDLE);
        if (hasRisen()) {
            bossEvent.setColor(BossEvent.BossBarColor.RED);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("curator_risen", hasRisen());
        output.putBoolean("curator_site_managed", siteManaged);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (!siteManaged) {
            bossEvent.addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    /**
     * Hands the encounter's framing to {@code LastCuratorEncounterTracker}.
     *
     * <p>The authored site fight already owns a boss bar, a three-phase health
     * curve and its own defeat handling, so a site-managed Curator suppresses the
     * two things that would collide: its private boss bar, and the rebirth, whose
     * second life would desynchronise {@code LastCuratorProgress}. The attack
     * rhythm stays — that is the part the site has no opinion about.
     */
    public void setSiteManaged(boolean siteManaged) {
        this.siteManaged = siteManaged;
        if (siteManaged) {
            bossEvent.removeAllPlayers();
        }
    }

    public boolean isSiteManaged() {
        return siteManaged;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BLAZE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BLAZE_DEATH;
    }

    public Phase getPhase() {
        byte ordinal = entityData.get(PHASE);
        Phase[] values = Phase.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : Phase.IDLE;
    }

    public int getPhaseTicks() {
        return entityData.get(PHASE_TICKS);
    }

    public boolean hasRisen() {
        return entityData.get(RISEN);
    }

    /** Zero at the start of the current phase, one at its end. */
    public float getPhaseProgress(float partialTick) {
        int span = getPhase().durationTicks();
        if (span <= 0) {
            return 0.0F;
        }
        return Mth.clamp((getPhaseTicks() + partialTick) / span, 0.0F, 1.0F);
    }

    /** Zero outside the rebirth, rising to one across it. */
    public float getRebirthProgress(float partialTick) {
        if (rebirthTicks <= 0) {
            return 0.0F;
        }
        return Mth.clamp((REBIRTH_TICKS - rebirthTicks + partialTick) / REBIRTH_TICKS, 0.0F, 1.0F);
    }

    private void setRisen(boolean risen) {
        entityData.set(RISEN, risen);
    }

    private void setPhaseTicks(int ticks) {
        entityData.set(PHASE_TICKS, ticks);
    }

    private void beginPhase(Phase phase) {
        entityData.set(PHASE, (byte) phase.ordinal());
        setPhaseTicks(0);
    }

    /** The attack beats, in the order the model animates them. */
    public enum Phase {
        IDLE(0),
        CHARGE(CHARGE_TICKS),
        DASH(DASH_TICKS),
        STRIKE(STRIKE_TICKS),
        EMBER_BURST(BURST_TICKS);

        private final int durationTicks;

        Phase(int durationTicks) {
            this.durationTicks = durationTicks;
        }

        public int durationTicks() {
            return durationTicks;
        }
    }
}
