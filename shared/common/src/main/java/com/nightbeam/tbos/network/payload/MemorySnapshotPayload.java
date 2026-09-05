package com.nightbeam.tbos.network.payload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
public record MemorySnapshotPayload(String json) implements CustomPacketPayload {
    public MemorySnapshotPayload { if(json==null || json.length()>8192) throw new IllegalArgumentException("Oversized memory snapshot"); }
    public static final Type<MemorySnapshotPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("tbos","memory_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf,MemorySnapshotPayload> STREAM_CODEC=StreamCodec.of(MemorySnapshotPayload::write,MemorySnapshotPayload::read);
    private static void write(RegistryFriendlyByteBuf buffer,MemorySnapshotPayload value) { buffer.writeUtf(value.json,8192); }
    private static MemorySnapshotPayload read(RegistryFriendlyByteBuf buffer) { return new MemorySnapshotPayload(buffer.readUtf(8192)); }
    public Type<? extends CustomPacketPayload> type() {return TYPE;}
}
