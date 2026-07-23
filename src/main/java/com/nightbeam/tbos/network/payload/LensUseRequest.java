package com.nightbeam.tbos.network.payload;

import com.nightbeam.tbos.Yesterglass;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record LensUseRequest() implements CustomPacketPayload {
    public static final LensUseRequest INSTANCE = new LensUseRequest();
    public static final Type<LensUseRequest> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "lens_use"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LensUseRequest> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
