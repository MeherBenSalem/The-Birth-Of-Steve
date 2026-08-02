package com.nightbeam.tbos.platform.services;

import com.nightbeam.tbos.platform.registry.Registrar;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * Creates target-specific deferred registrars and entity-type builders.
 *
 * <p>The 1.21.1 Fabric API injects a no-argument entity builder that preserves
 * serialization without asking vanilla's fixed data-fixer schema to recognise a
 * mod id. NeoForge retains the normal keyed builder path. Keeping that choice in
 * this service avoids leaking either loader API into shared gameplay code.
 */
public interface IRegistryHelper {
    <T> Registrar<T> create(ResourceKey<? extends Registry<T>> registryKey, String namespace);

    <E extends Entity> EntityType<E> buildEntityType(
            EntityType.Builder<E> builder, ResourceLocation id);
}
