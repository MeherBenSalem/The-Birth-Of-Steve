package com.nightbeam.tbos.client;

import com.nightbeam.tbos.config.YesterglassClientConfig;
import com.nightbeam.tbos.network.payload.ArchivePuzzlePayload;
import com.nightbeam.tbos.run.ArchiveDimensions;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** A stacked objective card that replaces action-bar puzzle instructions. */
public final class ArchivePuzzleHud {
    private static final long STALE_NANOS = 2_000_000_000L;
    private static final long FEEDBACK_NANOS = 900_000_000L;
    private static final long COMPLETE_NANOS = 2_400_000_000L;
    private static ArchivePuzzlePayload puzzle;
    private static long receivedAtNanos;
    private static long failureStartedNanos = Long.MIN_VALUE;
    private static long completionStartedNanos = Long.MIN_VALUE;

    private ArchivePuzzleHud() {
    }

    public static void accept(ArchivePuzzlePayload payload) {
        long now = System.nanoTime();
        if (payload.kind() == ArchivePuzzlePayload.PuzzleKind.NONE) {
            puzzle = null;
            receivedAtNanos = now;
            return;
        }
        if (puzzle != null
                && puzzle.runId().equals(payload.runId())
                && puzzle.roomIndex() == payload.roomIndex()) {
            if (payload.failures() > puzzle.failures()) {
                failureStartedNanos = now;
            }
            if (puzzle.state() != ArchivePuzzlePayload.PuzzleState.COMPLETE
                    && payload.state() == ArchivePuzzlePayload.PuzzleState.COMPLETE) {
                completionStartedNanos = now;
            }
        }
        puzzle = payload;
        receivedAtNanos = now;
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        long now = System.nanoTime();
        if (puzzle == null
                || minecraft.player == null
                || minecraft.level == null
                || !minecraft.level.dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE)
                || now - receivedAtNanos > STALE_NANOS
                || minecraft.options.hideGui) {
            return;
        }

        int x = 8;
        int y = 76;
        int width = 172;
        int height = 72;
        int ink = 0xE8111620;
        int teal = 0xFF397F80;
        int cyan = 0xFF72D5D2;
        int parchment = 0xFFE2D5B4;
        int gold = 0xFFE0B85B;
        int danger = 0xFFC95763;
        boolean complete = puzzle.state() == ArchivePuzzlePayload.PuzzleState.COMPLETE;
        boolean combat = puzzle.state() == ArchivePuzzlePayload.PuzzleState.COMBAT;
        int accent = complete ? gold : combat ? danger : teal;

        graphics.fill(x, y, x + width, y + height, ink);
        graphics.outline(x, y, width, height, accent);
        graphics.fill(x + 1, y + 1, x + 4, y + height - 1, accent);
        graphics.fill(x + 8, y + 25, x + width - 8, y + 26, 0x66397F80);

        graphics.text(
                minecraft.font,
                Component.translatable(puzzle.kind() == ArchivePuzzlePayload.PuzzleKind.HALL
                        ? "puzzle.tbos.hall.title"
                        : "puzzle.tbos.choir.title"),
                x + 9,
                y + 6,
                complete ? gold : parchment,
                true);
        Component stateText = switch (puzzle.state()) {
            case WAITING -> Component.translatable("puzzle.tbos.state.waiting");
            case SOLVING -> Component.translatable(
                    "puzzle.tbos.state.solving", puzzle.stage(), puzzle.stageTotal());
            case COMBAT -> Component.translatable(
                    "puzzle.tbos.state.combat", puzzle.stage(), puzzle.stageTotal());
            case COMPLETE -> Component.translatable("puzzle.tbos.state.complete");
        };
        graphics.text(
                minecraft.font,
                stateText,
                x + 9,
                y + 16,
                combat ? danger : cyan,
                false);

        int glyphY = y + 33;
        int glyphSize = 18;
        int gap = 5;
        for (int index = 0; index < puzzle.glyphs().size(); index++) {
            int glyphX = x + 9 + index * (glyphSize + gap);
            boolean lit = index < puzzle.progress() || complete;
            graphics.fill(
                    glyphX,
                    glyphY,
                    glyphX + glyphSize,
                    glyphY + glyphSize,
                    lit ? 0xCC2B595B : 0xCC202832);
            graphics.outline(glyphX, glyphY, glyphSize, glyphSize, lit ? gold : teal);
            String symbol = glyph(puzzle.kind(), puzzle.glyphs().get(index));
            graphics.text(
                    minecraft.font,
                    Component.literal(symbol),
                    glyphX + (glyphSize - minecraft.font.width(symbol)) / 2,
                    glyphY + 5,
                    lit ? 0xFFFFFFFF : 0xFF9CA4AA,
                    false);
        }

        graphics.text(
                minecraft.font,
                Component.translatable(
                        "puzzle.tbos.progress",
                        puzzle.progress(),
                        puzzle.progressTotal()),
                x + 9,
                y + 57,
                complete ? gold : cyan,
                false);
        if (puzzle.failures() > 0) {
            graphics.text(
                    minecraft.font,
                    Component.translatable("puzzle.tbos.failures", puzzle.failures()),
                    x + 111,
                    y + 57,
                    danger,
                    false);
        }

        boolean reducedMotion = YesterglassClientConfig.REDUCED_MOTION.getAsBoolean();
        if (!reducedMotion) {
            long failureElapsed = now - failureStartedNanos;
            if (failureElapsed >= 0L && failureElapsed < FEEDBACK_NANOS) {
                int pulse = Math.max(0, 70 - Math.round(70.0F * failureElapsed / FEEDBACK_NANOS));
                graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, (pulse << 24) | 0x00C95763);
            }
            long completionElapsed = now - completionStartedNanos;
            if (completionElapsed >= 0L && completionElapsed < COMPLETE_NANOS) {
                float phase = completionElapsed / (float) COMPLETE_NANOS;
                int sweepX = x + 4 + Math.round((width - 8) * phase);
                graphics.fill(sweepX, y + 2, Math.min(x + width - 2, sweepX + 10), y + height - 2, 0x55E0B85B);
            }
        }
    }

    private static String glyph(ArchivePuzzlePayload.PuzzleKind kind, int glyph) {
        if (kind == ArchivePuzzlePayload.PuzzleKind.CHOIR) {
            return switch (glyph) {
                case 0 -> "I";
                case 1 -> "II";
                case 2 -> "III";
                default -> "IV";
            };
        }
        return switch (glyph) {
            case 0 -> "N";
            case 1 -> "E";
            case 2 -> "S";
            default -> "W";
        };
    }
}
