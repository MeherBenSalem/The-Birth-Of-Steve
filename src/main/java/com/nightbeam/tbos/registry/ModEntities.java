package com.nightbeam.tbos.registry;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.entity.HourCantorEntity;
import com.nightbeam.tbos.entity.MemoryLeechEntity;
import com.nightbeam.tbos.entity.MeridianSentinelEntity;
import com.nightbeam.tbos.entity.ParallaxWraithEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
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

    private ModEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        modBus.addListener(ModEntities::createAttributes);
    }

    private static void createAttributes(EntityAttributeCreationEvent event) {
        event.put(
                PARALLAX_WRAITH.get(),
                Zombie.createAttributes()
                        .add(Attributes.MAX_HEALTH, 24.0D)
                        .add(Attributes.ATTACK_DAMAGE, 4.0D)
                        .add(Attributes.MOVEMENT_SPEED, 0.34D)
                        .add(Attributes.FOLLOW_RANGE, 36.0D)
                        .build());
        event.put(
                MERIDIAN_SENTINEL.get(),
                Zombie.createAttributes()
                        .add(Attributes.MAX_HEALTH, 44.0D)
                        .add(Attributes.ATTACK_DAMAGE, 7.0D)
                        .add(Attributes.MOVEMENT_SPEED, 0.22D)
                        .add(Attributes.KNOCKBACK_RESISTANCE, 0.45D)
                        .add(Attributes.FOLLOW_RANGE, 32.0D)
                        .build());
        event.put(
                HOUR_CANTOR.get(),
                Zombie.createAttributes()
                        .add(Attributes.MAX_HEALTH, 180.0D)
                        .add(Attributes.ATTACK_DAMAGE, 10.0D)
                        .add(Attributes.MOVEMENT_SPEED, 0.27D)
                        .add(Attributes.ARMOR, 10.0D)
                        .add(Attributes.KNOCKBACK_RESISTANCE, 0.75D)
                        .add(Attributes.FOLLOW_RANGE, 48.0D)
                        .build());
        event.put(
                MEMORY_LEECH.get(),
                net.minecraft.world.entity.monster.Monster.createMonsterAttributes()
                        .add(Attributes.MAX_HEALTH, 32.0D)
                        .add(Attributes.ATTACK_DAMAGE, 6.0D)
                        .add(Attributes.MOVEMENT_SPEED, 0.31D)
                        .add(Attributes.ARMOR, 3.0D)
                        .add(Attributes.KNOCKBACK_RESISTANCE, 0.15D)
                        .add(Attributes.FOLLOW_RANGE, 32.0D)
                        .build());
    }
}
