package com.nightbeam.tbos.client;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.network.payload.LensUseRequest;
import com.nightbeam.tbos.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class ClientEvents {
    private static final TagKey<Block> LENS_INTERACTION_PASSTHROUGH = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "lens_interaction_passthrough"));

    private ClientEvents() {
    }

    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        ItemStack held = player.getItemInHand(event.getHand());
        if (!held.is(ModItems.YESTERGLASS_LENS.get())) {
            return;
        }
        if (minecraft.level != null
                && minecraft.hitResult instanceof BlockHitResult blockHit
                && minecraft.level.getBlockState(blockHit.getBlockPos()).is(LENS_INTERACTION_PASSTHROUGH)) {
            return;
        }
        ClientPacketDistributor.sendToServer(LensUseRequest.INSTANCE);
        event.setCanceled(true);
        event.setSwingHand(true);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        ClientTransitionTracker.tick(Minecraft.getInstance());
    }
}
