package com.nightbeam.tbos.neoforge.platform;

import com.nightbeam.tbos.platform.registry.Registrar;
import com.nightbeam.tbos.platform.registry.RegistryEntry;
import com.nightbeam.tbos.platform.services.IRegistryHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Backs the common registrars with NeoForge's {@link DeferredRegister}.
 *
 * <p>{@code DeferredHolder} is already a supplier that resolves once the mod bus
 * has fired, so an entry is just a thin rename of one.
 */
public final class NeoForgeRegistryHelper implements IRegistryHelper {
    private static final List<DeferredRegister<?>> CREATED = new ArrayList<>();

    @Override
    public <T> Registrar<T> create(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
        DeferredRegister<T> deferred = DeferredRegister.create(registryKey, namespace);
        CREATED.add(deferred);
        return new NeoForgeRegistrar<>(deferred);
    }

    /** NeoForge's keyed builder is its normal 1.21.1 entity registration path. */
    @Override
    public <E extends Entity> EntityType<E> buildEntityType(
            EntityType.Builder<E> builder, ResourceLocation id) {
        return builder.build(id.toString());
    }

    /**
     * Hands every registrar created so far to the mod bus.
     *
     * <p>Must run after every {@code Mod*} class has been touched, because a
     * {@code DeferredRegister} only publishes what was queued before this point.
     */
    public static void attachAll(IEventBus modBus) {
        CREATED.forEach(deferred -> deferred.register(modBus));
    }

    private static final class NeoForgeRegistrar<T> implements Registrar<T> {
        private final DeferredRegister<T> deferred;
        private final List<RegistryEntry<T>> entries = new ArrayList<>();

        private NeoForgeRegistrar(DeferredRegister<T> deferred) {
            this.deferred = deferred;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <I extends T> RegistryEntry<I> register(
                String name, Function<ResourceLocation, ? extends I> factory) {
            DeferredHolder<T, I> holder = deferred.register(name, factory);
            NeoForgeEntry<T, I> entry = new NeoForgeEntry<>(holder);
            entries.add((RegistryEntry<T>) entry);
            return entry;
        }

        @Override
        public List<RegistryEntry<T>> entries() {
            return List.copyOf(entries);
        }
    }

    private record NeoForgeEntry<T, I extends T>(DeferredHolder<T, I> holder) implements RegistryEntry<I> {
        @Override
        public I get() {
            return holder.get();
        }

        @Override
        public ResourceLocation id() {
            return holder.getId();
        }

        @Override
        @SuppressWarnings("unchecked")
        public ResourceKey<I> key() {
            return (ResourceKey<I>) holder.getKey();
        }
    }
}
