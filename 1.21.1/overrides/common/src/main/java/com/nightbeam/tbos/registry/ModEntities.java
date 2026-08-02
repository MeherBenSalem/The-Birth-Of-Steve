package com.nightbeam.tbos.registry;

import com.nightbeam.tbos.entity.HourCantorEntity;
import com.nightbeam.tbos.entity.LenswardEntity;
import com.nightbeam.tbos.entity.MemoryLeechEntity;
import com.nightbeam.tbos.entity.MeridianSentinelEntity;
import com.nightbeam.tbos.entity.ParallaxWraithEntity;
import com.nightbeam.tbos.entity.ThemeExclusiveEntity;
import com.nightbeam.tbos.entity.ThemeExclusiveKind;
import com.nightbeam.tbos.platform.registry.ModRegistries;
import com.nightbeam.tbos.platform.registry.RegistryEntry;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;

public final class ModEntities {
    // Declared before the constants below because static fields initialise in
    // order and each registration puts itself in here. Lets callers iterate the
    // sixteen kinds instead of restating the list — the client registers both a
    // renderer and a model layer per kind and would otherwise repeat it twice.
    private static final Map<ThemeExclusiveKind, RegistryEntry<EntityType<ThemeExclusiveEntity>>>
            THEME_EXCLUSIVES = new EnumMap<>(ThemeExclusiveKind.class);

    public static final RegistryEntry<EntityType<ParallaxWraithEntity>> PARALLAX_WRAITH =
            ModRegistries.ENTITIES.registerEntityType(
                    "parallax_wraith",
                    ParallaxWraithEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder.sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(2));

    public static final RegistryEntry<EntityType<MeridianSentinelEntity>> MERIDIAN_SENTINEL =
            ModRegistries.ENTITIES.registerEntityType(
                    "meridian_sentinel",
                    MeridianSentinelEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder.sized(0.72F, 2.05F).clientTrackingRange(10).updateInterval(2));

    public static final RegistryEntry<EntityType<HourCantorEntity>> HOUR_CANTOR =
            ModRegistries.ENTITIES.registerEntityType(
                    "hour_cantor",
                    HourCantorEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder.sized(0.8F, 2.3F).clientTrackingRange(12).updateInterval(2));

    public static final RegistryEntry<EntityType<MemoryLeechEntity>> MEMORY_LEECH =
            ModRegistries.ENTITIES.registerEntityType(
                    "memory_leech",
                    MemoryLeechEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder
                            .sized(0.9F, 0.7F)
                            .eyeHeight(0.4F)
                            .clientTrackingRange(10)
                            .updateInterval(2)
                            );

    public static final RegistryEntry<EntityType<LenswardEntity>> LENSWARD =
            ModRegistries.ENTITIES.registerEntityType(
                    "lensward",
                    LenswardEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder
                            .sized(0.9F, 1.0F)
                            .eyeHeight(0.7F)
                            .clientTrackingRange(10)
                            .updateInterval(2)
                            );

