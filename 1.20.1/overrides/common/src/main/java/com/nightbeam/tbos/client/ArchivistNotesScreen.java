package com.nightbeam.tbos.client;

import com.nightbeam.tbos.Yesterglass;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

    private static final ResourceLocation FRAME = sprite("frame");
    private static final ResourceLocation WELL = sprite("well");

    private int page;

    public ArchivistNotesScreen() {
        super(Component.translatable("item.tbos.starter_tome"));
    }

    public static void open() {
        ClientCompat.setScreen(net.minecraft.client.Minecraft.getInstance(), new ArchivistNotesScreen());
    }

    private static ResourceLocation sprite(String name) {
        return new ResourceLocation(Yesterglass.MOD_ID, "textures/gui/sprites/journal/" + name + ".png");
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mid = left() + panelWidth() / 2;
        if (mouseY >= top() && mouseY < top() + panelHeight()) {
            turn(mouseX < mid ? -1 : 1);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int panelLeft = left();
        int panelTop = top();
        graphics.blitNineSliced(FRAME, panelLeft, panelTop, panelWidth, panelHeight, 6, 32, 32, 0, 0);
        graphics.drawCenteredString(font, title, width / 2, panelTop + 10, INK_TEAL);
        int wellX = panelLeft + 12;
        int wellY = panelTop + 24;
        int wellWidth = panelWidth - 24;
        graphics.blitNineSliced(WELL, wellX, wellY, wellWidth, panelHeight - 48, 3, 16, 16, 0, 0);
        graphics.drawWordWrap(
                font,
                Component.translatable("tome.tbos.page." + page),
                wellX + 10,
                wellY + 10,
                wellWidth - 20,
                INK);
        graphics.drawCenteredString(
                font,
                Component.translatable("tome.tbos.page_indicator", page + 1, PAGE_COUNT),
                width / 2,
                panelTop + panelHeight - 18,
                INK_SOFT);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
