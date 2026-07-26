package com.nightbeam.tbos.client;

import com.nightbeam.tbos.advancement.ModAdvancements;
import com.nightbeam.tbos.network.payload.JournalQuestRequest;
import com.nightbeam.tbos.network.payload.JournalQuestSnapshotPayload;
import com.nightbeam.tbos.run.ArchiveFloorPresentation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Server-synchronized Story and Archive Run pages for the Archivist's Journal. */
public final class ArchivistQuestScreen extends Screen {
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
    private JournalQuestSnapshotPayload snapshot;
    private Tab tab = Tab.STORY;
    private int refreshTicks;
    private static boolean awaitingOpen;

    private ArchivistQuestScreen(JournalQuestSnapshotPayload snapshot) {
        super(Component.translatable("journal.tbos.quests.title"));
        this.snapshot = snapshot;
    }

    public static void requestOpen() {
        awaitingOpen = true;
        ClientPacketDistributor.sendToServer(JournalQuestRequest.INSTANCE);
    }

    public static void accept(JournalQuestSnapshotPayload snapshot) {
        if (MinecraftHolder.instance().screen instanceof ArchivistQuestScreen screen) {
            screen.snapshot = snapshot;
            return;
        }
        if (!awaitingOpen) {
            return;
        }
        awaitingOpen = false;
        MinecraftHolder.instance().setScreen(new ArchivistQuestScreen(snapshot));
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(374, width - 18);
        int left = (width - panelWidth) / 2;
        int top = Math.max(8, (height - 238) / 2);
        addRenderableWidget(Button.builder(
                        Component.translatable("journal.tbos.quests.tab.story"),
                        button -> tab = Tab.STORY)
                .bounds(left + 16, top + 25, 104, 20)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("journal.tbos.quests.tab.run"),
                        button -> tab = Tab.RUN)
                .bounds(left + 124, top + 25, 116, 20)
                .build());
    }

    @Override
    public void tick() {
        if (++refreshTicks >= 20) {
            refreshTicks = 0;
            ClientPacketDistributor.sendToServer(JournalQuestRequest.INSTANCE);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractTransparentBackground(graphics);
        int panelWidth = Math.min(374, width - 18);
        int panelHeight = Math.min(238, height - 16);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF2D9C89E);
        graphics.outline(left, top, panelWidth, panelHeight, 0xFF6D4A29);
        graphics.outline(left + 3, top + 3, panelWidth - 6, panelHeight - 6, 0xFF9A7040);
        graphics.fill(left + 8, top + 50, left + panelWidth - 8, top + panelHeight - 10, 0x92776548);
        graphics.centeredText(font, title, width / 2, top + 9, 0xFF38291C);
        if (tab == Tab.STORY) {
            drawStory(graphics, left, top, panelWidth, panelHeight);
        } else {
            drawRun(graphics, left, top, panelWidth, panelHeight);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawStory(GuiGraphicsExtractor graphics, int left, int top, int panelWidth, int panelHeight) {
        int completedSteps = 0;
        int current = -1;
        for (int index = 0; index < STORY_MASKS.length; index++) {
            boolean complete = (snapshot.storyMask() & STORY_MASKS[index]) == STORY_MASKS[index];
            if (complete) {
                completedSteps++;
            } else if (current < 0) {
                current = index;
            }
        }
        if (completedSteps == STORY_MASKS.length) {
            current = STORY_MASKS.length - 1;
        }
        int listX = left + 16;
        int listY = top + 60;
        int listWidth = panelWidth / 2 - 24;
        for (int index = 0; index < STORY_MASKS.length; index++) {
            boolean complete = (snapshot.storyMask() & STORY_MASKS[index]) == STORY_MASKS[index];
            boolean active = !complete && index == current;
            int y = listY + index * 19;
            if (active) {
                graphics.fill(listX - 4, y - 3, listX + listWidth + 4, y + 14, 0x553D8383);
            }
            int color = complete ? 0xFF397A63 : active ? 0xFF6E3E80 : 0xFF574839;
            Component marker = Component.literal(complete ? "✓" : active ? "◆" : "◇");
            graphics.text(font, marker, listX, y, color, true);
            graphics.text(
                    font,
                    Component.translatable("journal.tbos.quest." + (index + 1) + ".title", index + 1),
                    listX + 13,
                    y,
                    color,
                    false);
            if (index == 4) {
                int substeps = Integer.bitCount(snapshot.storyMask() & STORY_MASKS[index]);
                graphics.text(font, Component.literal(substeps + "/4"), listX + listWidth - 24, y, color, false);
            }
        }
        int detailX = left + panelWidth / 2 + 5;
        graphics.verticalLine(detailX - 8, listY - 5, top + panelHeight - 30, 0xAA9A7040);
        graphics.text(
                font,
                Component.translatable("journal.tbos.quest." + (current + 1) + ".detail_title"),
                detailX,
                listY,
                0xFF356F70,
                true);
        graphics.textWithWordWrap(
                font,
                Component.translatable("journal.tbos.quest." + (current + 1) + ".description"),
                detailX,
                listY + 18,
                panelWidth / 2 - 26,
                0xFF493727);
        int barX = left + 16;
        int barY = top + panelHeight - 20;
        int barWidth = panelWidth - 32;
        int filled = Math.round(barWidth * completedSteps / (float) STORY_MASKS.length);
        graphics.fill(barX, barY, barX + barWidth, barY + 5, 0xFF5A4A39);
        graphics.fill(barX, barY, barX + filled, barY + 5, 0xFF397F80);
        graphics.text(
                font,
                Component.translatable("journal.tbos.quests.progress", completedSteps, STORY_MASKS.length),
                barX,
                barY - 11,
                0xFF463526,
                false);
    }

    private void drawRun(GuiGraphicsExtractor graphics, int left, int top, int panelWidth, int panelHeight) {
        int x = left + 18;
        int y = top + 62;
        if (!snapshot.hasRun()) {
            graphics.centeredText(
                    font,
                    Component.translatable("journal.tbos.run.inactive"),
                    left + panelWidth / 2,
                    y + 24,
                    0xFF6E3E80);
            graphics.textWithWordWrap(
                    font,
                    Component.translatable("journal.tbos.run.inactive.hint"),
                    x + 16,
                    y + 48,
                    panelWidth - 68,
                    0xFF493727);
            return;
        }
        graphics.text(
                font,
                Component.translatable(
                        "journal.tbos.run.floor",
                        ArchiveFloorPresentation.displayFloor(snapshot.floorIndex()),
                        ArchiveFloorPresentation.name(snapshot.floorIndex())),
                x,
                y,
                0xFF356F70,
                true);
        y += 25;
        drawRunLine(graphics, x, y, "journal.tbos.run.state",
                Component.translatable(snapshot.preparing()
                        ? "journal.tbos.run.preparing"
                        : "journal.tbos.run.active"));
        drawRunLine(graphics, x, y + 21, "journal.tbos.run.revives", Component.literal(
                Integer.toString(snapshot.sharedRevives())));
        drawRunLine(graphics, x, y + 42, "journal.tbos.run.rooms", Component.literal(
                snapshot.roomsCleared() + " / " + snapshot.roomsRequired()));
        drawRunLine(graphics, x, y + 63, "journal.tbos.run.wardens", Component.literal(
                snapshot.lesserBossesDefeated() + " / " + snapshot.lesserBossesTotal()));
        drawRunLine(graphics, x, y + 84, "journal.tbos.run.gateway",
                Component.translatable(snapshot.gatewayReady()
                        ? "journal.tbos.run.gateway.ready"
                        : "journal.tbos.run.gateway.locked"));
        int barWidth = panelWidth - 36;
        int filled = snapshot.roomsRequired() == 0
                ? 0
                : Math.round(barWidth * snapshot.roomsCleared() / (float) snapshot.roomsRequired());
        graphics.fill(x, top + panelHeight - 24, x + barWidth, top + panelHeight - 18, 0xFF5A4A39);
        graphics.fill(x, top + panelHeight - 24, x + filled, top + panelHeight - 18, 0xFF397F80);
    }

    private void drawRunLine(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            String key,
            Component value) {
        graphics.text(font, Component.translatable(key), x, y, 0xFF5D442D, false);
        graphics.text(font, value, x + 118, y, 0xFF2E6F70, true);
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

    private enum Tab {
        STORY,
        RUN
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
