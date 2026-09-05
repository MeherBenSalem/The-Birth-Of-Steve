package com.nightbeam.tbos.client;

import com.nightbeam.tbos.network.payload.ArchiveQuestPayload;
import com.nightbeam.tbos.run.ArchiveDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Top-left Cantor Seal card using 1.20.1's immediate GuiGraphics API. */
public final class ArchiveQuestHud {
    private static final long STALE_NANOS = 3_000_000_000L;
    private static ArchiveQuestPayload progress;
    private static long receivedAtNanos;

    private ArchiveQuestHud() {
    }

    public static void accept(ArchiveQuestPayload payload) {
        progress = payload;
        receivedAtNanos = System.nanoTime();
    }

    public static void render(GuiGraphics graphics, float partialTick) {
        MemoryHud.render(graphics);
        Minecraft minecraft = Minecraft.getInstance();
        if (progress == null
                || minecraft.player == null
                || minecraft.level == null
                || !minecraft.level.dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE)
                || System.nanoTime() - receivedAtNanos > STALE_NANOS
                || ClientCompat.isHudHidden(minecraft)
                || ModKeyMappings.objectivesHidden()) {
            return;
        }
        int x = 8;
        int y = 8;
        int width = 172;
        int height = 61;
        int accent = progress.complete() ? 0xFFE0B85B : 0xFF397F80;
        graphics.fill(x, y, x + width, y + height, 0xD0121822);
        graphics.renderOutline(x, y, width, height, accent);
        graphics.fill(x + 1, y + 1, x + 4, y + height - 1, accent);
        graphics.drawString(minecraft.font, Component.translatable("quest.tbos.cantor_seal"), x + 9, y + 6, 0xFFE2D5B4, true);
        graphics.drawString(
                minecraft.font,
                Component.translatable("quest.tbos.cantor_seal.rooms", progress.roomsCleared(), progress.roomsRequired()),
                x + 9,
                y + 20,
                0xFF72D5D2,
                false);
        graphics.drawString(
                minecraft.font,
                Component.translatable(
                        "quest.tbos.cantor_seal.wardens", progress.lesserBossesDefeated(), progress.lesserBossesTotal()),
                x + 9,
                y + 32,
                0xFF72D5D2,
                false);
        int barWidth = 96;
        int filled = progress.roomsRequired() == 0
                ? barWidth
                : Math.min(barWidth, Math.round(barWidth * progress.roomsCleared() / (float) progress.roomsRequired()));
        graphics.fill(x + 9, y + 45, x + 9 + barWidth, y + 49, 0xFF252D36);
        graphics.fill(x + 9, y + 45, x + 9 + filled, y + 49, accent);
    }
}
