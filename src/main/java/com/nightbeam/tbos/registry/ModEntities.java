package com.nightbeam.tbos.registry;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.entity.HourCantorEntity;
import com.nightbeam.tbos.entity.LenswardEntity;
import com.nightbeam.tbos.entity.MemoryLeechEntity;
import com.nightbeam.tbos.entity.MeridianSentinelEntity;
import com.nightbeam.tbos.entity.ParallaxWraithEntity;
import com.nightbeam.tbos.entity.ThemeExclusiveEntity;
import com.nightbeam.tbos.entity.ThemeExclusiveKind;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    private static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(Yesterglass.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<ParallaxWraithEntity>> PARALLAX_WRAITH =
            ENTITIES.registerEntityType(
                    "parallax_wraith",
                    ParallaxWraithEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder.sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(2).noLootTable());

    public static final DeferredHolder<EntityType<?>, EntityType<MeridianSentinelEntity>> MERIDIAN_SENTINEL =
            ENTITIES.registerEntityType(
                    "meridian_sentinel",
                    MeridianSentinelEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder.sized(0.72F, 2.05F).clientTrackingRange(10).updateInterval(2).noLootTable());

    public static final DeferredHolder<EntityType<?>, EntityType<HourCantorEntity>> HOUR_CANTOR =
            ENTITIES.registerEntityType(
                    "hour_cantor",
                    HourCantorEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder.sized(0.8F, 2.3F).clientTrackingRange(12).updateInterval(2).noLootTable());

    public static final DeferredHolder<EntityType<?>, EntityType<MemoryLeechEntity>> MEMORY_LEECH =
            ENTITIES.registerEntityType(
                    "memory_leech",
                    MemoryLeechEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder
                            .sized(0.9F, 0.7F)
                            .eyeHeight(0.4F)
                            .clientTrackingRange(10)
                            .updateInterval(2)
                            .noLootTable());

    public static final DeferredHolder<EntityType<?>, EntityType<LenswardEntity>> LENSWARD =
            ENTITIES.registerEntityType(
                    "lensward",
                    LenswardEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder
                            .sized(0.9F, 1.0F)
                            .eyeHeight(0.7F)
                            .clientTrackingRange(10)
                            .updateInterval(2)
                            .noLootTable());

    public static final DeferredHolder<EntityType<?>, EntityType<ThemeExclusiveEntity>> SHARD_DRIFTER =
            themeExclusive(ThemeExclusiveKind.SHARD_DRIFTER);
    public static final DeferredHolder<EntityType<?>, EntityType<ThemeExclusiveEntity>> WAKE_CUTTER =
            themeExclusive(ThemeExclusiveKind.WAKE_CUTTER);
    public static final DeferredHolder<EntityType<?>, EntityType<ThemeExclusiveEntity>> NULL_PORTRAIT =
            themeExclusive(ThemeExclusiveKind.NULL_PORTRAIT);
    public static final DeferredHolder<EntityType<?>, EntityType<ThemeExclusiveEntity>> GALLERY_MOTH =
            themeExclusive(ThemeExclusiveKind.GALLERY_MOTH);
    public static final DeferredHolder<EntityType<?>, EntityType<ThemeExclusiveEntity>> GNOMON_KNIGHT =
            themeExclusive(ThemeExclusiveKind.GNOMON_KNIGHT);
    public static final DeferredHolder<EntityType<?>, EntityType<ThemeExclusiveEntity>> ARMILLARY_SCOUT =
            themeExclusive(ThemeExclusiveKind.ARMILLARY_SCOUT);
    public static final DeferredHolder<EntityType<?>, EntityType<ThemeExclusiveEntity>> DUST_CANTORILE =
            themeExclusive(ThemeExclusiveKind.DUST_CANTORILE);
    public static final DeferredHolder<EntityType<?>, EntityType<ThemeExclusiveEntity>> ASH_CHORISTER =
            themeExclusive(ThemeExclusiveKind.ASH_CHORISTER);
    public static final DeferredHolder<EntityType<?>, EntityType<ThemeExclusiveEntity>> PRISM_STALKER =
            themeExclusive(ThemeExclusiveKind.PRISM_STALKER);
    public static final DeferredHolder<EntityType<?>, EntityType<ThemeExclusiveEntity>> SHARDLING_SWARM =
            themeExclusive(ThemeExclusiveKind.SHARDLING_SWARM);
    public static final DeferredHolder<EntityType<?>, EntityType<ThemeExclusiveEntity>> INDEX_WIGHT =
            themeExclusive(ThemeExclusiveKind.INDEX_WIGHT);
    public static final DeferredHolder<EntityType<?>, EntityType<ThemeExclusiveEntity>> SHELF_CRAWLER =
            themeExclusive(ThemeExclusiveKind.SHELF_CRAWLER);
    public static final DeferredHolder<EntityType<?>, EntityType<ThemeExclusiveEntity>> METRONOME_HOUND =
            themeExclusive(ThemeExclusiveKind.METRONOME_HOUND);
    public static final DeferredHolder<EntityType<?>, EntityType<ThemeExclusiveEntity>> LABYRINTH_USHER =
            themeExclusive(ThemeExclusiveKind.LABYRINTH_USHER);
    public static final DeferredHolder<EntityType<?>, EntityType<ThemeExclusiveEntity>> BLANK_CHRONIST =
            themeExclusive(ThemeExclusiveKind.BLANK_CHRONIST);
    public static final DeferredHolder<EntityType<?>, EntityType<ThemeExclusiveEntity>> HOUR_HAND_WRAITH =
            themeExclusive(ThemeExclusiveKind.HOUR_HAND_WRAITH);

    private ModEntities() {
    }

    private static DeferredHolder<EntityType<?>, EntityType<ThemeExclusiveEntity>> themeExclusive(
            ThemeExclusiveKind kind) {
        return ENTITIES.registerEntityType(
                kind.texturePath(),
                ThemeExclusiveEntity::new,
                MobCategory.MONSTER,
                builder -> builder
                        .sized(kind.width(), kind.height())
                        .clientTrackingRange(10)
                        .updateInterval(2)
                        .noLootTable());
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        modBus.addListener(ModEntities::createAttributes);
    }

    private static void createAttributes(EntityAttributeCreationEvent event) {
        event.put(
                PARALLAX_WRAITH.get(),
                Monster.createMonsterAttributes()
                        .add(Attributes.MAX_HEALTH, 24.0D)
                        .add(Attributes.ATTACK_DAMAGE, 4.0D)
                        .add(Attributes.MOVEMENT_SPEED, 0.34D)
                        .add(Attributes.FOLLOW_RANGE, 36.0D)
                        .build());
        event.put(
                MERIDIAN_SENTINEL.get(),
                Monster.createMonsterAttributes()
                        .add(Attributes.MAX_HEALTH, 44.0D)
                        .add(Attributes.ATTACK_DAMAGE, 7.0D)
                        .add(Attributes.MOVEMENT_SPEED, 0.22D)
                        .add(Attributes.KNOCKBACK_RESISTANCE, 0.45D)
                        .add(Attributes.FOLLOW_RANGE, 32.0D)
                        .build());
        event.put(
                HOUR_CANTOR.get(),
                Monster.createMonsterAttributes()
                        .add(Attributes.MAX_HEALTH, 180.0D)
                        .add(Attributes.ATTACK_DAMAGE, 10.0D)
                        .add(Attributes.MOVEMENT_SPEED, 0.27D)
                        .add(Attributes.ARMOR, 10.0D)
                        .add(Attributes.KNOCKBACK_RESISTANCE, 0.75D)
                        .add(Attributes.FOLLOW_RANGE, 48.0D)
                        .build());
        event.put(
                MEMORY_LEECH.get(),
                Monster.createMonsterAttributes()
                        .add(Attributes.MAX_HEALTH, 32.0D)
                        .add(Attributes.ATTACK_DAMAGE, 6.0D)
                        .add(Attributes.MOVEMENT_SPEED, 0.31D)
                        .add(Attributes.ARMOR, 3.0D)
                        .add(Attributes.KNOCKBACK_RESISTANCE, 0.15D)
                        .add(Attributes.FOLLOW_RANGE, 32.0D)
                        .build());
        event.put(
                LENSWARD.get(),
                Monster.createMonsterAttributes()
                        .add(Attributes.MAX_HEALTH, 28.0D)
                        .add(Attributes.ATTACK_DAMAGE, 5.0D)
                        .add(Attributes.MOVEMENT_SPEED, 0.24D)
                        .add(Attributes.ARMOR, 6.0D)
                        .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                        .add(Attributes.FOLLOW_RANGE, 24.0D)
                        .build());
        putTheme(event, SHARD_DRIFTER, 26, 4, 0.33);
        putTheme(event, WAKE_CUTTER, 30, 6, 0.36);
        putTheme(event, NULL_PORTRAIT, 28, 6, 0.20);
        putTheme(event, GALLERY_MOTH, 18, 3, 0.38);
        putTheme(event, GNOMON_KNIGHT, 40, 7, 0.22);
        putTheme(event, ARMILLARY_SCOUT, 24, 5, 0.34);
        putTheme(event, DUST_CANTORILE, 26, 4, 0.28);
        putTheme(event, ASH_CHORISTER, 22, 4, 0.30);
        putTheme(event, PRISM_STALKER, 32, 5, 0.29);
        putTheme(event, SHARDLING_SWARM, 14, 3, 0.40);
        putTheme(event, INDEX_WIGHT, 28, 4, 0.27);
        putTheme(event, SHELF_CRAWLER, 20, 5, 0.32);
        putTheme(event, METRONOME_HOUND, 26, 6, 0.35);
        putTheme(event, LABYRINTH_USHER, 34, 5, 0.26);
        putTheme(event, BLANK_CHRONIST, 30, 4, 0.28);
        putTheme(event, HOUR_HAND_WRAITH, 36, 6, 0.30);
    }

    private static void putTheme(
            EntityAttributeCreationEvent event,
            DeferredHolder<EntityType<?>, EntityType<ThemeExclusiveEntity>> holder,
            double health,
            double damage,
            double speed) {
        event.put(
                holder.get(),
                Monster.createMonsterAttributes()
                        .add(Attributes.MAX_HEALTH, health)
                        .add(Attributes.ATTACK_DAMAGE, damage)
                        .add(Attributes.MOVEMENT_SPEED, speed)
                        .add(Attributes.FOLLOW_RANGE, 32.0D)
                        .build());
    }
}
