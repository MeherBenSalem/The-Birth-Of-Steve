package com.nightbeam.tbos.compat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.skeleton.Stray;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.zombie.Husk;

/**
 * The 26.2 shape of the server-side compat seam. Overrides the shared 26.1.2 copy
 * at {@code shared/common/src/main/java/com/nightbeam/tbos/compat/VanillaCompat.java};
 * keep the two in step whenever a constant or method is added.
 *
 * <p>26.2 moved the vanilla entity type constants from {@code EntityType} into a
 * new {@code EntityTypes} holder, and {@code LivingEntity.knockback} gained a
 * damage source and amount.
 */
public final class VanillaCompat {

    private VanillaCompat() {
    }

    public static final EntityType<Sheep> SHEEP = EntityTypes.SHEEP;
    public static final EntityType<ArmorStand> ARMOR_STAND = EntityTypes.ARMOR_STAND;
    public static final EntityType<IronGolem> IRON_GOLEM = EntityTypes.IRON_GOLEM;
    public static final EntityType<Husk> HUSK = EntityTypes.HUSK;
    public static final EntityType<Skeleton> SKELETON = EntityTypes.SKELETON;
    public static final EntityType<Stray> STRAY = EntityTypes.STRAY;
    public static final EntityType<CaveSpider> CAVE_SPIDER = EntityTypes.CAVE_SPIDER;
    public static final EntityType<Silverfish> SILVERFISH = EntityTypes.SILVERFISH;
    public static final EntityType<Vindicator> VINDICATOR = EntityTypes.VINDICATOR;
    public static final EntityType<Evoker> EVOKER = EntityTypes.EVOKER;
    public static final EntityType<Ravager> RAVAGER = EntityTypes.RAVAGER;

    /**
     * Knockback with no damage behind it - a pressure plate launching whoever
     * stepped on it. Vanilla ignores the source and amount in its own body; they
     * exist for the loaders' knockback hooks, so a generic source with zero
     * damage is the honest description of a launch that dealt none.
     */
    public static void knockback(LivingEntity target, double power, double xd, double zd) {
        target.knockback(power, xd, zd, target.level().damageSources().generic(), 0.0F);
    }

    /** Knockback from a blow that already dealt {@code damage} through {@code source}. */
    public static void knockback(
            LivingEntity target, double power, double xd, double zd, DamageSource source, float damage) {
        target.knockback(power, xd, zd, source, damage);
    }

    public static java.util.Set<String> memoryTags(LivingEntity entity) { return entity.entityTags(); }
    public static boolean memoryDamage(net.minecraft.server.level.ServerLevel level, LivingEntity target,
            net.minecraft.server.level.ServerPlayer owner, float amount) {
        var source=owner==null?target.damageSources().magic():target.damageSources().playerAttack(owner);
        return target.hurtServer(level,source,amount);
    }
    public static void memoryBurn(LivingEntity target,int seconds) {target.igniteForSeconds(seconds);}
    public static net.minecraft.world.entity.Mob memoryTrialMob(net.minecraft.server.level.ServerLevel level,boolean ranged) {
        return (ranged ? SKELETON : HUSK).create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
    }
}