    public static final RegistryEntry<EntityType<ThemeExclusiveEntity>> SHARD_DRIFTER =
            registerKind(ThemeExclusiveKind.SHARD_DRIFTER);
    public static final RegistryEntry<EntityType<ThemeExclusiveEntity>> WAKE_CUTTER =
            registerKind(ThemeExclusiveKind.WAKE_CUTTER);
    public static final RegistryEntry<EntityType<ThemeExclusiveEntity>> NULL_PORTRAIT =
            registerKind(ThemeExclusiveKind.NULL_PORTRAIT);
    public static final RegistryEntry<EntityType<ThemeExclusiveEntity>> GALLERY_MOTH =
            registerKind(ThemeExclusiveKind.GALLERY_MOTH);
    public static final RegistryEntry<EntityType<ThemeExclusiveEntity>> GNOMON_KNIGHT =
            registerKind(ThemeExclusiveKind.GNOMON_KNIGHT);
    public static final RegistryEntry<EntityType<ThemeExclusiveEntity>> ARMILLARY_SCOUT =
            registerKind(ThemeExclusiveKind.ARMILLARY_SCOUT);
    public static final RegistryEntry<EntityType<ThemeExclusiveEntity>> DUST_CANTORILE =
            registerKind(ThemeExclusiveKind.DUST_CANTORILE);
    public static final RegistryEntry<EntityType<ThemeExclusiveEntity>> ASH_CHORISTER =
            registerKind(ThemeExclusiveKind.ASH_CHORISTER);
    public static final RegistryEntry<EntityType<ThemeExclusiveEntity>> PRISM_STALKER =
            registerKind(ThemeExclusiveKind.PRISM_STALKER);
    public static final RegistryEntry<EntityType<ThemeExclusiveEntity>> SHARDLING_SWARM =
            registerKind(ThemeExclusiveKind.SHARDLING_SWARM);
    public static final RegistryEntry<EntityType<ThemeExclusiveEntity>> INDEX_WIGHT =
            registerKind(ThemeExclusiveKind.INDEX_WIGHT);
    public static final RegistryEntry<EntityType<ThemeExclusiveEntity>> SHELF_CRAWLER =
            registerKind(ThemeExclusiveKind.SHELF_CRAWLER);
    public static final RegistryEntry<EntityType<ThemeExclusiveEntity>> METRONOME_HOUND =
            registerKind(ThemeExclusiveKind.METRONOME_HOUND);
    public static final RegistryEntry<EntityType<ThemeExclusiveEntity>> LABYRINTH_USHER =
            registerKind(ThemeExclusiveKind.LABYRINTH_USHER);
    public static final RegistryEntry<EntityType<ThemeExclusiveEntity>> BLANK_CHRONIST =
            registerKind(ThemeExclusiveKind.BLANK_CHRONIST);
    public static final RegistryEntry<EntityType<ThemeExclusiveEntity>> HOUR_HAND_WRAITH =
            registerKind(ThemeExclusiveKind.HOUR_HAND_WRAITH);

    private ModEntities() {
    }

    public static RegistryEntry<EntityType<ThemeExclusiveEntity>> themeExclusive(
            ThemeExclusiveKind kind) {
        RegistryEntry<EntityType<ThemeExclusiveEntity>> holder =
                THEME_EXCLUSIVES.get(kind);
        if (holder == null) {
            throw new IllegalStateException("Theme exclusive not registered: " + kind);
        }
        return holder;
    }

    private static RegistryEntry<EntityType<ThemeExclusiveEntity>> registerKind(
            ThemeExclusiveKind kind) {
        RegistryEntry<EntityType<ThemeExclusiveEntity>> holder = ModRegistries.ENTITIES.registerEntityType(
                kind.texturePath(),
                ThemeExclusiveEntity::new,
                MobCategory.MONSTER,
                builder -> builder
                        .sized(kind.width(), kind.height())
                        .clientTrackingRange(10)
                        .updateInterval(2)
                        );
        THEME_EXCLUSIVES.put(kind, holder);
        return holder;
    }

    /** Touching this class is what queues every entity type; the loader flushes later. */
    public static void register() {
    }

