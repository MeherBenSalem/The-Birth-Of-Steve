package com.nightbeam.tbos.network.payload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
/** Client-to-server JSON request for the 1.0 systems; validated by {@code RpgNet.handleAction}. */
public record RpgActionPayload(String json) implements CustomPacketPayload {
    public static final int MAX_LENGTH=1024;
    public RpgActionPayload { if(json==null || json.length()>MAX_LENGTH) throw new IllegalArgumentException("Oversized RPG action"); }
    public static final Type<RpgActionPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("tbos","rpg_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf,RpgActionPayload> STREAM_CODEC=StreamCodec.of(RpgActionPayload::write,RpgActionPayload::read);
    private static void write(RegistryFriendlyByteBuf buffer,RpgActionPayload value) { buffer.writeUtf(value.json,MAX_LENGTH); }
    private static RpgActionPayload read(RegistryFriendlyByteBuf buffer) { return new RpgActionPayload(buffer.readUtf(MAX_LENGTH)); }
    public Type<? extends CustomPacketPayload> type() {return TYPE;}
}
