package com.nightbeam.tbos.network.payload;

import com.nightbeam.tbos.Yesterglass;
import net.minecraft.resources.ResourceLocation;

/** Requests an authoritative Archivist's Journal quest snapshot. */
public record JournalQuestRequest() implements TbosPacket {
    public static final JournalQuestRequest INSTANCE = new JournalQuestRequest();
    public static final ResourceLocation ID = new ResourceLocation(Yesterglass.MOD_ID, "journal_quest_request");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(net.minecraft.network.FriendlyByteBuf buffer) {
    }
}
