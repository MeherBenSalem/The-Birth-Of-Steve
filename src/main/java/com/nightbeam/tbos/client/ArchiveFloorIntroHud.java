package com.nightbeam.tbos.client;

import com.nightbeam.tbos.config.YesterglassClientConfig;
import com.nightbeam.tbos.network.payload.ArchiveFloorIntroPayload;
import com.nightbeam.tbos.run.ArchiveFloorPresentation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/** Four-second, non-blocking title treatment shown after each floor teleport. */
public final class ArchiveFloorIntroHud {
    public static final int DURATION_TICKS = 80;
    private static ArchiveFloorIntroPayload intro;
    private static int ticksRemaining;
    private static int particleTick;

    private ArchiveFloorIntroHud() {
    }

    public static void begin(ArchiveFloorIntroPayload payload) {
        intro = payload;
        ticksRemaining = DURATION_TICKS;
        particleTick = 0;
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.END_PORTAL_SPAWN, 0.82F, 0.72F));
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.18F));
    }

    public static void tick(Minecraft minecraft) {
        if (intro == null || ticksRemaining <= 0) {
            intro = null;
            return;
        }
        ticksRemaining--;
        particleTick++;
        if (YesterglassClientConfig.REDUCED_MOTION.getAsBoolean()
                || minecraft.player == null
                || minecraft.level == null
                || (particleTick & 1) != 0) {
            return;
        }
        int count = Math.max(1, YesterglassClientConfig.EFFECT_QUALITY.getAsInt());
        for (int index = 0; index < count; index++) {
            double angle = (particleTick * 0.28D) + index * (Math.PI * 2.0D / count);
            double radius = 0.7D + index * 0.12D;
            double x = minecraft.player.getX() + Math.cos(angle) * radius;
            double y = minecraft.player.getY() + 0.4D + (particleTick % 14) * 0.08D;
            double z = minecraft.player.getZ() + Math.sin(angle) * radius;
            minecraft.level.addParticle(
                    (index & 1) == 0 ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.END_ROD,
                    x,
                    y,
                    z,
                    -Math.cos(angle) * 0.018D,
                    0.018D,
                    -Math.sin(angle) * 0.018D);
        }
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (intro == null || ticksRemaining <= 0 || minecraft.options.hideGui) {
            return;
        }
        float elapsed = DURATION_TICKS - ticksRemaining + deltaTracker.getGameTimeDeltaPartialTick(false);
        float fadeIn = Mth.clamp(elapsed / 12.0F, 0.0F, 1.0F);
        float fadeOut = Mth.clamp(ticksRemaining / 18.0F, 0.0F, 1.0F);
        float alpha = Math.min(fadeIn, fadeOut);
        boolean reducedMotion = YesterglassClientConfig.REDUCED_MOTION.getAsBoolean();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int centerY = height / 2;
        int darkAlpha = Math.round(126.0F * alpha);

        graphics.fill(0, 0, width, Math.max(24, centerY - 58), darkAlpha << 24);
        graphics.fill(0, Math.min(height, centerY + 54), width, height, darkAlpha << 24);
        graphics.fillGradient(
                0,
                centerY - 58,
                width,
                centerY + 54,
                (Math.round(58.0F * alpha) << 24) | 0x07131A,
                (Math.round(76.0F * alpha) << 24) | 0x160D20);

        int edgeAlpha = Math.round(220.0F * alpha);
        int teal = (edgeAlpha << 24) | 0x4FAEAA;
        int gold = (edgeAlpha << 24) | 0xD7A94A;
        graphics.horizontalLine(0, width, centerY - 38, teal);
        graphics.horizontalLine(0, width, centerY + 36, gold);
        if (!reducedMotion) {
            float sweepProgress = Mth.clamp(elapsed / 38.0F, 0.0F, 1.0F);
            int sweepX = Math.round((width + 64) * sweepProgress) - 64;
            graphics.fill(sweepX, centerY - 37, sweepX + 42, centerY + 36, 0x284FAEAA);
            graphics.fill(sweepX + 42, centerY - 37, sweepX + 48, centerY + 36, 0x50D7A94A);
        }

        Component number = Component.translatable(
                "floor.tbos.intro.title",
                ArchiveFloorPresentation.displayFloor(intro.floorIndex()));
        Component name = ArchiveFloorPresentation.name(intro.floorIndex());
        int textAlpha = Math.max(4, Math.round(255.0F * alpha));
        int numberColor = (textAlpha << 24) | 0xE8D6A7;
        int nameColor = (textAlpha << 24) | 0x74D7D2;
        float scale = reducedMotion ? 1.65F : 1.65F + 0.18F * (1.0F - Mth.clamp(elapsed / 18.0F, 0.0F, 1.0F));
        graphics.pose().pushMatrix();
        graphics.pose().translate(width / 2.0F, centerY - 24.0F);
        graphics.pose().scale(scale, scale);
        graphics.centeredText(minecraft.font, number, 0, 0, numberColor);
        graphics.pose().popMatrix();
        graphics.centeredText(minecraft.font, name, width / 2, centerY + 8, nameColor);
        graphics.centeredText(
                minecraft.font,
                Component.translatable(intro.ominous()
                        ? "floor.tbos.intro.subtitle.ominous"
                        : "floor.tbos.intro.subtitle"),
                width / 2,
                centerY + 23,
                (textAlpha << 24) | (intro.ominous() ? 0xFF6655 : 0xB8A98B));
    }
}
