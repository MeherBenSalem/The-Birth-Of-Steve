package com.nightbeam.tbos.rpg;

import com.nightbeam.tbos.compat.RpgCompat;
import com.nightbeam.tbos.platform.registry.ModRegistries;
import com.nightbeam.tbos.platform.registry.RegistryEntry;
import net.minecraft.sounds.SoundEvent;

/**
 * Sound events added in 1.0. Every one is an original recording processed for the
 * game (see {@code tools/sounds/}); {@code sounds.json} maps each to its files and
 * subtitle.
 */
public final class RpgSounds {
    public static final RegistryEntry<SoundEvent> EXPEDITION_ATTUNE = event("expedition.attune");
    public static final RegistryEntry<SoundEvent> EXPEDITION_DISTORTION = event("expedition.distortion");
    public static final RegistryEntry<SoundEvent> EXPEDITION_BEGIN = event("expedition.begin");
    public static final RegistryEntry<SoundEvent> EXPEDITION_RECOLLECTION = event("expedition.recollection");
    public static final RegistryEntry<SoundEvent> EXPEDITION_RECORDED = event("expedition.recorded");
    public static final RegistryEntry<SoundEvent> EXPEDITION_KEY_RISE = event("expedition.key_rise");
    public static final RegistryEntry<SoundEvent> EXPEDITION_REMEMBER = event("expedition.remember");
    public static final RegistryEntry<SoundEvent> EXPEDITION_FORGET = event("expedition.forget");
    public static final RegistryEntry<SoundEvent> EXPEDITION_FORGOTTEN = event("expedition.forgotten");
    public static final RegistryEntry<SoundEvent> HOURGLASS_TICK = event("expedition.hourglass_tick");
    public static final RegistryEntry<SoundEvent> HOURGLASS_END = event("expedition.hourglass_end");
    public static final RegistryEntry<SoundEvent> RELIC_COMMON = event("relic.reveal_common");
    public static final RegistryEntry<SoundEvent> RELIC_RARE = event("relic.reveal_rare");
    public static final RegistryEntry<SoundEvent> RELIC_EPIC = event("relic.reveal_epic");
    public static final RegistryEntry<SoundEvent> RELIC_LEGENDARY = event("relic.reveal_legendary");
    public static final RegistryEntry<SoundEvent> RELIC_REMNANT = event("relic.reveal_remnant");
    public static final RegistryEntry<SoundEvent> CODEX_OPEN = event("codex.open");
    public static final RegistryEntry<SoundEvent> CODEX_PAGE = event("codex.page");
    public static final RegistryEntry<SoundEvent> CODEX_INSIGHT = event("codex.insight");
    public static final RegistryEntry<SoundEvent> CODEX_RENOWN = event("codex.renown");
    public static final RegistryEntry<SoundEvent> PARTY_INVITE = event("party.invite");
    public static final RegistryEntry<SoundEvent> PARTY_JOIN = event("party.join");
    public static final RegistryEntry<SoundEvent> DISTORTION_BOLSTER = event("distortion.bolster");
    public static final RegistryEntry<SoundEvent> DISTORTION_VOLATILE_HISS = event("distortion.volatile_hiss");
    public static final RegistryEntry<SoundEvent> DISTORTION_VOLATILE_BURST = event("distortion.volatile_burst");
    public static final RegistryEntry<SoundEvent> DISTORTION_HAUNT = event("distortion.haunt");
    public static final RegistryEntry<SoundEvent> UI_CLICK = event("ui.click");

    private RpgSounds() {
    }

    private static RegistryEntry<SoundEvent> event(String name) {
        return ModRegistries.SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(RpgCompat.id(name)));
    }

    /** Touching this class queues every sound; the loader flushes later. */
    public static void register() {
    }
}
