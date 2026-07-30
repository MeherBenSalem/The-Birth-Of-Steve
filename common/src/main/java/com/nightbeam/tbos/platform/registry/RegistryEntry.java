package com.nightbeam.tbos.platform.registry;

import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * A registered object that may not exist yet.
 *
 * <p>Deliberately a {@link Supplier}: the fields in {@code ModBlocks},
 * {@code ModItems} and the rest used to be NeoForge {@code DeferredHolder}s,
 * which are suppliers too, so every {@code ModX.Y.get()} call site in the mod
 * reads the same after the move to this interface.
 */
public interface RegistryEntry<T> extends Supplier<T> {
    /** Known as soon as the entry is declared, long before {@link #get()} works. */
    Identifier id();

    ResourceKey<T> key();

    /**
     * @throws IllegalStateException before the owning registrar has been flushed
     */
    @Override
    T get();
}
