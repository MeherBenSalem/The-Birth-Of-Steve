package com.nightbeam.tbos.client;

import com.nightbeam.tbos.config.YesterglassClientConfig;
import com.nightbeam.tbos.network.payload.ArchiveQuestPayload;
import com.nightbeam.tbos.run.ArchiveDimensions;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Top-left objective card for the run-wide Cantor Seal gate. */
public final class ArchiveQuestHud {
    private static final long STALE_NANOS = 3_000_000_000L;
    private static final long PULSE_NANOS = 3_200_000_000L;
    private static ArchiveQuestPayload progress;
    private static long receivedAtNanos;
    private static long completionStartedNanos = Long.MIN_VALUE;
    private static boolean celebrating;
    private static UUID retiredRun;

    private ArchiveQuestHud() {
    }

    public static void accept(ArchiveQuestPayload payload) {
        long now = System.nanoTime();
        receivedAtNanos = now;
        if (payload.runId().equals(retiredRun)) {
            dismiss();
            return;
        }
        boolean sameRun = progress != null && progress.runId().equals(payload.runId());
        if (payload.complete()) {
            if (!sameRun) {
                retiredRun = payload.runId();
                dismiss();
                return;
            }
            if (!progress.complete()) {
                completionStartedNanos = now;
                celebrating = true;
            }
        }
        progress = payload;
    }

    private static void dismiss() {
        progress = null;
        celebrating = false;
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        long now = System.nanoTime();
        if (progress == null
                || minecraft.player == null
                || minecraft.level == null
                || !minecraft.level.dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE)
                || now - receivedAtNanos > STALE_NANOS
                || ClientCompat.isHudHidden(minecraft)
                || ModKeyMappings.objectivesHidden()) {
            return;
        }
        if (progress.complete()
                && (!celebrating || now - completionStartedNanos >= PULSE_NANOS)) {
            retiredRun = progress.runId();
            dismiss();
            return;
        }

        int x = 8;
        int y = 8;
        int width = 172;
        int height = 61;
        int teal = 0xFF397F80;
        int cyan = 0xFF72D5D2;
        int parchment = 0xFFE2D5B4;
        int gold = 0xFFE0B85B;
        graphics.fill(x, y, x + width, y + height, 0xD0121822);
        graphics.outline(x, y, width, height, progress.complete() ? gold : teal);
        graphics.fill(x + 1, y + 1, x + 4, y + height - 1, progress.complete() ? gold : teal);

        graphics.text(
                minecraft.font,
                Component.translatable("quest.tbos.cantor_seal"),
                x + 9,
                y + 6,
                progress.complete() ? gold : parchment,
                true);
        graphics.text(
                minecraft.font,
                Component.translatable(
                        "quest.tbos.cantor_seal.rooms",
                        progress.roomsCleared(),
                        progress.roomsRequired()),
                x + 9,
                y + 19,
                cyan,
                false);
        graphics.text(
                minecraft.font,
                Component.translatable(
                        "quest.tbos.cantor_seal.wardens",
                        progress.lesserBossesDefeated(),
                        progress.lesserBossesTotal()),
                x + 9,
                y + 30,
                cyan,
                false);

        int barX = x + 9;
        int barY = y + 43;
        int barWidth = 96;
        int filled = progress.roomsRequired() == 0
                ? barWidth
                : Math.min(
                        barWidth,
                        Math.round(barWidth * progress.roomsCleared() / (float) progress.roomsRequired()));
        graphics.fill(barX, barY, barX + barWidth, barY + 4, 0xFF252D36);
        graphics.fill(barX, barY, barX + filled, barY + 4, progress.complete() ? gold : teal);
        graphics.text(
                minecraft.font,
                Component.translatable(progress.complete()
                        ? "quest.tbos.cantor_seal.open"
                        : "quest.tbos.cantor_seal.locked"),
                x + 111,
                y + 40,
                progress.complete() ? gold : 0xFF9CA4AA,
                false);

        boolean reducedMotion = YesterglassClientConfig.REDUCED_MOTION.getAsBoolean();
        long elapsed = now - completionStartedNanos;
        if (!reducedMotion && elapsed >= 0L && elapsed < PULSE_NANOS) {
            float phase = elapsed / (float) PULSE_NANOS;
            int sweepX = x + 4 + Math.round((width - 8) * phase);
            graphics.fill(sweepX, y + 2, Math.min(x + width - 2, sweepX + 8), y + height - 2, 0x48E0B85B);
            int inset = Math.min(4, Math.round(phase * 5.0F));
            graphics.outline(x - inset, y - inset, width + inset * 2, height + inset * 2, gold);
        }
    }
}
