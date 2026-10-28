package com.nightbeam.tbos.network.payload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
/** Server-to-client JSON for the 1.0 systems; {@code RpgNet} routes it by its {@code "t"} field. */
public record RpgSyncPayload(String json) implements TbosPacket {
    public static final int MAX_LENGTH=32767;
    public RpgSyncPayload { if(json==null || json.length()>MAX_LENGTH) throw new IllegalArgumentException("Oversized RPG sync"); }
    public static final ResourceLocation ID=new ResourceLocation("tbos","rpg_sync");
    public void write(FriendlyByteBuf buffer) { buffer.writeUtf(json,MAX_LENGTH); }
    public static RpgSyncPayload read(FriendlyByteBuf buffer) { return new RpgSyncPayload(buffer.readUtf(MAX_LENGTH)); }
    public ResourceLocation id() {return ID;}
}
