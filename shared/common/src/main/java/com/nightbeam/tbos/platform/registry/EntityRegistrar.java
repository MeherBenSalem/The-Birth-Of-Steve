package com.nightbeam.tbos.platform.registry;

import java.util.function.UnaryOperator;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Mirrors NeoForge's {@code DeferredRegister.Entities} builder convenience. */
public final class EntityRegistrar {
    private final Registrar<EntityType<?>> delegate;

    public EntityRegistrar(Registrar<EntityType<?>> delegate) {
        this.delegate = delegate;
    }

    public <E extends Entity> RegistryEntry<EntityType<E>> registerEntityType(
            String name,
            EntityType.EntityFactory<E> factory,
            MobCategory category,
            UnaryOperator<EntityType.Builder<E>> builder) {
        return delegate.register(
                name,
                id -> builder.apply(EntityType.Builder.of(factory, category))
                        .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));
    }
}
