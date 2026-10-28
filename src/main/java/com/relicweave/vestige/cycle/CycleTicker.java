package com.relicweave.vestige.cycle;

import com.mojang.brigadier.Command;
import com.relicweave.RelicWeave;
import com.relicweave.vestige.VestigeConfig;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.network.VestigeNetwork;
import com.relicweave.vestige.network.VestigeNetwork.CycleSyncPacket;
import com.relicweave.vestige.registry.VestigeSounds;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Drives the Concordance Cycle on the server: once per second it compares the phase of the Vestige level with the last
 * one it saw, announces changes (event, sound, action bar, {@link CycleSyncPacket}) and runs the pre-Convergence
 * warning. The phase itself is a pure function of game time ({@link ConcordanceCycle}); nothing here is persisted.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class CycleTicker {
    private static final int CHECK_INTERVAL = 20;

    /** Phase seen by the previous check; null until the first check after a server start. */
    private static CyclePhase lastPhase;
    /** Cycle index (gameTime / cycleTicks) for which the Convergence warning was already given. */
    private static long warnedCycle = -1;

    private CycleTicker() {}

    /** Current phase of the Vestige cycle (it only depends on game time, so any loaded level works). */
    public static CyclePhase currentPhase(MinecraftServer server) {
        ServerLevel level = server.getLevel(VestigeKeys.THE_VESTIGE);
        return ConcordanceCycle.phase(level != null ? level : server.overworld());
    }

    /** Clears remembered state (server start, tests). */
    public static void reset() {
        lastPhase = null;
        warnedCycle = -1;
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        reset();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % CHECK_INTERVAL != 0) {
            return;
        }
        ServerLevel level = event.getServer().getLevel(VestigeKeys.THE_VESTIGE);
        if (level != null) {
            tick(level, level.getGameTime());
        }
    }

    /** One second of cycle logic for {@code level} at {@code gameTime}. */
    public static void tick(ServerLevel level, long gameTime) {
        checkPhase(level, gameTime);
        checkWarning(level, gameTime);
    }

    /**
     * Compares the phase at {@code gameTime} with the previous check and, on a change, posts
     * {@link VestigeCycleEvent.PhaseChanged} and announces it to every player in {@code level}. The first call after
     * {@link #reset()} only records the phase.
     */
    public static void checkPhase(ServerLevel level, long gameTime) {
        CyclePhase now = ConcordanceCycle.phaseAt(gameTime);
        CyclePhase before = lastPhase;
        lastPhase = now;
        if (before == null || before == now) {
            return;
        }
        MinecraftForge.EVENT_BUS.post(new VestigeCycleEvent.PhaseChanged(level, before, now));
        SoundEvent sound = switch (now) {
            case QUIET -> VestigeSounds.CYCLE_QUIET.get();
            case RESONANCE -> VestigeSounds.CYCLE_RESONANCE.get();
            case CONVERGENCE -> VestigeSounds.CYCLE_CONVERGENCE.get();
        };
        for (ServerPlayer player : level.players()) {
            player.playNotifySound(sound, SoundSource.AMBIENT, 1.0F, 1.0F);
            player.displayClientMessage(Component.translatable("message.relicweave.cycle." + now.key())
                    .withStyle(now.color()), true);
            sync(player, gameTime);
        }
    }

    /** Sends the pre-Convergence warning once per cycle. */
    public static void checkWarning(ServerLevel level, long gameTime) {
        if (!inWarningWindow(gameTime)) {
            return;
        }
        long cycle = Math.floorDiv(gameTime, ConcordanceCycle.cycleTicks());
        if (cycle == warnedCycle) {
            return;
        }
        warnedCycle = cycle;
        for (ServerPlayer player : level.players()) {
            sync(player, gameTime);
            player.playNotifySound(VestigeSounds.CYCLE_WARNING.get(), SoundSource.AMBIENT, 1.0F, 1.0F);
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
            player.connection.send(new ClientboundSetTitleTextPacket(Component.empty()));
            player.connection.send(new ClientboundSetSubtitleTextPacket(
                    Component.translatable("message.relicweave.cycle.warning").withStyle(CyclePhase.CONVERGENCE.color())));
        }
    }

    /** True during the last {@code convergenceWarningSeconds} of Resonance. */
    public static boolean inWarningWindow(long gameTime) {
        if (ConcordanceCycle.phaseAt(gameTime) != CyclePhase.RESONANCE) {
            return false;
        }
        int seconds = VestigeConfig.SPEC.isLoaded() ? VestigeConfig.CONVERGENCE_WARNING_SECONDS.get() : 30;
        return ConcordanceCycle.ticksUntilNextPhase(gameTime) <= seconds * 20L;
    }

    /** Sends the current cycle state to {@code player}. */
    public static void sync(ServerPlayer player, long gameTime) {
        boolean enabled = ConcordanceCycle.enabled();
        long remaining = enabled ? ConcordanceCycle.ticksUntilNextPhase(gameTime) : 0;
        VestigeNetwork.send(player, new CycleSyncPacket(ConcordanceCycle.phaseAt(gameTime).ordinal(), remaining,
                inWarningWindow(gameTime), enabled));
    }

    private static void syncIfInVestige(ServerPlayer player) {
        if (player.level().dimension() == VestigeKeys.THE_VESTIGE) {
            sync(player, player.level().getGameTime());
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncIfInVestige(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTo() == VestigeKeys.THE_VESTIGE) {
            syncIfInVestige(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncIfInVestige(player);
        }
    }

    // ---- Debug command: /relicweave cycle info ----

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("relicweave")
                .then(Commands.literal("cycle")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("info").executes(ctx -> {
                            long time = ctx.getSource().getLevel().getGameTime();
                            CyclePhase phase = ConcordanceCycle.phaseAt(time);
                            long remaining = ConcordanceCycle.ticksUntilNextPhase(time);
                            String text = ConcordanceCycle.enabled()
                                    ? "Phase " + phase.key() + ", next phase in " + remaining / 20 + "s, cycle length "
                                            + ConcordanceCycle.cycleTicks() / 20 + "s"
                                    : "Cycle disabled (Quiet forever)";
                            ctx.getSource().sendSuccess(() -> Component.literal(text), false);
                            return Command.SINGLE_SUCCESS;
                        }))));
    }
}
