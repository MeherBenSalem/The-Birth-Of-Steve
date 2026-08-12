package com.nightbeam.tbos.client;

import com.nightbeam.tbos.Yesterglass;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/** Read-only parchment for the Last Archivist's Notes. Six pages, one per Memory Scene. */
public final class ArchivistNotesScreen extends Screen {
    private static final int PAGE_COUNT = 6;
    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 196;
    private static final int INK = 0xFF2E2216;
    private static final int INK_SOFT = 0xFF5D442D;
    private static final int INK_TEAL = 0xFF1E5E60;

    private static final Identifier FRAME = sprite("frame");
    private static final Identifier WELL = sprite("well");

    private int page;

    public ArchivistNotesScreen() {
        super(Component.translatable("item.tbos.starter_tome"));
    }

    public static void open() {
        ClientCompat.setScreen(net.minecraft.client.Minecraft.getInstance(), new ArchivistNotesScreen());
    }

    private static Identifier sprite(String name) {
        return Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "journal/" + name);
    }

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

    private void turn(int delta) {
        int next = Mth.clamp(page + delta, 0, PAGE_COUNT - 1);
        if (next != page) {
            page = next;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mid = left() + panelWidth() / 2;
        if (event.y() >= top() && event.y() < top() + panelHeight()) {
            turn(event.x() < mid ? -1 : 1);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractTransparentBackground(graphics);
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int panelLeft = left();
        int panelTop = top();
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FRAME, panelLeft, panelTop, panelWidth, panelHeight);
        graphics.centeredText(font, title, width / 2, panelTop + 10, INK_TEAL);
        int wellX = panelLeft + 12;
        int wellY = panelTop + 24;
        int wellWidth = panelWidth - 24;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, WELL, wellX, wellY, wellWidth, panelHeight - 48);
        graphics.textWithWordWrap(
                font,
                Component.translatable("tome.tbos.page." + page),
                wellX + 10,
                wellY + 10,
                wellWidth - 20,
                INK,
                false);
        graphics.centeredText(
                font,
                Component.translatable("tome.tbos.page_indicator", page + 1, PAGE_COUNT),
                width / 2,
                panelTop + panelHeight - 18,
                INK_SOFT);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
