package com.nightbeam.tbos.client;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.advancement.ModAdvancements;
import com.nightbeam.tbos.network.payload.JournalQuestRequest;
import com.nightbeam.tbos.network.payload.JournalQuestSnapshotPayload;
import com.nightbeam.tbos.platform.Services;
import com.nightbeam.tbos.registry.ModItems;
import com.nightbeam.tbos.run.ArchiveFloorPresentation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/** Server-synchronized Story and Archive Run pages for the Archivist's Journal. */
public final class ArchivistQuestScreen extends Screen {
    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 244;
    private int tabWidth() { return (panelWidth() - 36) / 3; }
    private static final int TAB_HEIGHT = 20;
    private static final int ROW_HEIGHT = 22;
    /** Below this pitch the 16px item icon no longer clears its neighbours. */
    private static final int ICON_ROW_HEIGHT = 18;
    private static final int MIN_ROW_HEIGHT = 14;
    /** Progress caption (9) + gap + bar (8) + bottom margin. Reserved before any list is laid out. */
    private static final int FOOTER_HEIGHT = 30;
    private static final int GLYPH = 12;

    private static final ResourceLocation FRAME = sprite("frame");
    private static final ResourceLocation WELL = sprite("well");
    private static final ResourceLocation TAB_ACTIVE = sprite("tab_active");
    private static final ResourceLocation TAB_IDLE = sprite("tab_idle");
    private static final ResourceLocation BAR_TRACK = sprite("bar_track");
    private static final ResourceLocation BAR_FILL = sprite("bar_fill");
    private static final ResourceLocation STEP_DONE = sprite("step_done");
    private static final ResourceLocation STEP_ACTIVE = sprite("step_active");
    private static final ResourceLocation STEP_LOCKED = sprite("step_locked");

    /*
     * Ink is chosen against the sprite it lands on, not against the page as a
     * whole: the frame reads #D4C5A2, the well #ECE0C2 and the active tab
     * #CDBD99, but the idle tab is a dark #6B6456. Dark ink on the idle tab is
     * invisible, so it gets its own light pair. Nothing on this screen draws a
     * drop shadow — see ink()/inkCentered()/inkWrapped().
     */
    private static final int INK = 0xFF2E2216;
    private static final int INK_SOFT = 0xFF5D442D;
    private static final int INK_TEAL = 0xFF1E5E60;
    private static final int INK_DONE = 0xFF2F6A57;
    private static final int INK_LOCKED = 0xFF6A5940;
    private static final int TAB_IDLE_INK = 0xFFF2E8CC;
    private static final int TAB_IDLE_HOVER_INK = 0xFFFFF6DE;
    private static final int RULE = 0xFF8A6033;
    private static final int ROW_ACTIVE = 0x33236E70;
    private static final int ROW_HOVER = 0x1A5D442D;
    private static final int PLATE_FILL = 0x1A3A2716;
    private static final int PLATE_RULE = 0x338A6033;

    /**
     * One entry per Story step, in order: the advancement bits that complete it
     * and the item that stands for it in the list.
     */
    private static final int[] STORY_MASKS = {
        ModAdvancements.DISCOVER_FRACTURE_SHRINE_BIT,
        ModAdvancements.OBTAIN_CRACKED_LENS_BIT,
        ModAdvancements.REPAIR_LENS_BIT,
        ModAdvancements.ENTER_MERIDIAN_ARCHIVE_BIT,
        ModAdvancements.FIRST_RECONSTRUCTION_BIT
                | ModAdvancements.HALL_ALIGNMENT_BIT
                | ModAdvancements.CHOIR_OF_HOURS_BIT
                | ModAdvancements.BROKEN_MERIDIAN_BIT,
        ModAdvancements.LAST_CURATOR_BIT,
        ModAdvancements.ENTER_FRACTURED_ARCHIVE_BIT
    };
    /** The four-part step is the only one that reports sub-progress. */
    private static final int ROOMS_STEP = 4;
    /** Number of statistic rows drawn by the Archive Run page. */
    private static final int RUN_PLATES = 6;

