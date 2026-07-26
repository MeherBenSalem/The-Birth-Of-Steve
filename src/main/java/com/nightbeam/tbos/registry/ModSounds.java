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

    public static final DeferredHolder<SoundEvent, SoundEvent> PARALLAX_WRAITH_FRACTURE =
            event("entity.parallax_wraith.fracture");
    public static final DeferredHolder<SoundEvent, SoundEvent> PARALLAX_WRAITH_REFORM =
            event("entity.parallax_wraith.reform");
    public static final DeferredHolder<SoundEvent, SoundEvent> PARALLAX_WRAITH_AMBIENT =
            event("entity.parallax_wraith.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> PARALLAX_WRAITH_HURT =
            event("entity.parallax_wraith.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> MERIDIAN_SENTINEL_WIND_UP =
            event("entity.meridian_sentinel.wind_up");
    public static final DeferredHolder<SoundEvent, SoundEvent> MERIDIAN_SENTINEL_SLAM =
            event("entity.meridian_sentinel.slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> MERIDIAN_SENTINEL_AMBIENT =
            event("entity.meridian_sentinel.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> MERIDIAN_SENTINEL_HURT =
            event("entity.meridian_sentinel.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> HOUR_CANTOR_INTONE =
            event("entity.hour_cantor.intone");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOUR_CANTOR_REFRAIN =
            event("entity.hour_cantor.refrain");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOUR_CANTOR_AMBIENT =
            event("entity.hour_cantor.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOUR_CANTOR_HURT =
            event("entity.hour_cantor.hurt");

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
