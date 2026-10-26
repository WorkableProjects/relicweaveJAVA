package com.relicweave.vestige.gate;

import com.mojang.brigadier.CommandDispatcher;
import com.relicweave.RelicWeave;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Debug commands: {@code /relicweave gate tp|return|info} (op level 2). */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class GateCommands {
    private GateCommands() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("relicweave").then(Commands.literal("gate")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("tp").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (!GateTravel.debugToArrival(player)) {
                        ctx.getSource().sendFailure(Component.literal("The Vestige is not available."));
                        return 0;
                    }
                    return 1;
                }))
                .then(Commands.literal("return").executes(ctx -> {
                    GateTravel.debugReturn(ctx.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("info").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    CompoundTag point = GateTravel.returnPoint(player);
                    String ret = point.isEmpty() ? "none" : String.format("%s %.1f %.1f %.1f yaw %.0f", point.getString("dimension"),
                            point.getDouble("x"), point.getDouble("y"), point.getDouble("z"), point.getFloat("yaw"));
                    BlockPos arrival = ArrivalPlaza.vestige(player.server) == null ? null
                            : VestigeWorldData.get(ArrivalPlaza.vestige(player.server)).isBuilt()
                            ? VestigeWorldData.get(ArrivalPlaza.vestige(player.server)).arrival() : null;
                    String arrivalText = arrival == null ? "not built yet" : arrival.toShortString();
                    ctx.getSource().sendSuccess(() -> Component.literal("Return point: " + ret
                            + "; cooldown: " + GateTravel.cooldownRemaining(player) + " ticks; arrival: " + arrivalText), false);
                    return 1;
                }))));
    }
}
