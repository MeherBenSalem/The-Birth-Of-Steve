package com.nightbeam.tbos.fabric.platform;

import com.nightbeam.tbos.platform.registry.Registrar;
import com.nightbeam.tbos.platform.registry.RegistryEntry;
import com.nightbeam.tbos.platform.services.IRegistryHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Holds registrations until Fabric's initialiser runs, then commits them with
 * plain {@link Registry#register}.
 *
 * <p>Order matters and is the reason {@link #flushAll()} walks the registrars in
 * creation order: a {@code BlockItem}'s factory dereferences its block, so the
 * block registrar has to be committed first. {@code Yesterglass.init()} touches
 * the {@code Mod*} classes in exactly that order.
 */
public final class FabricRegistryHelper implements IRegistryHelper {
    private static final List<FabricRegistrar<?>> CREATED = new ArrayList<>();

    @Override
    public <T> Registrar<T> create(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
        FabricRegistrar<T> registrar = new FabricRegistrar<>(registryKey, namespace);
        CREATED.add(registrar);
        return registrar;
    }

    public static void flushAll() {
        CREATED.forEach(FabricRegistrar::flush);
    }

    private static final class FabricRegistrar<T> implements Registrar<T> {
        private final ResourceKey<? extends Registry<T>> registryKey;
        private final String namespace;
        private final List<FabricEntry<T, ?>> pending = new ArrayList<>();
        private boolean flushed;

        private FabricRegistrar(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
            this.registryKey = registryKey;
            this.namespace = namespace;
        }

        @Override
        public <I extends T> RegistryEntry<I> register(
                String name, Function<Identifier, ? extends I> factory) {
            if (flushed) {
                throw new IllegalStateException(
                        "Registrar for " + registryKey.identifier() + " was already flushed; "
                                + name + " is too late");
            }
            FabricEntry<T, I> entry = new FabricEntry<>(
                    Identifier.fromNamespaceAndPath(namespace, name), registryKey, factory);
            pending.add(entry);
            return entry;
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<RegistryEntry<T>> entries() {
            return List.copyOf((List<? extends RegistryEntry<T>>) (List<?>) pending);
        }

        @SuppressWarnings("unchecked")
        private void flush() {
            if (flushed) {
                return;
            }
            flushed = true;
            Registry<T> registry = (Registry<T>) BuiltInRegistries.REGISTRY.getValue(registryKey.identifier());
            if (registry == null) {
                throw new IllegalStateException("No registry for " + registryKey.identifier());
            }
            pending.forEach(entry -> entry.commit(registry));
        }
    }

    private static final class FabricEntry<T, I extends T> implements RegistryEntry<I> {
        private final Identifier id;
        private final ResourceKey<? extends Registry<T>> registryKey;
        private final Function<Identifier, ? extends I> factory;
        private I value;

        private FabricEntry(
                Identifier id,
                ResourceKey<? extends Registry<T>> registryKey,
                Function<Identifier, ? extends I> factory) {
            this.id = id;
            this.registryKey = registryKey;
            this.factory = factory;
        }

        @SuppressWarnings("unchecked")
        private void commit(Registry<T> registry) {
            value = (I) Registry.register(registry, id, factory.apply(id));
        }

        @Override
        public I get() {
            if (value == null) {
                throw new IllegalStateException(id + " has not been registered yet");
            }
            return value;
        }

        @Override
        public Identifier id() {
            return id;
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public ResourceKey<I> key() {
            return (ResourceKey<I>) ResourceKey.create((ResourceKey) registryKey, id);
        }
    }
}
