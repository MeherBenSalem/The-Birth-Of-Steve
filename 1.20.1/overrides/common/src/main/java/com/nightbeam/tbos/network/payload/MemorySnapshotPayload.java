package com.nightbeam.tbos.network.payload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
public record MemorySnapshotPayload(String json) implements TbosPacket {
    public MemorySnapshotPayload { if(json==null || json.length()>8192) throw new IllegalArgumentException("Oversized memory snapshot"); }
    public static final ResourceLocation ID=new ResourceLocation("tbos","memory_snapshot");
    public void write(FriendlyByteBuf buffer) { MemorySnapshotPayload value=this; buffer.writeUtf(value.json,8192); }
    public static MemorySnapshotPayload read(FriendlyByteBuf buffer) { return new MemorySnapshotPayload(buffer.readUtf(8192)); }
    public ResourceLocation id() {return ID;}
}
