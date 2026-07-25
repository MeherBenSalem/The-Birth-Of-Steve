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

    public static final DeferredHolder<SoundEvent, SoundEvent> CRATE_BREAK = event("block.archive_crate.break");

    public static final DeferredHolder<SoundEvent, SoundEvent> LENSWARD_CHARGE = event("entity.lensward.charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> LENSWARD_FIRE = event("entity.lensward.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> LENSWARD_AMBIENT = event("entity.lensward.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> LENSWARD_HURT = event("entity.lensward.hurt");

    private ModSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> event(String name) {
        return SOUNDS.register(
                name,
                () -> SoundEvent.createVariableRangeEvent(
                        Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, name)));
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }
}
