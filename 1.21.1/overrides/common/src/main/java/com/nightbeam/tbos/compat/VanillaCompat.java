package com.nightbeam.tbos.compat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Vindicator;

/**
 * 1.21.1's Mojang mappings keep the vanilla entity types on {@link EntityType},
 * but place several implementation classes directly in {@code monster} and
 * {@code animal}.  Keeping that difference here lets the shared encounter
 * code keep one set of semantic entity choices.
 */
public final class VanillaCompat {
    private VanillaCompat() {
    }

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

    public static void knockback(LivingEntity target, double power, double xd, double zd) {
        target.knockback(power, xd, zd);
    }

    public static void knockback(
            LivingEntity target, double power, double xd, double zd, DamageSource source, float damage) {
        target.knockback(power, xd, zd);
    }

    /** Adapts the pre-1.21.2 block item hook to its dedicated result enum. */
    public static ItemInteractionResult itemResult(InteractionResult result) {
        return switch (result) {
            case SUCCESS -> ItemInteractionResult.SUCCESS;
            case SUCCESS_NO_ITEM_USED -> ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            case CONSUME -> ItemInteractionResult.CONSUME;
            case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL;
            case PASS -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            case FAIL -> ItemInteractionResult.FAIL;
        };
    }

    /** Converts the later packed-RGB dust constructor to 1.21.1's RGB vector. */
    public static DustParticleOptions dust(int packedRgb, float scale) {
        return new DustParticleOptions(
                new Vector3f(
                        ((packedRgb >>> 16) & 0xFF) / 255.0F,
                        ((packedRgb >>> 8) & 0xFF) / 255.0F,
                        (packedRgb & 0xFF) / 255.0F),
                scale);
    }

    public static java.util.Set<String> memoryTags(LivingEntity entity) { return entity.getTags(); }
    public static boolean memoryDamage(net.minecraft.server.level.ServerLevel level, LivingEntity target,
            net.minecraft.server.level.ServerPlayer owner, float amount) {
        var source=owner==null?target.damageSources().magic():target.damageSources().playerAttack(owner);
        return target.hurt(source,amount);
    }
    public static void memoryBurn(LivingEntity target,int seconds) {target.igniteForSeconds(seconds);}
    public static net.minecraft.world.entity.Mob memoryTrialMob(net.minecraft.server.level.ServerLevel level,boolean ranged) {
        return (ranged ? SKELETON : HUSK).create(level);
    }
}
