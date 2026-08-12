package com.nightbeam.tbos.client;

import com.nightbeam.tbos.config.YesterglassClientConfig;
import com.nightbeam.tbos.network.payload.ArchiveFloorIntroPayload;
import com.nightbeam.tbos.run.ArchiveFloorPresentation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/** Four-second floor title treatment, adapted to the 1.21.1 HUD render callback. */
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
        if (intro == null || ticksRemaining-- <= 0) {
            intro = null;
            return;
        }
        particleTick++;
        if (YesterglassClientConfig.REDUCED_MOTION.getAsBoolean()
                || minecraft.player == null
                || minecraft.level == null
                || (particleTick & 1) != 0) {
            return;
        }
        double angle = particleTick * 0.28D;
        minecraft.level.addParticle(
                ParticleTypes.REVERSE_PORTAL,
                minecraft.player.getX() + Math.cos(angle) * 0.7D,
                minecraft.player.getY() + 0.8D,
                minecraft.player.getZ() + Math.sin(angle) * 0.7D,
                0.0D,
                0.02D,
                0.0D);
    }

    public static void render(GuiGraphics graphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (intro == null || ticksRemaining <= 0 || ClientCompat.isHudHidden(minecraft)) {
            return;
        }
        float elapsed = DURATION_TICKS - ticksRemaining + partialTick;
        float alpha = Math.min(Mth.clamp(elapsed / 12.0F, 0.0F, 1.0F), Mth.clamp(ticksRemaining / 18.0F, 0.0F, 1.0F));
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int centerY = height / 2;
        graphics.fill(0, 0, width, Math.max(24, centerY - 58), Math.round(126.0F * alpha) << 24);
        graphics.fill(0, Math.min(height, centerY + 54), width, height, Math.round(126.0F * alpha) << 24);
        int textAlpha = Math.max(4, Math.round(255.0F * alpha));
        Component number = Component.translatable("floor.tbos.intro.title", ArchiveFloorPresentation.displayFloor(intro.floorIndex()));
        graphics.drawCenteredString(minecraft.font, number, width / 2, centerY - 24, (textAlpha << 24) | 0xE8D6A7);
        graphics.drawCenteredString(minecraft.font, ArchiveFloorPresentation.name(intro.floorIndex()), width / 2, centerY + 8, (textAlpha << 24) | 0x74D7D2);
        graphics.drawCenteredString(
                minecraft.font,
                Component.translatable(intro.ominous()
                        ? "floor.tbos.intro.subtitle.ominous"
                        : "floor.tbos.intro.blurb." + ArchiveFloorPresentation.nameIndex(intro.floorIndex())),
                width / 2,
                centerY + 23,
                (textAlpha << 24) | (intro.ominous() ? 0xFF6655 : 0xB8A98B));
    }
}