    /**
     * Hands every mod mob its default attributes.
     *
     * <p>Builders rather than built suppliers, because NeoForge's attribute
     * event wants {@code AttributeSupplier} while Fabric's registry wants the
     * builder. Each loader adapts; the numbers live here once.
     */
    public static void forEachAttribute(
            BiConsumer<EntityType<? extends LivingEntity>, AttributeSupplier.Builder> sink) {
        sink.accept(
                PARALLAX_WRAITH.get(),
                Monster.createMonsterAttributes()
                        .add(Attributes.MAX_HEALTH, 24.0D)
                        .add(Attributes.ATTACK_DAMAGE, 4.0D)
                        .add(Attributes.MOVEMENT_SPEED, 0.34D)
                        .add(Attributes.FOLLOW_RANGE, 36.0D));
        sink.accept(
                MERIDIAN_SENTINEL.get(),
                Monster.createMonsterAttributes()
                        .add(Attributes.MAX_HEALTH, 44.0D)
                        .add(Attributes.ATTACK_DAMAGE, 7.0D)
                        .add(Attributes.MOVEMENT_SPEED, 0.22D)
                        .add(Attributes.KNOCKBACK_RESISTANCE, 0.45D)
                        .add(Attributes.FOLLOW_RANGE, 32.0D));
        sink.accept(
                HOUR_CANTOR.get(),
                Monster.createMonsterAttributes()
                        .add(Attributes.MAX_HEALTH, 180.0D)
                        .add(Attributes.ATTACK_DAMAGE, 10.0D)
                        .add(Attributes.MOVEMENT_SPEED, 0.27D)
                        .add(Attributes.ARMOR, 10.0D)
                        .add(Attributes.KNOCKBACK_RESISTANCE, 0.75D)
                        .add(Attributes.FOLLOW_RANGE, 48.0D));
        sink.accept(
                MEMORY_LEECH.get(),
                Monster.createMonsterAttributes()
                        .add(Attributes.MAX_HEALTH, 32.0D)
                        .add(Attributes.ATTACK_DAMAGE, 6.0D)
                        .add(Attributes.MOVEMENT_SPEED, 0.31D)
                        .add(Attributes.ARMOR, 3.0D)
                        .add(Attributes.KNOCKBACK_RESISTANCE, 0.15D)
                        .add(Attributes.FOLLOW_RANGE, 32.0D));
        sink.accept(
                LENSWARD.get(),
                Monster.createMonsterAttributes()
                        .add(Attributes.MAX_HEALTH, 28.0D)
                        .add(Attributes.ATTACK_DAMAGE, 5.0D)
                        .add(Attributes.MOVEMENT_SPEED, 0.24D)
                        .add(Attributes.ARMOR, 6.0D)
                        .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                        .add(Attributes.FOLLOW_RANGE, 24.0D));
        putTheme(sink, SHARD_DRIFTER, 26, 4, 0.33);
        putTheme(sink, WAKE_CUTTER, 30, 6, 0.36);
        putTheme(sink, NULL_PORTRAIT, 28, 6, 0.20);
        putTheme(sink, GALLERY_MOTH, 18, 3, 0.38);
        putTheme(sink, GNOMON_KNIGHT, 40, 7, 0.22);
        putTheme(sink, ARMILLARY_SCOUT, 24, 5, 0.34);
        putTheme(sink, DUST_CANTORILE, 26, 4, 0.28);
        putTheme(sink, ASH_CHORISTER, 22, 4, 0.30);
        putTheme(sink, PRISM_STALKER, 32, 5, 0.29);
        putTheme(sink, SHARDLING_SWARM, 14, 3, 0.40);
        putTheme(sink, INDEX_WIGHT, 28, 4, 0.27);
        putTheme(sink, SHELF_CRAWLER, 20, 5, 0.32);
        putTheme(sink, METRONOME_HOUND, 26, 6, 0.35);
        putTheme(sink, LABYRINTH_USHER, 34, 5, 0.26);
        putTheme(sink, BLANK_CHRONIST, 30, 4, 0.28);
        putTheme(sink, HOUR_HAND_WRAITH, 36, 6, 0.30);
    }

    private static void putTheme(
            BiConsumer<EntityType<? extends LivingEntity>, AttributeSupplier.Builder> sink,
            RegistryEntry<EntityType<ThemeExclusiveEntity>> holder,
            double health,
            double damage,
            double speed) {
        sink.accept(
                holder.get(),
                Monster.createMonsterAttributes()
                        .add(Attributes.MAX_HEALTH, health)
                        .add(Attributes.ATTACK_DAMAGE, damage)
                        .add(Attributes.MOVEMENT_SPEED, speed)
                        .add(Attributes.FOLLOW_RANGE, 32.0D));
    }
}