    private JournalQuestSnapshotPayload snapshot;
    private Tab tab = Tab.STORY;
    private int refreshTicks;
    private static boolean awaitingOpen;
    private static boolean openingStory=true;

    private ArchivistQuestScreen(JournalQuestSnapshotPayload snapshot) {
        super(Component.translatable("journal.tbos.quests.title"));
        this.snapshot = snapshot;
    }

    private static ResourceLocation sprite(String name) {
        return ResourceLocation.fromNamespaceAndPath(Yesterglass.MOD_ID, "journal/" + name);
    }

    private static ItemStack icon(int step) {
        return switch (step) {
            case 0 -> new ItemStack(ModItems.RIFT_THRESHOLD.get());
            case 1 -> new ItemStack(ModItems.CRACKED_YESTERGLASS_LENS.get());
            case 2 -> new ItemStack(ModItems.YESTERGLASS_LENS.get());
            case 3 -> new ItemStack(ModItems.ARCHIVE_SURVEY_MAP.get());
            case 4 -> new ItemStack(ModItems.ALIGNMENT_DIAL.get());
            case 5 -> new ItemStack(ModItems.CURATOR_CORE.get());
            default -> new ItemStack(ModItems.ARCHIVE_CORE.get());
        };
    }

    public static void requestOpen() {
        openingStory=true;
        awaitingOpen = true;
        Services.NETWORK.sendToServer(JournalQuestRequest.INSTANCE);
    }

    public static void requestChapter(boolean story) {
        requestOpen();openingStory=story;
    }

    public static void accept(JournalQuestSnapshotPayload snapshot) {
        if (ClientCompat.currentScreen(MinecraftHolder.instance()) instanceof ArchivistQuestScreen screen) {
            screen.snapshot = snapshot;
            return;
        }
        if (!awaitingOpen) {
            return;
        }
        awaitingOpen = false;
        ArchivistQuestScreen opened=new ArchivistQuestScreen(snapshot);
        opened.tab=openingStory?Tab.STORY:Tab.RUN;
        ClientCompat.setScreen(MinecraftHolder.instance(),opened);
    }

    /*
     * Every string on the page is ink on parchment, so none of them take a drop
     * shadow: the shadow is the text colour scaled to 25% and offset one pixel,
     * which on a light page reads as a second, smeared copy of the glyph rather
     * than as depth. The shadowed overloads are never called directly.
     */
    private void ink(GuiGraphics graphics, Component text, int x, int y, int color) {
        graphics.drawString(font, text, x, y, color, false);
    }

    private void ink(GuiGraphics graphics, FormattedCharSequence text, int x, int y, int color) {
        graphics.drawString(font, text, x, y, color, false);
    }

    private void inkCentered(GuiGraphics graphics, Component text, int centerX, int y, int color) {
        FormattedCharSequence line = text.getVisualOrderText();
        graphics.drawString(font, line, centerX - font.width(line) / 2, y, color, false);
    }

    private void inkWrapped(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
        graphics.drawWordWrap(font, text, x, y, width, color);
    }

    /** Trims a label to {@code maxWidth} so it can never run under the column beside it. */
    private FormattedCharSequence fitted(Component text, int maxWidth) {
        FormattedCharSequence line = text.getVisualOrderText();
        if (font.width(line) <= maxWidth) {
            return line;
        }
        int budget = Math.max(0, maxWidth - font.width("..."));
        return FormattedCharSequence.forward(
                font.plainSubstrByWidth(text.getString(), budget) + "...", Style.EMPTY);
    }

    // Geometry is derived on demand rather than cached, so layout and hit-testing
    // cannot disagree after a resize.
    private int panelWidth() {
        return Math.min(PANEL_WIDTH, width - 16);
    }

    private int panelHeight() {
        return Math.min(PANEL_HEIGHT, height - 16);
    }

    private int left() {
        return (width - panelWidth()) / 2;
    }

    private int top() {
        return (height - panelHeight()) / 2;
    }

    private int tabX(Tab which) {
        return left() + 14 + which.ordinal() * (tabWidth() + 4);
    }

