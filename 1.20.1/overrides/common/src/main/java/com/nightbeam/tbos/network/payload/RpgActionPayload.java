package com.nightbeam.tbos.network.payload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
/** Client-to-server JSON request for the 1.0 systems; validated by {@code RpgNet.handleAction}. */
public record RpgActionPayload(String json) implements TbosPacket {
    public static final int MAX_LENGTH=1024;
    public RpgActionPayload { if(json==null || json.length()>MAX_LENGTH) throw new IllegalArgumentException("Oversized RPG action"); }
    public static final ResourceLocation ID=new ResourceLocation("tbos","rpg_action");
    public void write(FriendlyByteBuf buffer) { buffer.writeUtf(json,MAX_LENGTH); }
    public static RpgActionPayload read(FriendlyByteBuf buffer) { return new RpgActionPayload(buffer.readUtf(MAX_LENGTH)); }
    public ResourceLocation id() {return ID;}
}
