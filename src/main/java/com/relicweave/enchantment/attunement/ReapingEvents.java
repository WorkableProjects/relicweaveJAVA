package com.relicweave.enchantment.attunement;

import com.relicweave.RelicWeave;
import com.relicweave.registry.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Reaping (hoe): breaking a mature crop harvests every mature crop in a square of radius {@code level} (I = 3x3,
 * II = 5x5, III = 7x7) on the same Y level and replants those whose drops contain a seed. Immature crops are never
 * touched, seeds are never invented (no seed in the drops: the block becomes air), and crouching breaks a single crop.
 * Every harvested crop beyond the broken one costs 1 durability. Creative players get no drops and no wear.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class ReapingEvents {
    /** Extra durability per harvested crop beyond the first (the first counts as the vanilla break). */
    public static final int DURABILITY_PER_EXTRA_CROP = 1;
    /** Vanilla level event for block break particles and sound. */
    private static final int BLOCK_BREAK_EVENT = 2001;

    private ReapingEvents() {}

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)
                || player.isShiftKeyDown() || !isMature(event.getState())) {
            return;
        }
        ItemStack hoe = player.getMainHandItem();
        int reaping = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.REAPING.get(), hoe);
        if (reaping <= 0) {
            return;
        }
        event.setCanceled(true);
        harvestArea(level, event.getPos(), player, hoe, reaping);
    }

    public static boolean isMature(BlockState state) {
        return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
    }

    /** Harvests the mature crops around {@code center} (included). Returns how many crops were harvested. */
    public static int harvestArea(ServerLevel level, BlockPos center, ServerPlayer player, ItemStack hoe, int reapingLevel) {
        int radius = Math.max(1, reapingLevel);
        int harvested = 0;
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                BlockPos pos = center.offset(dx, 0, dz);
                BlockState state = level.getBlockState(pos);
                if (!isMature(state)) {
                    continue;
                }
                if (harvested > 0) {
                    hoe.hurtAndBreak(DURABILITY_PER_EXTRA_CROP, player, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
                }
                harvestOne(level, pos, state, player, hoe);
                harvested++;
                if (hoe.isEmpty()) {
                    return harvested;
                }
            }
        }
        return harvested;
    }

    private static void harvestOne(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player, ItemStack hoe) {
        CropBlock crop = (CropBlock) state.getBlock();
        List<ItemStack> drops = Block.getDrops(state, level, pos, null, player, hoe);
        Item seed = state.getBlock().getCloneItemStack(level, pos, state).getItem();
        boolean replant = false;
        for (ItemStack drop : drops) {
            if (drop.is(seed) && !drop.isEmpty()) {
                drop.shrink(1);
                replant = true;
                break;
            }
        }
        level.levelEvent(BLOCK_BREAK_EVENT, pos, Block.getId(state));
        level.setBlock(pos, replant ? crop.getStateForAge(0) : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        if (!player.isCreative()) {
            drops.forEach(drop -> Block.popResource(level, pos, drop));
        }
    }
}
