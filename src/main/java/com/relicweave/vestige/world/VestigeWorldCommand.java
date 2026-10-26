package com.relicweave.vestige.world;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.relicweave.RelicWeave;
import com.relicweave.vestige.VestigeKeys;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.TreeMap;

/** Debug command {@code /relicweave world biomes [radius]}: counts biomes on a 64-block grid around the executor. */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class VestigeWorldCommand {
    private static final int GRID = 64;
    private static final int DEFAULT_RADIUS = 1024;
    private static final int MAX_RADIUS = 16384;

    private VestigeWorldCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("relicweave")
                .then(Commands.literal("world")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("biomes")
                                .executes(ctx -> run(ctx, DEFAULT_RADIUS))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(GRID, MAX_RADIUS))
                                        .executes(ctx -> run(ctx, IntegerArgumentType.getInteger(ctx, "radius")))))));
    }

    private static int run(CommandContext<CommandSourceStack> ctx, int radius) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        if (!level.dimension().equals(VestigeKeys.THE_VESTIGE)) {
            source.sendFailure(Component.literal("Run this inside The Vestige (relicweave:the_vestige)."));
            return 0;
        }
        Map<String, Integer> counts = sample(level, source.getPosition().x, source.getPosition().z, radius);
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        source.sendSuccess(() -> Component.literal("Biomes within " + radius + " blocks (" + total + " samples):"), false);
        counts.forEach((name, n) -> source.sendSuccess(
                () -> Component.literal(String.format("  %s: %d (%.1f%%)", name, n, 100.0 * n / total)), false));
        return total;
    }

    /** Samples the biome source (no chunk loading) on a {@value #GRID}-block grid; returns counts keyed by biome id. */
    public static Map<String, Integer> sample(ServerLevel level, double cx, double cz, int radius) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        BiomeSource source = generator.getBiomeSource();
        var sampler = level.getChunkSource().randomState().sampler();
        Map<String, Integer> counts = new TreeMap<>();
        for (int dx = -radius; dx <= radius; dx += GRID) {
            for (int dz = -radius; dz <= radius; dz += GRID) {
                int x = (int) Math.floor(cx) + dx;
                int z = (int) Math.floor(cz) + dz;
                Holder<Biome> biome = source.getNoiseBiome(x >> 2, 64 >> 2, z >> 2, sampler);
                String name = biome.unwrapKey().map(key -> key.location().getPath()).orElse("unknown");
                counts.merge(name, 1, Integer::sum);
            }
        }
        return counts;
    }
}
