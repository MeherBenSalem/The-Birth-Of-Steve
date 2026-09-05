package com.nightbeam.tbos.network.payload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
public record MemoryActionRequest(int action, int first, int second) implements CustomPacketPayload {
    public static final Type<MemoryActionRequest> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("tbos","memory_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf,MemoryActionRequest> STREAM_CODEC=StreamCodec.of(MemoryActionRequest::write,MemoryActionRequest::read);
    private static void write(RegistryFriendlyByteBuf buffer,MemoryActionRequest value) { buffer.writeVarInt(value.action);buffer.writeVarInt(value.first);buffer.writeVarInt(value.second); }
    private static MemoryActionRequest read(RegistryFriendlyByteBuf buffer) { return new MemoryActionRequest(buffer.readVarInt(),buffer.readVarInt(),buffer.readVarInt()); }
    public Type<? extends CustomPacketPayload> type() {return TYPE;}
}
