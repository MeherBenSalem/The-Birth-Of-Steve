package com.nightbeam.tbos.platform.registry;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Item-shaped sugar over a plain {@link Registrar}.
 *
 * <p>The bodies mirror NeoForge's {@code DeferredRegister.Items} exactly,
 * including the {@code setId} on item properties and the
 * {@code useBlockDescriptionPrefix} that gives a {@link BlockItem} its
 * {@code block.} translation key.
 */
public final class ItemRegistrar {
    private final Registrar<Item> delegate;

    public ItemRegistrar(Registrar<Item> delegate) {
        this.delegate = delegate;
    }

    public <I extends Item> RegistryEntry<I> registerItem(
            String name,
            Function<Item.Properties, ? extends I> factory,
            Supplier<Item.Properties> properties) {
        return delegate.register(
                name,
                id -> factory.apply(properties.get()));
    }

    public <I extends Item> RegistryEntry<I> registerItem(
            String name,
            Function<Item.Properties, ? extends I> factory,
            UnaryOperator<Item.Properties> properties) {
        return registerItem(name, factory, () -> properties.apply(new Item.Properties()));
    }

    public RegistryEntry<Item> registerSimpleItem(
            String name, UnaryOperator<Item.Properties> properties) {
        return registerItem(name, Item::new, properties);
    }

    public RegistryEntry<BlockItem> registerSimpleBlockItem(
            String name, Supplier<? extends Block> block) {
        return registerItem(
                name,
                properties -> new BlockItem(block.get(), properties),
                Item.Properties::new);
    }

    public List<RegistryEntry<Item>> entries() {
        return delegate.entries();
    }
}
