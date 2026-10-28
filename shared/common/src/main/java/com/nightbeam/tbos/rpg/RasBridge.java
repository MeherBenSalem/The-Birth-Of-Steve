package com.nightbeam.tbos.rpg;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.compat.RpgCompat;
import com.nightbeam.tbos.platform.Services;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * RPG Attribute System, reached by reflection so this mod runs fine without it.
 * Remembering an expedition grants Valor through {@code LevelingService.addXp}, or
 * through the {@code /ras xp} command when that method cannot be found.
 */
public final class RasBridge {
    public static final String MOD_ID = "rpg_attribute_system";
    private static boolean resolved;
    private static boolean present;
    private static MethodHandle addXp;

    private RasBridge() {
    }

    public static boolean present() {
        resolve();
        return present;
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            present = Services.PLATFORM.isModLoaded(MOD_ID);
        } catch (RuntimeException exception) {
            present = false;
        }
        if (!present) {
            return;
        }
        try {
            Class<?> leveling = Class.forName("tn.nightbeam.ras.procedures.LevelingService");
            addXp = MethodHandles.publicLookup().findStatic(leveling, "addXp",
                    MethodType.methodType(void.class, Entity.class, double.class));
        } catch (ReflectiveOperationException | LinkageError exception) {
            Yesterglass.LOGGER.info("RPG Attribute System found; granting Valor through /ras xp ({})", exception.toString());
        }
        Yesterglass.LOGGER.info("RPG Attribute System found: Remembered expeditions grant Valor");
    }

    public static void addValor(ServerPlayer player, double amount) {
        resolve();
        if (!present || !(amount > 0) || !Double.isFinite(amount)) {
            return;
        }
        if (addXp != null) {
            try {
                addXp.invokeExact((Entity) player, amount);
                return;
            } catch (Throwable throwable) {
                Yesterglass.LOGGER.debug("RAS addXp failed, falling back to /ras xp", throwable);
            }
        }
        var server = player.level().getServer();
        String command = String.format(Locale.ROOT, "ras xp %.2f %s", amount, RpgCompat.playerName(player));
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
    }
}
