package com.nightbeam.tbos.platform.registry;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;

/** Deferred registration into one Minecraft registry, for one namespace. */
public interface Registrar<T> {
    /**
     * @param factory receives the fully-qualified id, because 26.1 requires
     *                block and item properties to carry their own registry key
     */
    <I extends T> RegistryEntry<I> register(String name, Function<Identifier, ? extends I> factory);

    default <I extends T> RegistryEntry<I> register(String name, Supplier<? extends I> factory) {
        return register(name, id -> factory.get());
    }

    /** Every entry in declaration order. The creative tab depends on that order. */
    List<RegistryEntry<T>> entries();
}
