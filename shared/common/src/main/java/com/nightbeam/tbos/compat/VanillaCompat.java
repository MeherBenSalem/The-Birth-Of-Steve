package com.nightbeam.tbos.compat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
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
 * The server-side seam where vanilla API differences between supported Minecraft
 * versions are absorbed. This is the 26.1.2 shape; 26.2 replaces the whole class
 * from {@code 26.2/overrides}.
 *
 * <p>Everything here exists because the mod is built from one source tree for
 * several Minecraft versions. Route a call through this class instead of copying
 * its caller into an override directory - the callers are large and the deltas
 * are not.
 */
public final class VanillaCompat {

    private VanillaCompat() {
    }

    // 26.2 moved the vanilla EntityType constants out into EntityTypes. The
    // entity classes themselves stayed in the same packages, so the declared
    // types below are identical on both versions.
    public static final EntityType<Sheep> SHEEP = EntityType.SHEEP;
    public static final EntityType<ArmorStand> ARMOR_STAND = EntityType.ARMOR_STAND;
    public static final EntityType<IronGolem> IRON_GOLEM = EntityType.IRON_GOLEM;
    public static final EntityType<Husk> HUSK = EntityType.HUSK;
    public static final EntityType<Skeleton> SKELETON = EntityType.SKELETON;
    public static final EntityType<Stray> STRAY = EntityType.STRAY;
    public static final EntityType<CaveSpider> CAVE_SPIDER = EntityType.CAVE_SPIDER;
    public static final EntityType<Silverfish> SILVERFISH = EntityType.SILVERFISH;
    public static final EntityType<Vindicator> VINDICATOR = EntityType.VINDICATOR;
    public static final EntityType<Evoker> EVOKER = EntityType.EVOKER;
    public static final EntityType<Ravager> RAVAGER = EntityType.RAVAGER;

    /**
     * Knockback with no damage behind it - a pressure plate launching whoever
     * stepped on it. 26.2 requires a damage source, so it synthesises a generic
     * one there rather than making every caller invent one.
     */
    public static void knockback(LivingEntity target, double power, double xd, double zd) {
        target.knockback(power, xd, zd);
    }

    /** Knockback from a blow that already dealt {@code damage} through {@code source}. */
    public static void knockback(
            LivingEntity target, double power, double xd, double zd, DamageSource source, float damage) {
        target.knockback(power, xd, zd);
    }
}
