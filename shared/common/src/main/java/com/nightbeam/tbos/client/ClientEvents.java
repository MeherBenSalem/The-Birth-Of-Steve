package com.nightbeam.tbos.client;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.network.payload.LensUseRequest;
import com.nightbeam.tbos.platform.Services;
import com.nightbeam.tbos.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;

public final class ClientEvents {
    private static final TagKey<Block> LENS_INTERACTION_PASSTHROUGH = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "lens_interaction_passthrough"));

    private ClientEvents() {
    }

    /** What a use-item interaction should do once this handler has looked at it. */
    public enum UseResult {
        /** Not one of the mod's items; let vanilla handle the interaction. */
        PASS,
        /** Handled: cancel the interaction, do not swing the arm. */
        CONSUME_NO_SWING,
        /** Handled: cancel the interaction and swing the arm. */
        CONSUME_AND_SWING
    }

    /**
     * Decides what a right-click holding the journal or the lens means.
     *
     * <p>Returns a verdict instead of cancelling an event object, so NeoForge can
     * drive it from its interaction-key event and Fabric from
     * {@code UseItemCallback} / {@code UseBlockCallback}.
     */
    public static UseResult onUseItem(InteractionHand hand) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return UseResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.ARCHIVISTS_JOURNAL.get())) {
            // One interface: the server-backed quest screen, sneaking or not.
            ArchivistQuestScreen.requestOpen();
            return UseResult.CONSUME_NO_SWING;
        }
        if (held.is(ModItems.STARTER_TOME.get())) {
            ArchivistNotesScreen.open();
            return UseResult.CONSUME_NO_SWING;
        }
        if (!held.is(ModItems.YESTERGLASS_LENS.get())) {
            return UseResult.PASS;
        }
        if (minecraft.level != null
                && minecraft.hitResult instanceof BlockHitResult blockHit
                && minecraft.level.getBlockState(blockHit.getBlockPos()).is(LENS_INTERACTION_PASSTHROUGH)) {
            return UseResult.PASS;
        }
        Services.NETWORK.sendToServer(LensUseRequest.INSTANCE);
        return UseResult.CONSUME_AND_SWING;
    }

    public static void onClientTick(Minecraft minecraft) {
        ClientTransitionTracker.tick(minecraft);
        ArchiveFloorIntroHud.tick(minecraft);
        while (ModKeyMappings.TOGGLE_OBJECTIVES.consumeClick()) {
            boolean hidden = ModKeyMappings.toggleObjectives();
            ClientCompat.setOverlayMessage(
                    minecraft,
                    Component.translatable(hidden
                            ? "message.tbos.objectives.hidden"
                            : "message.tbos.objectives.shown"),
                    false);
        }
    }
}
