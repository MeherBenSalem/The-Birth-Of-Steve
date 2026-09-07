package com.nightbeam.tbos.client;

import com.nightbeam.tbos.config.YesterglassClientConfig;
import com.nightbeam.tbos.network.payload.ArchivePuzzlePayload;
import com.nightbeam.tbos.run.ArchiveDimensions;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** A stacked objective card that replaces action-bar puzzle instructions. */
public final class ArchivePuzzleHud {
    private static final long STALE_NANOS = 2_000_000_000L;
    private static final long FEEDBACK_NANOS = 900_000_000L;
    private static final long COMPLETE_NANOS = 2_400_000_000L;
    private static final Set<Integer> RETIRED = new HashSet<>();
    private static UUID trackedRun;
    private static ArchivePuzzlePayload puzzle;
    private static long receivedAtNanos;
    private static long failureStartedNanos = Long.MIN_VALUE;
    private static long completionStartedNanos = Long.MIN_VALUE;
    private static boolean celebrating;

    private ArchivePuzzleHud() {
    }

    public static void accept(ArchivePuzzlePayload payload) {
        long now = System.nanoTime();
        receivedAtNanos = now;
        if (!payload.runId().equals(trackedRun)) {
            trackedRun = payload.runId();
            RETIRED.clear();
        }
        if (payload.kind() == ArchivePuzzlePayload.PuzzleKind.NONE
                || RETIRED.contains(payload.roomIndex())) {
            dismiss();
            return;
        }
        boolean sameCard = puzzle != null
                && puzzle.runId().equals(payload.runId())
                && puzzle.roomIndex() == payload.roomIndex();
        if (payload.state() == ArchivePuzzlePayload.PuzzleState.COMPLETE) {
            if (!sameCard) {
                RETIRED.add(payload.roomIndex());
                dismiss();
                return;
            }
            if (puzzle.state() != ArchivePuzzlePayload.PuzzleState.COMPLETE) {
                completionStartedNanos = now;
                celebrating = true;
            }
        }
        if (sameCard && payload.failures() > puzzle.failures()) {
            failureStartedNanos = now;
        }
        puzzle = payload;
    }

    private static void dismiss() {
        puzzle = null;
        celebrating = false;
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        long now = System.nanoTime();
        if (puzzle == null
                || minecraft.player == null
                || minecraft.level == null
                || !minecraft.level.dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE)
                || now - receivedAtNanos > STALE_NANOS
                || ClientCompat.isHudHidden(minecraft)
                || ModKeyMappings.objectivesHidden()) {
            return;
        }
        if (puzzle.state() == ArchivePuzzlePayload.PuzzleState.COMPLETE
                && (!celebrating || now - completionStartedNanos >= COMPLETE_NANOS)) {
            RETIRED.add(puzzle.roomIndex());
            dismiss();
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

        GreekGui.panel(graphics,GreekGui.HUD,x,y,width,height);
        GreekGui.material(graphics,GreekGui.GOLD,x+8,y+26,width-16,1);

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
            GreekGui.panel(graphics,lit?GreekGui.SELECTED:GreekGui.DISABLED,glyphX,glyphY,glyphSize,glyphSize);
            String symbol = glyph(puzzle.kind(), puzzle.glyphs().get(index));
            graphics.text(
                    minecraft.font,
                    Component.literal(symbol),
                    glyphX + (glyphSize - minecraft.font.width(symbol)) / 2,
                    glyphY + 5,
                    lit ? GreekGui.ACCENT : GreekGui.MUTED,
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

        long failureElapsed=now-failureStartedNanos;
        if(failureElapsed>=0L&&failureElapsed<FEEDBACK_NANOS)
            GreekGui.material(graphics,GreekGui.WARNING,x+8,y+height-5,width-16,2);
        if(complete)GreekGui.ornament(graphics,0,x+width-21,y+4,16,16);
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
