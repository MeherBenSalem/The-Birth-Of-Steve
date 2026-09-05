package com.nightbeam.tbos.network.payload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
public record MemoryActionRequest(int action, int first, int second) implements TbosPacket {
    public static final ResourceLocation ID=new ResourceLocation("tbos","memory_action");
    public void write(FriendlyByteBuf buffer) { MemoryActionRequest value=this; buffer.writeVarInt(value.action);buffer.writeVarInt(value.first);buffer.writeVarInt(value.second); }
    public static MemoryActionRequest read(FriendlyByteBuf buffer) { return new MemoryActionRequest(buffer.readVarInt(),buffer.readVarInt(),buffer.readVarInt()); }
    public ResourceLocation id() {return ID;}
}
