package com.relicweave.vestige.hazard.field;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.relicweave.RelicWeave;
import com.relicweave.vestige.VestigeConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Debug commands: {@code /relicweave hazard pulse [up|side]|starfall|noise|nullcheck} (op level 2). */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class FieldCommands {
    private FieldCommands() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> hazard = Commands.literal("hazard").requires(s -> s.hasPermission(2))
                .then(Commands.literal("pulse")
                        .executes(ctx -> pulse(ctx.getSource().getPlayerOrException(), Direction.UP))
                        .then(Commands.literal("up").executes(ctx -> pulse(ctx.getSource().getPlayerOrException(), Direction.UP)))
                        .then(Commands.literal("side").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            return pulse(player, Direction.from2DDataValue(player.getRandom().nextInt(4)));
                        })))
                .then(Commands.literal("starfall").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    int warning = VestigeConfig.SPEC.isLoaded() ? VestigeConfig.STARFALL_WARNING_TICKS.get() : 60;
                    float damage = VestigeConfig.SPEC.isLoaded() ? VestigeConfig.STARFALL_DAMAGE.get().floatValue() : 6.0F;
                    StarfallImpacts.spawnMarker(player.serverLevel(), player.position(), warning, damage, null);
                    return 1;
                }))
                .then(Commands.literal("noise").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    float level = HushNoise.level(player);
                    ctx.getSource().sendSuccess(() -> Component.literal(String.format("Hush noise: %.1f (%s)", level,
                            HushNoise.isLoud(player) ? "loud" : "quiet")), false);
                    return (int) level;
                }))
                .then(Commands.literal("nullcheck").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    boolean suppressed = NullPockets.isSuppressed(player);
                    boolean ignoring = NullPockets.isSuppressedIgnoringDimension(player);
                    ctx.getSource().sendSuccess(() -> Component.literal("Suppressed: " + suppressed
                            + " (ignoring dimension: " + ignoring + "), anchored: " + GravityShear.isAnchored(player)), false);
                    return suppressed ? 1 : 0;
                }));
        event.getDispatcher().register(Commands.literal("relicweave").then(hazard));
    }

    private static int pulse(ServerPlayer player, Direction dir) {
        double strength = VestigeConfig.SPEC.isLoaded() ? VestigeConfig.GRAVITY_PULSE_STRENGTH.get() : 0.6;
        GravityShear.pulse(player, dir, strength);
        return 1;
    }
}
