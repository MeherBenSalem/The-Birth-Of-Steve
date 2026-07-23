package com.nightbeam.tbos.registry;

import com.nightbeam.tbos.Yesterglass;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Yesterglass.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> CRATE_BREAK = SOUNDS.register(
            "block.archive_crate.break",
            () -> SoundEvent.createVariableRangeEvent(
                    Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "block.archive_crate.break")));

    private ModSounds() {
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }
}
