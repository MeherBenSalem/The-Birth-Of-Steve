package com.nightbeam.tbos.platform.services;

import com.nightbeam.tbos.platform.registry.Registrar;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/**
 * Creates the deferred registrars that {@code ModBlocks}, {@code ModItems} and
 * friends populate from their static initialisers.
 *
 * <p>Neither loader lets a mod write into a built-in registry at an arbitrary
 * moment, so registration is always deferred: NeoForge queues onto its mod bus,
 * Fabric holds the entries until its initialiser runs. Both are flushed by the
 * loader entry point once every {@code Mod*} class has been touched.
 */
public interface IRegistryHelper {
    <T> Registrar<T> create(ResourceKey<? extends Registry<T>> registryKey, String namespace);
}
