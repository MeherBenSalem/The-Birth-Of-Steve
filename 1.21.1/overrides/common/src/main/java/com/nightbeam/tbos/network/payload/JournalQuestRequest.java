package com.nightbeam.tbos.network.payload;

import com.nightbeam.tbos.Yesterglass;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Requests an authoritative Archivist's Journal quest snapshot. */
public record JournalQuestRequest() implements CustomPacketPayload {
    public static final JournalQuestRequest INSTANCE = new JournalQuestRequest();
    public static final Type<JournalQuestRequest> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Yesterglass.MOD_ID, "journal_quest_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, JournalQuestRequest> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
