package com.nightbeam.tbos.registry;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.platform.registry.ModRegistries;
import com.nightbeam.tbos.platform.registry.RegistryEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
    public static final RegistryEntry<SoundEvent> CRATE_BREAK = event("block.archive_crate.break");

    public static final RegistryEntry<SoundEvent> LENSWARD_CHARGE = event("entity.lensward.charge");
    public static final RegistryEntry<SoundEvent> LENSWARD_FIRE = event("entity.lensward.fire");
    public static final RegistryEntry<SoundEvent> LENSWARD_AMBIENT = event("entity.lensward.ambient");
    public static final RegistryEntry<SoundEvent> LENSWARD_HURT = event("entity.lensward.hurt");

    public static final RegistryEntry<SoundEvent> PARALLAX_WRAITH_FRACTURE =
            event("entity.parallax_wraith.fracture");
    public static final RegistryEntry<SoundEvent> PARALLAX_WRAITH_REFORM =
            event("entity.parallax_wraith.reform");
    public static final RegistryEntry<SoundEvent> PARALLAX_WRAITH_AMBIENT =
            event("entity.parallax_wraith.ambient");
    public static final RegistryEntry<SoundEvent> PARALLAX_WRAITH_HURT =
            event("entity.parallax_wraith.hurt");

    public static final RegistryEntry<SoundEvent> MERIDIAN_SENTINEL_WIND_UP =
            event("entity.meridian_sentinel.wind_up");
    public static final RegistryEntry<SoundEvent> MERIDIAN_SENTINEL_SLAM =
            event("entity.meridian_sentinel.slam");
    public static final RegistryEntry<SoundEvent> MERIDIAN_SENTINEL_AMBIENT =
            event("entity.meridian_sentinel.ambient");
    public static final RegistryEntry<SoundEvent> MERIDIAN_SENTINEL_HURT =
            event("entity.meridian_sentinel.hurt");

    public static final RegistryEntry<SoundEvent> HOUR_CANTOR_INTONE =
            event("entity.hour_cantor.intone");
    public static final RegistryEntry<SoundEvent> HOUR_CANTOR_REFRAIN =
            event("entity.hour_cantor.refrain");
    public static final RegistryEntry<SoundEvent> HOUR_CANTOR_AMBIENT =
            event("entity.hour_cantor.ambient");
    public static final RegistryEntry<SoundEvent> HOUR_CANTOR_HURT =
            event("entity.hour_cantor.hurt");

    private ModSounds() {
    }

    private static RegistryEntry<SoundEvent> event(String name) {
        return ModRegistries.SOUNDS.register(
                name,
                () -> SoundEvent.createVariableRangeEvent(
                        Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, name)));
    }

    /** Touching this class is what queues every sound; the loader flushes later. */
    public static void register() {
    }
}
