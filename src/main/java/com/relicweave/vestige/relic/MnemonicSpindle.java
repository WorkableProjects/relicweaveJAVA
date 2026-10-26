package com.relicweave.vestige.relic;

import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.network.VestigeNetwork;
import com.relicweave.vestige.puzzle.PuzzleBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;
import com.relicweave.relic.RelicAugment;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Mnemonic Spindle: decodes an Ancient Terminal within 5 blocks into a chat clue pointing to the nearest Vestige
 * structure (never the Crown of Concordance keep), and with Deep Reading outlines hidden glyphs within 16 blocks.
 * It never opens doors, gates or altars.
 */
public final class MnemonicSpindle {
    public static final int TERMINAL_RANGE = 5;
    public static final int SEARCH_CHUNKS = 50;
    public static final int DEEP_RADIUS = 16;

    /** Structures the clue can point to: every Vestige structure except the Crown of Concordance keep. */
    public static final List<ResourceKey<Structure>> CLUE_STRUCTURES = List.of(
            VestigeKeys.WAYSTONE_MONOLITH, VestigeKeys.ARCHIVISTS_OUTPOST, VestigeKeys.RUINED_AETHER_AQUEDUCT,
            VestigeKeys.GILDED_CIVIC_TEMPLE, VestigeKeys.GLASSROOT_WARREN, VestigeKeys.HUSH_ARCHIVE,
            VestigeKeys.SUNDERED_OBSERVATORY, VestigeKeys.GRAVITY_VAULT, VestigeKeys.STARFALL_FORGE,
            VestigeKeys.CYCLE_OBSERVATORY, VestigeKeys.HALL_OF_NINE_SEALS, VestigeKeys.CROWN_BASTION,
            VestigeKeys.RIFT_SCAR);

    private static final String[] DIRECTIONS = {"east", "south_east", "south", "south_west", "west", "north_west", "north", "north_east"};

    private MnemonicSpindle() {}

    /** True if an {@code ancient_terminal} is within {@link #TERMINAL_RANGE} blocks of {@code pos}. */
    public static boolean terminalNearby(ServerLevel level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-TERMINAL_RANGE, -TERMINAL_RANGE, -TERMINAL_RANGE),
                pos.offset(TERMINAL_RANGE, TERMINAL_RANGE, TERMINAL_RANGE))) {
            if (level.hasChunkAt(p) && level.getBlockState(p).is(PuzzleBlocks.ANCIENT_TERMINAL.get())) {
                return true;
            }
        }
        return false;
    }

    /** 8-way compass key (east, south_east, ...) from {@code from} toward {@code to}; +x is east, +z is south. */
    public static String direction(BlockPos from, BlockPos to) {
        double angle = Math.atan2(to.getZ() - from.getZ(), to.getX() - from.getX());
        int index = (int) Math.floorMod(Math.round(angle / (Math.PI / 4.0)), 8L);
        return DIRECTIONS[index];
    }

    /** Distance rounded to the nearest 50 blocks, at least 50. */
    public static int roughDistance(BlockPos from, BlockPos to) {
        double dist = Math.sqrt(from.distSqr(to));
        return (int) Math.max(50, Math.round(dist / 50.0) * 50);
    }

    /** Nearest clue structure within {@link #SEARCH_CHUNKS} chunks, or null. */
    @Nullable
    public static com.mojang.datafixers.util.Pair<BlockPos, Holder<Structure>> findNearest(ServerLevel level, BlockPos from) {
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        List<Holder<Structure>> holders = new ArrayList<>();
        for (ResourceKey<Structure> key : CLUE_STRUCTURES) {
            registry.getHolder(key).ifPresent(holders::add);
        }
        if (holders.isEmpty()) {
            return null;
        }
        return level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holders), from, SEARCH_CHUNKS, false);
    }

    /** Decodes the terminal near {@code player}: sends the clue (and the Deep Reading outline). */
    public static void decode(ServerPlayer player, ItemStack spindle) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();
        var found = findNearest(level, pos);
        if (found == null) {
            player.sendSystemMessage(Component.translatable("message.relicweave.spindle.nothing"));
        } else {
            String name = found.getSecond().unwrapKey().map(k -> k.location().getPath()).orElse("unknown");
            player.sendSystemMessage(Component.translatable("message.relicweave.spindle.clue",
                    Component.translatable("structure.relicweave." + name),
                    Component.translatable("direction.relicweave." + direction(pos, found.getFirst())),
                    roughDistance(pos, found.getFirst())));
        }
        if (VestigeRelics.has(spindle, RelicAugment.SPINDLE_DEEP)) {
            VestigeNetwork.send(player, new VestigeNetwork.RevealPacket(VestigeNetwork.RevealPacket.KIND_SURVEYOR,
                    SurveyorScan.DURATION_TICKS, SurveyorScan.scan(level, pos, DEEP_RADIUS, true)));
        }
    }
}
