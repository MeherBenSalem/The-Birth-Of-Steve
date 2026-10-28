package com.nightbeam.tbos.relic;

import com.nightbeam.tbos.compat.RpgCompat;
import com.nightbeam.tbos.compat.VanillaCompat;
import com.nightbeam.tbos.expedition.Expeditions;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * What worn Relics do. Stat affixes are refreshed as transient modifiers every half
 * second; the rest react to hits, kills and incoming damage through the same hooks
 * Living Memories already uses.
 */
public final class RelicEffects {
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Map<UUID, String> APPLIED = new HashMap<>();
    private static final Map<UUID, Integer> HITS = new HashMap<>();
    private static final Map<UUID, Long> WARD_READY = new HashMap<>();
    private static boolean applyingBonus;

    private RelicEffects() {
    }

    /** Totals of every affix the player has active, plus their signatures. */
    public record Loadout(Map<Affix, Double> totals, java.util.Set<Affix.Signature> signatures) {
        public double get(Affix affix) {
            return Math.min(affix.cap * 2, totals.getOrDefault(affix, 0.0));
        }

        public boolean has(Affix.Signature signature) {
            return signatures.contains(signature);
        }
    }

    public static Loadout loadout(ServerPlayer player) {
        Map<Affix, Double> totals = new EnumMap<>(Affix.class);
        java.util.Set<Affix.Signature> signatures = java.util.EnumSet.noneOf(Affix.Signature.class);
        Relic held = Relic.read(player.getMainHandItem());
        if (held != null && held.weapon()) {
            accumulate(held, totals, signatures);
        }
        for (EquipmentSlot slot : ARMOR) {
            Relic worn = Relic.read(player.getItemBySlot(slot));
            if (worn != null && !worn.weapon()) {
                accumulate(worn, totals, signatures);
            }
        }
        return new Loadout(totals, signatures);
    }

    private static void accumulate(Relic relic, Map<Affix, Double> totals, java.util.Set<Affix.Signature> signatures) {
        relic.affixes().forEach((affix, value) -> totals.merge(affix, value, Double::sum));
        if (relic.signature() != null) {
            signatures.add(relic.signature());
        }
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 10 != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Loadout loadout = loadout(player);
            StringBuilder fingerprint = new StringBuilder();
            loadout.totals.forEach((affix, value) -> fingerprint.append(affix.ordinal()).append(':').append(value).append(';'));
            String key = fingerprint.toString();
            if (key.equals(APPLIED.get(player.getUUID()))) {
                continue;
            }
            APPLIED.put(player.getUUID(), key);
            for (Affix affix : Affix.values()) {
                if (affix.stat == null) {
                    continue;
                }
                double value = loadout.get(affix);
                String id = "relic_" + affix.id();
                if (value <= 0) {
                    RpgCompat.removeModifier(player, affix.stat, id);
                } else {
                    RpgCompat.setModifier(player, affix.stat, id, value, affix.multiply);
                }
            }
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    /** After the player dealt {@code amount} to {@code target}. */
    public static void onAttack(ServerPlayer player, LivingEntity target, float amount) {
        if (applyingBonus || amount <= 0 || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        Loadout loadout = loadout(player);
        if (loadout.totals.isEmpty() && loadout.signatures.isEmpty()) {
            return;
        }
        double lifesteal = loadout.get(Affix.THIRSTING);
        if (lifesteal > 0) {
            player.heal((float) (amount * Math.min(Affix.THIRSTING.cap, lifesteal)));
        }
        if (player.getRandom().nextDouble() < loadout.get(Affix.SMOLDERING)) {
            VanillaCompat.memoryBurn(target, 3);
        }
        double bonus = 0;
        if (Expeditions.isArchiveCreature(target)) {
            bonus += loadout.get(Affix.ARCHIVE_BREAKING);
        }
        if (target.getHealth() < target.getMaxHealth() * 0.3F) {
            bonus += loadout.get(Affix.REAPING);
        }
        if (loadout.has(Affix.Signature.CHOIRS_ECHO)) {
            int hits = HITS.merge(player.getUUID(), 1, Integer::sum);
            if (hits % 5 == 0) {
                bonus += 1.0;
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.NOTE,
                        target.getX(), target.getY() + target.getBbHeight() + 0.4, target.getZ(), 3, 0.3, 0.1, 0.3, 0.6);
            }
        }
        if (player.getRandom().nextDouble() < loadout.get(Affix.ECHOING)) {
            bonus += 0.4;
            RpgCompat.dust(level, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 0x8FE3E8, 1.1F, 8, 0.3, 0.02);
        }
        if (bonus > 0 && target.isAlive()) {
            strike(level, target, player, (float) (amount * bonus));
        }
    }

    /** Extra damage that does not trigger these effects again or get swallowed by hurt immunity. */
    public static void strike(ServerLevel level, LivingEntity target, ServerPlayer player, float amount) {
        if (amount <= 0 || applyingBonus) {
            return;
        }
        applyingBonus = true;
        try {
            target.invulnerableTime = 0;
            VanillaCompat.memoryDamage(level, target, player, amount);
        } finally {
            applyingBonus = false;
        }
    }

    public static boolean applyingBonus() {
        return applyingBonus;
    }

    /** Damage the player is about to take; returns the adjusted amount. */
    public static float onIncoming(ServerPlayer player, DamageSource source, float amount) {
        if (amount <= 0) {
            return amount;
        }
        Loadout loadout = loadout(player);
        if (loadout.has(Affix.Signature.LAST_HOUR) && player.getHealth() < player.getMaxHealth() * 0.3F) {
            amount *= 0.75F;
        }
        double reflect = loadout.get(Affix.MIRRORED);
        if (reflect > 0 && source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker
                && attacker != player && player.level() instanceof ServerLevel level) {
            float reflected = (float) (amount * Math.min(Affix.MIRRORED.cap, reflect));
            if (reflected > 0.25F) {
                applyingBonus = true;
                try {
                    attacker.invulnerableTime = 0;
                    VanillaCompat.memoryDamage(level, attacker, null, reflected);
                } finally {
                    applyingBonus = false;
                }
            }
        }
        return amount;
    }

    /** The player killed {@code victim}. */
    public static void onKill(ServerPlayer player, LivingEntity victim) {
        Loadout loadout = loadout(player);
        double ward = loadout.get(Affix.WARDED);
        long now = player.level().getGameTime();
        if (ward > 0 && WARD_READY.getOrDefault(player.getUUID(), 0L) <= now) {
            WARD_READY.put(player.getUUID(), now + 200L);
            player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), (float) Math.min(Affix.WARDED.cap, ward)));
        }
        if (loadout.has(Affix.Signature.FRACTURE_STEP)) {
            RpgCompat.addEffect(player, RpgCompat.Effect.SPEED, 60, 1, true);
        }
        if (loadout.has(Affix.Signature.YESTERGLASS)) {
            Expeditions.extendHourglass(player, 5);
        }
    }

    /** Renown multiplier bonus from Storied affixes. */
    public static double renownBonus(ServerPlayer player) {
        return Math.min(Affix.STORIED.cap, loadout(player).get(Affix.STORIED));
    }

    public static void forget(UUID player) {
        APPLIED.remove(player);
        HITS.remove(player);
        WARD_READY.remove(player);
    }

    public static void clear() {
        APPLIED.clear();
        HITS.clear();
        WARD_READY.clear();
    }
}
