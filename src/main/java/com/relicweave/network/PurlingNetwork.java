package com.relicweave.network;

import com.relicweave.RelicWeave;
import com.relicweave.client.ClientPacketHandlers;
import com.relicweave.enchantment.PurlingHandler;
import com.relicweave.enchantment.upbound.UpboundHandler;
import com.relicweave.relic.RelicActivation;
import com.relicweave.relic.RelicType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public final class PurlingNetwork {
    /** "2" since 0.0.3 (relic activation and Resonance streak packets). */
    private static final String PROTOCOL_VERSION = "2";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            RelicWeave.id("main"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    private PurlingNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(ActivatePurlingPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ActivatePurlingPacket::encode)
                .decoder(ActivatePurlingPacket::decode)
                .consumerMainThread(ActivatePurlingPacket::handle)
                .add();
        CHANNEL.messageBuilder(UpboundJumpPacket.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UpboundJumpPacket::encode)
                .decoder(UpboundJumpPacket::decode)
                .consumerMainThread(UpboundJumpPacket::handle)
                .add();
        CHANNEL.messageBuilder(ActivateRelicPacket.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ActivateRelicPacket::encode)
                .decoder(ActivateRelicPacket::decode)
                .consumerMainThread(ActivateRelicPacket::handle)
                .add();
        CHANNEL.messageBuilder(ResonanceStreakPacket.class, 3, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ResonanceStreakPacket::encode)
                .decoder(ResonanceStreakPacket::decode)
                .consumerMainThread(ResonanceStreakPacket::handle)
                .add();
    }

    /**
     * Server -> client: the miner's current Resonance streak, so the client predicts the same break speed as the
     * server. Only touches common code, so it is safe to register on a dedicated server.
     */
    public record ResonanceStreakPacket(ResourceLocation block, int count) {
        public void encode(FriendlyByteBuf buf) {
            buf.writeResourceLocation(block);
            buf.writeVarInt(count);
        }

        public static ResonanceStreakPacket decode(FriendlyByteBuf buf) {
            return new ResonanceStreakPacket(buf.readResourceLocation(), buf.readVarInt());
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.resonanceStreak(block, count));
            ctx.get().setPacketHandled(true);
        }
    }

    /**
     * Client -> server: "activate my selected relic". Carries only the relic's item path; the server checks that the
     * player really carries that relic in a valid location and that it is off cooldown.
     */
    public record ActivateRelicPacket(String relic) {
        private static final int MAX_LENGTH = 64;

        public void encode(FriendlyByteBuf buf) {
            buf.writeUtf(relic, MAX_LENGTH);
        }

        public static ActivateRelicPacket decode(FriendlyByteBuf buf) {
            return new ActivateRelicPacket(buf.readUtf(MAX_LENGTH));
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            RelicType type = RelicType.byItemPath(relic);
            if (sender != null && type != null) {
                RelicActivation.tryActivate(sender, type);
            }
            ctx.get().setPacketHandled(true);
        }
    }

    /** Client -> server: "I performed an Upbound air jump". Carries no data; the server validates everything. */
    public record UpboundJumpPacket() {
        public void encode(FriendlyByteBuf buf) {}

        public static UpboundJumpPacket decode(FriendlyByteBuf buf) {
            return new UpboundJumpPacket();
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                UpboundHandler.tryAirJump(sender);
            }
            ctx.get().setPacketHandled(true);
        }
    }

    /** Client -> server: "the Purling key was pressed". Carries no data; the server validates everything. */
    public record ActivatePurlingPacket() {
        public void encode(FriendlyByteBuf buf) {}

        public static ActivatePurlingPacket decode(FriendlyByteBuf buf) {
            return new ActivatePurlingPacket();
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                PurlingHandler.tryActivate(sender);
            }
            ctx.get().setPacketHandled(true);
        }
    }
}
