package com.relicweave.network;

import com.relicweave.RelicWeave;
import com.relicweave.enchantment.PurlingHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public final class PurlingNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            RelicWeave.id("main"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    private PurlingNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(ActivatePurlingPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ActivatePurlingPacket::encode)
                .decoder(ActivatePurlingPacket::decode)
                .consumerMainThread(ActivatePurlingPacket::handle)
                .add();
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