    private int tabY() {
        return top() + 24;
    }

    private boolean overTab(Tab which, double mouseX, double mouseY) {
        int x = tabX(which);
        return mouseX >= x && mouseX < x + tabWidth()
                && mouseY >= tabY() && mouseY < tabY() + TAB_HEIGHT;
    }

    @Override
    public void tick() {
        if (++refreshTicks >= 20) {
            refreshTicks = 0;
            Services.NETWORK.sendToServer(JournalQuestRequest.INSTANCE);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Tab candidate : Tab.values()) {
            if (overTab(candidate, mouseX, mouseY)) {
                if (candidate == Tab.MEMORIES) {
                    MemoryClient.request(6,0,0);
                    ClientCompat.setScreen(MinecraftHolder.instance(),new MemoryScreen(this));
                    return true;
                }
                if (candidate != tab) {
                    tab = candidate;
                    minecraft.getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // In 1.21.1 Screen.render() applies the world blur. It must run before
        // the journal is composed; calling it afterward blurs the parchment,
        // text, icons, and progress bars along with the world behind them.
        super.render(graphics, mouseX, mouseY, partialTick);
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int panelLeft = left();
        int panelTop = top();
        graphics.blitSprite(FRAME, panelLeft, panelTop, panelWidth, panelHeight);
        inkCentered(graphics, title, width / 2, panelTop + 10, INK);

        drawTabs(graphics, mouseX, mouseY);

        int wellX = panelLeft + 12;
        int wellY = tabY() + TAB_HEIGHT;
        int wellWidth = panelWidth - 24;
        int wellHeight = panelHeight - (wellY - panelTop) - 12;
        graphics.blitSprite(WELL, wellX, wellY, wellWidth, wellHeight);

        if (tab == Tab.STORY) {
            drawStory(graphics, mouseX, mouseY, wellX, wellY, wellWidth, wellHeight);
        } else {
            drawRun(graphics, wellX, wellY, wellWidth, wellHeight);
        }
    }

    private void drawTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        for (Tab candidate : Tab.values()) {
            boolean active = candidate == tab;
            int x = tabX(candidate);
            int y = tabY() + (active ? 0 : 2);
            int tabHeight = TAB_HEIGHT - (active ? 0 : 2);
            graphics.blitSprite(active ? TAB_ACTIVE : TAB_IDLE, x, y, tabWidth(), tabHeight);
            boolean hovered = overTab(candidate, mouseX, mouseY);
            FormattedCharSequence label=fitted(Component.translatable(candidate.key),tabWidth()-8);
            ink(graphics,label,x+(tabWidth()-font.width(label))/2,y+tabHeight/2-4,
                    active ? INK : (hovered ? TAB_IDLE_HOVER_INK : TAB_IDLE_INK));
        }
    }

    private void drawStory(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            int wellX,
            int wellY,
            int wellWidth,
            int wellHeight) {
        int completed = 0;
        int current = -1;
        for (int index = 0; index < STORY_MASKS.length; index++) {
            if (isComplete(index)) {
                completed++;
            } else if (current < 0) {
                current = index;
            }
        }
        if (completed == STORY_MASKS.length) {
            current = STORY_MASKS.length - 1;
        }

        int listX = wellX + 8;
        int listY = wellY + 10;
        int listWidth = wellWidth / 2 - 18;
        // The footer is reserved before the list is measured, so a shrunk panel
        // compresses the rows instead of running them through the progress bar.
        int listBottom = wellY + wellHeight - FOOTER_HEIGHT;
        int rowHeight = Mth.clamp(
                (listBottom - listY) / STORY_MASKS.length, MIN_ROW_HEIGHT, ROW_HEIGHT);
        boolean showIcons = rowHeight >= ICON_ROW_HEIGHT;
        int textX = listX + GLYPH + (showIcons ? 24 : 6);
        int counterX = listX + listWidth - 20;
        int hoveredStep = -1;
        for (int index = 0; index < STORY_MASKS.length; index++) {
            boolean complete = isComplete(index);
            boolean active = !complete && index == current;
            int y = listY + index * rowHeight;
            boolean hovered = mouseX >= listX - 4 && mouseX < listX + listWidth
                    && mouseY >= y - 3 && mouseY < y + rowHeight - 5;
            if (hovered) {
                hoveredStep = index;
            }
            if (active || hovered) {
                graphics.fill(listX - 5, y - 3, listX + listWidth, y + GLYPH + 3,
                        active ? ROW_ACTIVE : ROW_HOVER);
            }
            graphics.blitSprite(
                    complete ? STEP_DONE : active ? STEP_ACTIVE : STEP_LOCKED,
                    listX,
                    y,
                    GLYPH,
                    GLYPH);
            if (showIcons) {
                graphics.renderItem(icon(index), listX + GLYPH + 4, y - 2);
            }
            ink(
                    graphics,
                    fitted(
                            Component.translatable(
                                    "journal.tbos.quest." + (index + 1) + ".title", index + 1),
                            counterX - 4 - textX),
                    textX,
                    y + 2,
                    complete ? INK_DONE : active ? INK : INK_LOCKED);
            if (index == ROOMS_STEP && !complete) {
                int parts = Integer.bitCount(snapshot.storyMask() & STORY_MASKS[index]);
                ink(graphics, Component.literal(parts + "/4"), counterX, y + 2, INK_SOFT);
            }
        }

        int detailX = wellX + wellWidth / 2 + 4;
        int detailWidth = wellWidth / 2 - 16;
        graphics.vLine(detailX - 10, listY - 4, listBottom, RULE);
        int detail = hoveredStep >= 0 ? hoveredStep : current;
        ink(
                graphics,
                Component.translatable("journal.tbos.quest." + (detail + 1) + ".detail_title"),
                detailX,
                listY,
                INK_TEAL);
        inkWrapped(
                graphics,
                Component.translatable("journal.tbos.quest." + (detail + 1) + ".description"),
                detailX,
                listY + 18,
                detailWidth,
                INK);
        inkWrapped(
                graphics,
                Component.translatable("journal.tbos.quest." + (detail + 1) + ".flavor"),
                detailX,
                listY + 52,
                detailWidth,
                INK_SOFT);

        int barX = wellX + 8;
        int barY = wellY + wellHeight - 16;
        int barWidth = wellWidth - 16;
        ink(
                graphics,
                Component.translatable("journal.tbos.quests.progress", completed, STORY_MASKS.length),
                barX,
                barY - 12,
                INK_SOFT);
        progressBar(graphics, barX, barY, barWidth, completed / (float) STORY_MASKS.length);
    }

    private void drawRun(
            GuiGraphics graphics, int wellX, int wellY, int wellWidth, int wellHeight) {
        int x = wellX + 12;
        int y = wellY + 14;
        int contentBottom = wellY + wellHeight - FOOTER_HEIGHT;
        if (!snapshot.hasRun()) {
            // No progress bar on this branch, so the plate only has to stay
            // inside the well rather than clear a footer.
            int plateTop = y + 16;
            int plateBottom = Math.min(plateTop + 72, wellY + wellHeight - 8);
            graphics.fill(x, plateTop, wellX + wellWidth - 12, plateBottom, 0x26F4E5BC);
            graphics.hLine(x, wellX + wellWidth - 12, plateBottom, PLATE_RULE);
            inkCentered(
                    graphics,
                    Component.translatable("journal.tbos.run.inactive"),
                    wellX + wellWidth / 2,
                    plateTop + 10,
                    INK_TEAL);
            inkWrapped(
                    graphics,
                    Component.translatable("journal.tbos.run.inactive.hint"),
                    x + 12,
                    plateTop + 32,
                    wellWidth - 48,
                    INK);
            return;
        }
        ink(
                graphics,
                Component.translatable(
                        "journal.tbos.run.floor",
                        ArchiveFloorPresentation.displayFloor(snapshot.floorIndex()),
                        ArchiveFloorPresentation.name(snapshot.floorIndex())),
                x,
                y,
                INK_TEAL);

        int plateWidth = wellWidth - 24;
        int plateY = y + 22;
        // Same reservation as the Story list: the six rows compress rather than
        // spilling into the progress bar when the panel is clamped by screen size.
        int platePitch = Mth.clamp((contentBottom - plateY) / RUN_PLATES, MIN_ROW_HEIGHT, 19);
        plateY = statPlate(graphics, x, plateY, plateWidth, platePitch, "journal.tbos.run.state",
                Component.translatable(snapshot.preparing()
                        ? "journal.tbos.run.preparing"
                        : "journal.tbos.run.active"));
        plateY = statPlate(graphics, x, plateY, plateWidth, platePitch, "journal.tbos.run.mode",
                Component.translatable(snapshot.ominous()
                        ? "journal.tbos.run.mode.ominous"
                        : "journal.tbos.run.mode.normal"));
        plateY = statPlate(graphics, x, plateY, plateWidth, platePitch, "journal.tbos.run.revives",
                Component.literal(Integer.toString(snapshot.sharedRevives())));
        plateY = statPlate(graphics, x, plateY, plateWidth, platePitch, "journal.tbos.run.rooms",
                Component.literal(snapshot.roomsCleared() + " / " + snapshot.roomsRequired()));
        plateY = statPlate(graphics, x, plateY, plateWidth, platePitch, "journal.tbos.run.wardens",
                Component.literal(
                        snapshot.lesserBossesDefeated() + " / " + snapshot.lesserBossesTotal()));
        statPlate(graphics, x, plateY, plateWidth, platePitch, "journal.tbos.run.gateway",
                Component.translatable(snapshot.gatewayReady()
                        ? "journal.tbos.run.gateway.ready"
                        : "journal.tbos.run.gateway.locked"));

        float fraction = snapshot.roomsRequired() == 0
                ? 0.0F
                : snapshot.roomsCleared() / (float) snapshot.roomsRequired();
        progressBar(graphics, x, wellY + wellHeight - 16, plateWidth, fraction);
    }

    /** Draws one ruled statistic row and returns the next row's baseline. */
    private int statPlate(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int pitch,
            String key,
            Component value) {
        int rule = y + pitch - 7;
        int valueX = x + Math.min(132, width - 60);
        graphics.fill(x - 4, y - 3, x + width, rule, PLATE_FILL);
        graphics.hLine(x - 4, x + width, rule, PLATE_RULE);
        ink(graphics, fitted(Component.translatable(key), valueX - 4 - x), x, y, INK_SOFT);
        ink(graphics, value, valueX, y, INK_TEAL);
        return y + pitch;
    }

    /**
     * Draws the track, then the fill clipped to its fraction. The fill sprite is
     * nine-sliced, so a zero-width blit would smear its own borders; below one
     * full sprite width the track is left bare instead.
     */
    private void progressBar(GuiGraphics graphics, int x, int y, int width, float fraction) {
        int height = 8;
        graphics.blitSprite(BAR_TRACK, x, y, width, height);
        int filled = Math.round(width * Mth.clamp(fraction, 0.0F, 1.0F));
        if (filled >= height) {
            graphics.blitSprite(BAR_FILL, x, y, filled, height);
        }
    }

    private boolean isComplete(int index) {
        return (snapshot.storyMask() & STORY_MASKS[index]) == STORY_MASKS[index];
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        awaitingOpen = false;
        super.onClose();
    }

    public void openChapter(boolean story) {
        tab=story?Tab.STORY:Tab.RUN;
        ClientCompat.setScreen(MinecraftHolder.instance(),this);
    }

    private enum Tab {
        STORY("journal.tbos.quests.tab.story"),
        RUN("journal.tbos.quests.tab.run"),
        MEMORIES("memory.tbos.chapter");

        private final String key;

        Tab(String key) {
            this.key = key;
        }
    }

    /** Keeps Minecraft client access in one tiny, test-friendly boundary. */
    private static final class MinecraftHolder {
        private MinecraftHolder() {
        }

        private static net.minecraft.client.Minecraft instance() {
            return net.minecraft.client.Minecraft.getInstance();
        }
    }
}
