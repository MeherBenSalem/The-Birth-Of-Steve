package com.nightbeam.tbos.client.rpg;

import com.nightbeam.tbos.client.GreekGui;
import com.nightbeam.tbos.rpg.RpgSounds;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * A small immediate-mode toolkit over {@link Canvas}: buttons register their hit
 * area as they draw, and a click is matched against the last frame's areas. It
 * keeps the 1.0 screens free of vanilla widget classes, whose APIs change on every
 * target.
 */
public final class RpgUi {
    public static final int INK = GreekGui.INK;
    public static final int MUTED = GreekGui.MUTED;
    public static final int TEAL = GreekGui.ACCENT;
    public static final int BRONZE = GreekGui.BRONZE;
    public static final int LIGHT = GreekGui.LIGHT;
    public static final int LIGHT_TEAL = GreekGui.LIGHT_TEAL;
    public static final int GOLD = 0xFFE6B85C;
    public static final int EMBER = 0xFFD9583B;
    public static final int DIM = 0xFFA89B86;

    private record Hit(int x, int y, int w, int h, Runnable action, boolean enabled, List<Component> tooltip) {
        boolean contains(double mx, double my) {
            return mx >= x && my >= y && mx < x + w && my < y + h;
        }
    }

    private final List<Hit> hits = new ArrayList<>();
    private final List<Hit> previous = new ArrayList<>();

    /** Call once at the start of every frame. */
    public void begin() {
        previous.clear();
        previous.addAll(hits);
        hits.clear();
    }

    public static boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    /** A Greek-panel button. {@code primary} uses the teal-edged tile and teal label at rest. */
    public boolean button(Canvas c, int x, int y, int w, int h, Component label, int mx, int my,
            boolean enabled, boolean primary, Runnable action) {
        boolean hovered = enabled && over(mx, my, x, y, w, h);
        int tile = !enabled ? GreekGui.DISABLED : hovered ? GreekGui.HOVER : primary ? GreekGui.SELECTED : GreekGui.BUTTON;
        c.panel(tile, x, y, w, h);
        int color = !enabled ? MUTED : primary && !hovered ? TEAL : INK;
        String text = fit(label.getString(), w - 8);
        c.text(text, x + (w - Canvas.width(text)) / 2, y + (h - 8) / 2, color, false);
        hits.add(new Hit(x, y, w, h, action, enabled, null));
        return hovered;
    }

    /** An invisible click area, for cards and rows that draw themselves. */
    public void area(int x, int y, int w, int h, boolean enabled, Runnable action) {
        hits.add(new Hit(x, y, w, h, action, enabled, null));
    }

    /** Runs the topmost enabled area under the pointer. */
    public boolean click(double mx, double my) {
        List<Hit> source = hits.isEmpty() ? previous : hits;
        for (int i = source.size() - 1; i >= 0; i--) {
            Hit hit = source.get(i);
            if (hit.contains(mx, my)) {
                if (hit.enabled && hit.action != null) {
                    Canvas.playUi(RpgSounds.UI_CLICK.get(), 1.0F, 0.6F);
                    hit.action.run();
                }
                return true;
            }
        }
        return false;
    }

    /** A title with the bronze diamond ornaments either side. */
    public static void heading(Canvas c, Component text, int centerX, int y, int color) {
        int w = Canvas.width(text);
        c.text(text, centerX - w / 2, y, color, false);
        c.ornament(1, centerX - w / 2 - 14, y - 2, 11, 11);
        c.ornament(1, centerX + w / 2 + 3, y - 2, 11, 11);
    }

    public static void rule(Canvas c, int x, int y, int w) {
        c.fill(x, y, w, 1, 0x66765021);
    }

    /** Wraps {@code text} into {@code width}; returns the height drawn. */
    public static int paragraph(Canvas c, Component text, int x, int y, int width, int color, int maxLines) {
        List<FormattedCharSequence> lines = Canvas.split(text, width);
        int drawn = 0;
        for (FormattedCharSequence line : lines) {
            if (drawn >= maxLines) {
                break;
            }
            c.text(line, x, y + drawn * 10, color, false);
            drawn++;
        }
        return drawn * 10;
    }

    public static String fit(String text, int width) {
        if (Canvas.width(text) <= width) {
            return text;
        }
        String ellipsis = "…";
        String cut = Canvas.font().plainSubstrByWidth(text, Math.max(0, width - Canvas.width(ellipsis)));
        return cut + ellipsis;
    }

    /**
     * A short name in up to two lines within {@code width}. It shrinks (to no less than
     * 70%) only when a single word cannot fit, so tile labels never end in an ellipsis.
     */
    public static void twoLines(Canvas c, String text, int x, int y, int width, int color, boolean shadow) {
        String[] words = text.split(" ");
        int longest = 0;
        for (String word : words) {
            longest = Math.max(longest, Canvas.width(word));
        }
        float scale = longest > width ? Math.max(0.7F, width / (float) longest) : 1F;
        int room = Math.round(width / scale);
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String next = line.length() == 0 ? word : line + " " + word;
            if (line.length() == 0 || Canvas.width(next) <= room) {
                line.setLength(0);
                line.append(next);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        if (lines.size() > 2) {
            lines = java.util.List.of(lines.get(0), fit(String.join(" ", lines.subList(1, lines.size())), room));
        }
        c.push();
        c.translate(x, y);
        c.scale(scale);
        for (int i = 0; i < lines.size(); i++) {
            c.text(lines.get(i), 0, i * 9, color, shadow);
        }
        c.pop();
    }

    public static String number(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    public static String clock(long seconds) {
        seconds = Math.max(0, seconds);
        if (seconds >= 3600 * 24) {
            return (seconds / 86400) + "d " + (seconds % 86400 / 3600) + "h";
        }
        if (seconds >= 3600) {
            return (seconds / 3600) + "h " + (seconds % 3600 / 60) + "m";
        }
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    public static int alpha(int argb, float alpha) {
        int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0, Math.min(1, alpha)));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    /** ARGB from a 0xRRGGBB colour. */
    public static int opaque(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }
}
