package com.nightbeam.tbos.platform.registry;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Block-shaped sugar over a plain {@link Registrar}.
 *
 * <p>The bodies mirror NeoForge's {@code DeferredRegister.Blocks} exactly,
 * including the {@code setId} call 26.1 requires on block properties, so the
 * NeoForge side of the port keeps producing byte-identical blocks.
 */
public final class BlockRegistrar {
    private final Registrar<Block> delegate;

    public BlockRegistrar(Registrar<Block> delegate) {
        this.delegate = delegate;
    }

    public <B extends Block> RegistryEntry<B> registerBlock(
            String name,
            Function<BlockBehaviour.Properties, ? extends B> factory,
            Supplier<BlockBehaviour.Properties> properties) {
        return delegate.register(
                name,
                id -> factory.apply(properties.get().setId(ResourceKey.create(Registries.BLOCK, id))));
    }

    public <B extends Block> RegistryEntry<B> registerBlock(
            String name,
            Function<BlockBehaviour.Properties, ? extends B> factory,
            UnaryOperator<BlockBehaviour.Properties> properties) {
        return registerBlock(name, factory, () -> properties.apply(BlockBehaviour.Properties.of()));
    }

    public RegistryEntry<Block> registerSimpleBlock(
            String name, Supplier<BlockBehaviour.Properties> properties) {
        return registerBlock(name, Block::new, properties);
    }

    public RegistryEntry<Block> registerSimpleBlock(
            String name, UnaryOperator<BlockBehaviour.Properties> properties) {
        return registerBlock(name, Block::new, properties);
    }

    public List<RegistryEntry<Block>> entries() {
        return delegate.entries();
    }
}
