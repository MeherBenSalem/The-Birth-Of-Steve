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
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Backs the common registrars with Forge 1.20.1's {@link DeferredRegister}.
 *
 * <p>{@link RegistryObject} is already a supplier that resolves once the mod bus
 * has fired, so an entry is just a thin rename of one.
 */
public final class NeoForgeRegistryHelper implements IRegistryHelper {
    private static final List<DeferredRegister<?>> CREATED = new ArrayList<>();

    @Override
    public <T> Registrar<T> create(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
        DeferredRegister<T> deferred = DeferredRegister.create(registryKey, namespace);
        CREATED.add(deferred);
        return new ForgeRegistrar<>(deferred, namespace);
    }

    /** Forge 1.20.1's vanilla builder commits by full registry id. */
    @Override
    public <E extends Entity> EntityType<E> buildEntityType(
            EntityType.Builder<E> builder, ResourceLocation id) {
        return builder.build(id.toString());
    }

    /**
     * Hands every registrar created so far to the mod event bus.
     *
     * <p>Must run after every {@code Mod*} class has been touched, because a
     * {@code DeferredRegister} only publishes what was queued before this point.
     */
    public static void attachAll(IEventBus modBus) {
        CREATED.forEach(deferred -> deferred.register(modBus));
    }

    private static final class ForgeRegistrar<T> implements Registrar<T> {
        private final DeferredRegister<T> deferred;
        private final String namespace;
        private final List<RegistryEntry<T>> entries = new ArrayList<>();

        private ForgeRegistrar(DeferredRegister<T> deferred, String namespace) {
            this.deferred = deferred;
            this.namespace = namespace;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <I extends T> RegistryEntry<I> register(
                String name, Function<ResourceLocation, ? extends I> factory) {
            ResourceLocation id = new ResourceLocation(namespace, name);
            RegistryObject<I> object = deferred.register(name, () -> factory.apply(id));
            ForgeEntry<T, I> entry = new ForgeEntry<>(object, id);
            entries.add((RegistryEntry<T>) entry);
            return entry;
        }

        @Override
        public List<RegistryEntry<T>> entries() {
            return List.copyOf(entries);
        }
    }

    private record ForgeEntry<T, I extends T>(RegistryObject<I> object, ResourceLocation id)
            implements RegistryEntry<I> {
        @Override
        public I get() {
            return object.get();
        }

        @Override
        public ResourceLocation id() {
            return id;
        }

        @Override
        @SuppressWarnings("unchecked")
        public ResourceKey<I> key() {
            return (ResourceKey<I>) object.getKey();
        }
    }
}
