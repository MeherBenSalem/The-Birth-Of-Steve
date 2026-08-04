package com.nightbeam.tbos.platform.registry;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.platform.Services;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Every registrar the mod owns, in one place.
 *
 * <p>The {@code Mod*} classes populate these from their static initialisers, so
 * the registrars must exist before any of those classes is touched. Flushing is
 * the loader's job and happens once, after every {@code Mod*} class has loaded.
 */
public final class ModRegistries {
    /*
     * Declaration order is flush order on Fabric, where each entry's factory
     * runs at flush time and may dereference an earlier one. Keep dependencies
     * ahead of their dependants:
     *   blocks           <- BlockItems and block entities need the block
     *   items            <- the creative tab enumerates them
     * NeoForge is insulated from this by its own registry-event ordering, so a
     * mistake here only shows up on Fabric.
     */
    public static final BlockRegistrar BLOCKS = new BlockRegistrar(create(Registries.BLOCK));
    public static final ItemRegistrar ITEMS = new ItemRegistrar(create(Registries.ITEM));
    public static final Registrar<BlockEntityType<?>> BLOCK_ENTITIES = create(Registries.BLOCK_ENTITY_TYPE);
    public static final EntityRegistrar ENTITIES = new EntityRegistrar(create(Registries.ENTITY_TYPE));
    public static final Registrar<SoundEvent> SOUNDS = create(Registries.SOUND_EVENT);
    public static final Registrar<CreativeModeTab> CREATIVE_MODE_TABS = create(Registries.CREATIVE_MODE_TAB);

    private ModRegistries() {
    }

    private static <T> Registrar<T> create(ResourceKey<? extends Registry<T>> registryKey) {
        return Services.REGISTRIES.create(registryKey, Yesterglass.MOD_ID);
    }
}
