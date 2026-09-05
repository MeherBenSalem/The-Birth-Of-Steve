package com.nightbeam.tbos.neoforge.platform;

import com.nightbeam.tbos.network.YesterglassNetwork;
import com.nightbeam.tbos.network.payload.ArchiveFloorIntroPayload;
import com.nightbeam.tbos.network.payload.ArchivePuzzlePayload;
import com.nightbeam.tbos.network.payload.ArchiveQuestPayload;
import com.nightbeam.tbos.network.payload.BeginTransitionPayload;
import com.nightbeam.tbos.network.payload.JournalQuestRequest;
import com.nightbeam.tbos.network.payload.JournalQuestSnapshotPayload;
import com.nightbeam.tbos.network.payload.LensUseRequest;
import com.nightbeam.tbos.network.payload.SiteSnapshotPayload;
import com.nightbeam.tbos.network.payload.TbosPacket;
import com.nightbeam.tbos.platform.services.INetworkHelper;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Forge 1.20.1 {@code SimpleChannel} transport for the loader-neutral
 * {@link TbosPacket} shape. All packets travel as one discriminator-carrying
 * wrapper message; the client populates {@link #clientHandlers} so the
 * dedicated server never loads the {@code client} package.
 */
public final class NeoForgeNetworkHelper implements INetworkHelper {
    private static final String PROTOCOL_VERSION =
            String.valueOf(YesterglassNetwork.NETWORK_VERSION);
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("tbos", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    /** Client-side server→client dispatch, populated by {@code TbosNeoForgeClient}. */
    static final Map<ResourceLocation, Consumer<TbosPacket>> clientHandlers =
            new ConcurrentHashMap<>();

    static {
        CHANNEL.registerMessage(
                0,
                TbosMessage.class,
                TbosMessage::encode,
                TbosMessage::decode,
                NeoForgeNetworkHelper::handle);
    }

    /**
     * Public and no-arg because {@code Services} loads this through
     * {@link java.util.ServiceLoader}, which rejects a provider it cannot
     * instantiate. Do not narrow it back.
     */
    public NeoForgeNetworkHelper() {
    }

    private static void handle(TbosMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            TbosPacket packet = message.packet;
            if (context.getDirection().getReceptionSide().isClient()) {
                Consumer<TbosPacket> handler = clientHandlers.get(packet.id());
                if (handler != null) {
                    handler.accept(packet);
                }
                return;
            }
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            if (packet instanceof com.nightbeam.tbos.network.payload.MemoryActionRequest request) {
                com.nightbeam.tbos.memory.MemoryService.request(sender,request.action(),request.first(),request.second());
            } else if (packet.id().equals(LensUseRequest.ID)) {
                YesterglassNetwork.handleLensUse(sender);
            } else if (packet.id().equals(JournalQuestRequest.ID)) {
                YesterglassNetwork.handleJournalQuestRequest(sender);
            }
        });
        context.setPacketHandled(true);
    }

    /**
     * Whether a packet can physically reach this player.
     *
     * <p>The Fabric helper gets this from {@code ServerPlayNetworking.canSend};
     * Forge has no equivalent, so the connection is checked directly. A player
     * without a live channel is not hypothetical: {@code GameTestHelper}'s mock
     * players carry a {@code Connection} whose channel is null, and
     * {@code SimpleChannel} dereferences {@code channel().pipeline()} on send.
     */
    private static boolean canSend(ServerPlayer player) {
        return player.connection != null
                && player.connection.connection != null
                && player.connection.connection.channel() != null;
    }

    @Override
    public void sendToPlayer(ServerPlayer player, TbosPacket packet) {
        if (!canSend(player)) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new TbosMessage(packet));
    }

    @Override
    public void sendToPlayersNear(
            ServerLevel level, double x, double y, double z, double radius, TbosPacket packet) {
        // Fanned out per player rather than through PacketDistributor.NEAR so
        // every recipient passes the guard above, matching the Fabric helper.
        double radiusSq = radius * radius;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) <= radiusSq) {
                sendToPlayer(player, packet);
            }
        }
    }

    @Override
    public void sendToServer(TbosPacket packet) {
        CHANNEL.sendToServer(new TbosMessage(packet));
    }

    /** Share the client-dispatch registry with the client bootstrap. */
    public static Map<ResourceLocation, Consumer<TbosPacket>> clientDispatch() {
        return clientHandlers;
    }

    /** Discriminator-carrying wrapper so one {@code SimpleChannel} message fits every packet. */
    public static final class TbosMessage {
        final TbosPacket packet;

        TbosMessage(TbosPacket packet) {
            this.packet = packet;
        }

        private static void encode(TbosMessage message, FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(message.packet.id());
            message.packet.write(buffer);
        }

        private static TbosMessage decode(FriendlyByteBuf buffer) {
            ResourceLocation id = buffer.readResourceLocation();
            TbosPacket packet;
            if (id.equals(com.nightbeam.tbos.network.payload.MemoryActionRequest.ID)) {
                packet=com.nightbeam.tbos.network.payload.MemoryActionRequest.read(buffer);
            } else if (id.equals(com.nightbeam.tbos.network.payload.MemorySnapshotPayload.ID)) {
                packet=com.nightbeam.tbos.network.payload.MemorySnapshotPayload.read(buffer);
            } else if (id.equals(LensUseRequest.ID)) {
                packet = LensUseRequest.INSTANCE;
            } else if (id.equals(JournalQuestRequest.ID)) {
                packet = JournalQuestRequest.INSTANCE;
            } else if (id.equals(BeginTransitionPayload.ID)) {
                packet = BeginTransitionPayload.read(buffer);
            } else if (id.equals(SiteSnapshotPayload.ID)) {
                packet = SiteSnapshotPayload.read(buffer);
            } else if (id.equals(ArchiveQuestPayload.ID)) {
                packet = ArchiveQuestPayload.read(buffer);
            } else if (id.equals(ArchivePuzzlePayload.ID)) {
                packet = ArchivePuzzlePayload.read(buffer);
            } else if (id.equals(ArchiveFloorIntroPayload.ID)) {
                packet = ArchiveFloorIntroPayload.read(buffer);
            } else if (id.equals(JournalQuestSnapshotPayload.ID)) {
                packet = JournalQuestSnapshotPayload.read(buffer);
            } else {
                throw new IllegalArgumentException("Unknown tbos::" + id + " packet on the wire");
            }
            return new TbosMessage(packet);
        }
    }
}
