package com.nightbeam.tbos.client;

import com.nightbeam.tbos.network.payload.ArchivePuzzlePayload;
import com.nightbeam.tbos.run.ArchiveDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** The active Archive puzzle card for 1.21.1's HUD callback shape. */
public final class ArchivePuzzleHud {
    private static final long STALE_NANOS = 2_000_000_000L;
    private static ArchivePuzzlePayload puzzle;
    private static long receivedAtNanos;

    private ArchivePuzzleHud() {
    }

    public static void accept(ArchivePuzzlePayload payload) {
        puzzle = payload.kind() == ArchivePuzzlePayload.PuzzleKind.NONE ? null : payload;
        receivedAtNanos = System.nanoTime();
    }

    public static void render(GuiGraphics graphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (puzzle == null
                || minecraft.player == null
                || minecraft.level == null
                || !minecraft.level.dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE)
                || System.nanoTime() - receivedAtNanos > STALE_NANOS
                || ClientCompat.isHudHidden(minecraft)
                || ModKeyMappings.objectivesHidden()) {
            return;
        }
        int x = 8;
        int y = 76;
        int width = 172;
        int height = 72;
        int accent = puzzle.state() == ArchivePuzzlePayload.PuzzleState.COMPLETE
                ? 0xFFE0B85B
                : puzzle.state() == ArchivePuzzlePayload.PuzzleState.COMBAT ? 0xFFC95763 : 0xFF397F80;
        graphics.fill(x, y, x + width, y + height, 0xE8111620);
        graphics.renderOutline(x, y, width, height, accent);
        graphics.drawString(
                minecraft.font,
                Component.translatable(puzzle.kind() == ArchivePuzzlePayload.PuzzleKind.HALL
                        ? "puzzle.tbos.hall.title"
                        : "puzzle.tbos.choir.title"),
                x + 9,
                y + 6,
                0xFFE2D5B4,
                true);
        graphics.drawString(
                minecraft.font,
                Component.translatable("puzzle.tbos.progress", puzzle.progress(), puzzle.progressTotal()),
                x + 9,
                y + 18,
                0xFF72D5D2,
                false);
        for (int index = 0; index < puzzle.glyphs().size(); index++) {
            int glyphX = x + 9 + index * 23;
            boolean lit = index < puzzle.progress() || puzzle.state() == ArchivePuzzlePayload.PuzzleState.COMPLETE;
            graphics.fill(glyphX, y + 34, glyphX + 18, y + 52, lit ? 0xCC2B595B : 0xCC202832);
            graphics.renderOutline(glyphX, y + 34, 18, 18, lit ? 0xFFE0B85B : 0xFF397F80);
            String glyph = puzzle.kind() == ArchivePuzzlePayload.PuzzleKind.CHOIR
                    ? new String[] {"I", "II", "III", "IV"}[puzzle.glyphs().get(index)]
                    : new String[] {"N", "E", "S", "W"}[puzzle.glyphs().get(index)];
            graphics.drawString(minecraft.font, glyph, glyphX + (18 - minecraft.font.width(glyph)) / 2, y + 39, 0xFFFFFFFF, false);
        }
        if (puzzle.failures() > 0) {
            graphics.drawString(minecraft.font, Component.translatable("puzzle.tbos.failures", puzzle.failures()), x + 9, y + 58, 0xFFC95763, false);
        }
    }
}
