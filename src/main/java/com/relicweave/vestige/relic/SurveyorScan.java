package com.relicweave.vestige.relic;

import com.relicweave.relic.RelicAugment;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.network.VestigeNetwork;
import com.relicweave.vestige.puzzle.PuzzleBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Surveyor's Lens: scans a cube around the player for hidden glyphs / false walls, breakable seals and Ancient
 * Terminals and tells the client to outline them. Only block positions are sent: never containers, entities or boss
 * data. Works in any dimension.
 */
public final class SurveyorScan {
    public static final int BASE_RADIUS = 16;
    public static final int DURATION_TICKS = 200;

    private SurveyorScan() {}

    /** Scan radius for the lens {@code stack}: 16, Wide Survey x1.5, Focused Survey x0.75. */
    public static int radius(ItemStack stack) {
        if (VestigeRelics.has(stack, RelicAugment.SURVEYOR_WIDE)) {
            return (int) Math.round(BASE_RADIUS * 1.5);
        }
        if (VestigeRelics.has(stack, RelicAugment.SURVEYOR_FOCUSED)) {
            return (int) Math.round(BASE_RADIUS * 0.75);
        }
        return BASE_RADIUS;
    }

    /** True for blocks the lens outlines. */
    public static boolean isRevealable(BlockState state) {
        return state.is(VestigeKeys.HIDDEN_GLYPHS) || state.is(VestigeKeys.BREAKABLE_SEALS) || state.is(PuzzleBlocks.ANCIENT_TERMINAL.get());
    }

    /** Revealable blocks in the cube of {@code radius} around {@code center}, closest first, at most 256. Loaded chunks only. */
    public static List<BlockPos> scan(ServerLevel level, BlockPos center, int radius) {
        return scan(level, center, radius, false);
    }

    /** As {@link #scan(ServerLevel, BlockPos, int)}; with {@code glyphsOnly} only {@code #relicweave:hidden_glyphs}. */
    public static List<BlockPos> scan(ServerLevel level, BlockPos center, int radius, boolean glyphsOnly) {
        List<BlockPos> found = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                cursor.set(center.getX() + x, center.getY(), center.getZ() + z);
                if (!level.hasChunkAt(cursor)) {
                    continue;
                }
                for (int y = -radius; y <= radius; y++) {
                    cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (cursor.getY() < level.getMinBuildHeight() || cursor.getY() >= level.getMaxBuildHeight()) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cursor);
                    if (glyphsOnly ? state.is(VestigeKeys.HIDDEN_GLYPHS) : isRevealable(state)) {
                        found.add(cursor.immutable());
                    }
                }
            }
        }
        found.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        return found.size() > VestigeNetwork.RevealPacket.MAX_POSITIONS
                ? new ArrayList<>(found.subList(0, VestigeNetwork.RevealPacket.MAX_POSITIONS)) : found;
    }

    /** Scans and sends the reveal to {@code player}. Returns the number of outlined blocks. */
    public static int pulse(ServerPlayer player, ItemStack lens) {
        List<BlockPos> found = scan(player.serverLevel(), player.blockPosition(), radius(lens));
        VestigeNetwork.send(player, new VestigeNetwork.RevealPacket(VestigeNetwork.RevealPacket.KIND_SURVEYOR, DURATION_TICKS, found));
        return found.size();
    }
}
