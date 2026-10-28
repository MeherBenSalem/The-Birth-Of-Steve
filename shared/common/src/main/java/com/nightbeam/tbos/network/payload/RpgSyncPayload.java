package com.nightbeam.tbos.network.payload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
/** Server-to-client JSON for the 1.0 systems; {@code RpgNet} routes it by its {@code "t"} field. */
public record RpgSyncPayload(String json) implements CustomPacketPayload {
    public static final int MAX_LENGTH=32767;
    public RpgSyncPayload { if(json==null || json.length()>MAX_LENGTH) throw new IllegalArgumentException("Oversized RPG sync"); }
    public static final Type<RpgSyncPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("tbos","rpg_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf,RpgSyncPayload> STREAM_CODEC=StreamCodec.of(RpgSyncPayload::write,RpgSyncPayload::read);
    private static void write(RegistryFriendlyByteBuf buffer,RpgSyncPayload value) { buffer.writeUtf(value.json,MAX_LENGTH); }
    private static RpgSyncPayload read(RegistryFriendlyByteBuf buffer) { return new RpgSyncPayload(buffer.readUtf(MAX_LENGTH)); }
    public Type<? extends CustomPacketPayload> type() {return TYPE;}
}
