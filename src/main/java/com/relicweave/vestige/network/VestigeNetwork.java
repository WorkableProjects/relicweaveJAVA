package com.relicweave.vestige.network;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.client.VestigeClientState;
import com.relicweave.vestige.cycle.CyclePhase;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 0.0.4 channel ({@code relicweave:vestige}), separate from the 0.0.3 {@code relicweave:main} channel so neither
 * protocol changes the other. All packets are server -> client display state; the client never sends Vestige state.
 */
public final class VestigeNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            RelicWeave.id("vestige"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    private VestigeNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(CycleSyncPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CycleSyncPacket::encode).decoder(CycleSyncPacket::decode)
                .consumerMainThread(CycleSyncPacket::handle).add();
        CHANNEL.messageBuilder(HazardSyncPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(HazardSyncPacket::encode).decoder(HazardSyncPacket::decode)
                .consumerMainThread(HazardSyncPacket::handle).add();
        CHANNEL.messageBuilder(RevealPacket.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RevealPacket::encode).decoder(RevealPacket::decode)
                .consumerMainThread(RevealPacket::handle).add();
    }

    public static void send(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    /** Current cycle phase. Sent on phase change, on entering The Vestige, and when the warning starts. */
    public record CycleSyncPacket(int phase, long ticksRemaining, boolean warning, boolean enabled) {
        public void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(phase);
            buf.writeVarLong(ticksRemaining);
            buf.writeBoolean(warning);
            buf.writeBoolean(enabled);
        }

        public static CycleSyncPacket decode(FriendlyByteBuf buf) {
            return new CycleSyncPacket(buf.readVarInt(), buf.readVarLong(), buf.readBoolean(), buf.readBoolean());
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.cycle(this));
            ctx.get().setPacketHandled(true);
        }
    }

    /**
     * Hazard display state for the local player. {@code flags}: {@link #IN_RIFT_ZONE}, {@link #IN_NULL_POCKET},
     * {@link #GRAVITY_TELEGRAPH}, {@link #IN_HUSHFIELD}, {@link #RIFT_SICKNESS}.
     */
    public record HazardSyncPacket(int riftExposure, int flags, int riftProtectionPercent) {
        public static final int IN_RIFT_ZONE = 1;
        public static final int IN_NULL_POCKET = 1 << 1;
        public static final int GRAVITY_TELEGRAPH = 1 << 2;
        public static final int IN_HUSHFIELD = 1 << 3;
        public static final int RIFT_SICKNESS = 1 << 4;

        public void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(riftExposure);
            buf.writeVarInt(flags);
            buf.writeVarInt(riftProtectionPercent);
        }

        public static HazardSyncPacket decode(FriendlyByteBuf buf) {
            return new HazardSyncPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.hazard(this));
            ctx.get().setPacketHandled(true);
        }
    }

    /** Blocks to outline for a while (Surveyor's Lens, Glyphfinder). At most {@link #MAX_POSITIONS} positions. */
    public record RevealPacket(int kind, int durationTicks, List<BlockPos> positions) {
        public static final int KIND_SURVEYOR = 0;
        public static final int KIND_GLYPHFINDER = 1;
        public static final int MAX_POSITIONS = 256;

        public void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(kind);
            buf.writeVarInt(durationTicks);
            int n = Math.min(MAX_POSITIONS, positions.size());
            buf.writeVarInt(n);
            for (int i = 0; i < n; i++) {
                buf.writeBlockPos(positions.get(i));
            }
        }

        public static RevealPacket decode(FriendlyByteBuf buf) {
            int kind = buf.readVarInt();
            int duration = buf.readVarInt();
            int n = Math.min(MAX_POSITIONS, buf.readVarInt());
            List<BlockPos> positions = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                positions.add(buf.readBlockPos());
            }
            return new RevealPacket(kind, duration, positions);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.reveal(this));
            ctx.get().setPacketHandled(true);
        }
    }

    /** Client-only handlers; only class-loaded on the client through DistExecutor. */
    private static final class Client {
        private Client() {}

        static long gameTime() {
            return Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
        }

        static void cycle(CycleSyncPacket p) {
            VestigeClientState.phase = CyclePhase.byOrdinal(p.phase());
            VestigeClientState.ticksRemaining = p.ticksRemaining();
            VestigeClientState.syncedAtGameTime = gameTime();
            VestigeClientState.convergenceWarning = p.warning();
            VestigeClientState.cycleEnabled = p.enabled();
        }

        static void hazard(HazardSyncPacket p) {
            VestigeClientState.riftExposure = p.riftExposure();
            VestigeClientState.hazardFlags = p.flags();
            VestigeClientState.riftProtectionPercent = p.riftProtectionPercent();
        }

        static void reveal(RevealPacket p) {
            VestigeClientState.revealKind = p.kind();
            VestigeClientState.revealed = List.copyOf(p.positions());
            VestigeClientState.revealUntilGameTime = gameTime() + p.durationTicks();
        }
    }
}
