package com.relicweave.enchantment.vestige;

import com.relicweave.RelicWeave;
import com.relicweave.registry.ModEnchantments;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.network.VestigeNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Glyphfinder (helmet): in The Vestige, every 200 / 120 ticks, outlines {@code #relicweave:hidden_glyphs} blocks within
 * 8 / 12 blocks for 3 seconds through a {@link VestigeNetwork.RevealPacket}. Only blocks, and only when any were found.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class GlyphfinderEvents {
    public static final int REVEAL_TICKS = 60;

    private GlyphfinderEvents() {}

    public static int intervalTicks(int level) {
        return level >= 2 ? 120 : 200;
    }

    public static int radius(int level) {
        return level >= 2 ? 12 : 8;
    }

    /** Hidden-glyph block positions within the radius of {@code level}, at most {@link VestigeNetwork.RevealPacket#MAX_POSITIONS}. */
    public static List<BlockPos> scan(ServerPlayer player, int level) {
        List<BlockPos> found = new ArrayList<>();
        int r = radius(level);
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (player.level().getBlockState(pos).is(VestigeKeys.HIDDEN_GLYPHS)) {
                found.add(pos.immutable());
                if (found.size() >= VestigeNetwork.RevealPacket.MAX_POSITIONS) {
                    break;
                }
            }
        }
        return found;
    }

    /** Scans and sends the reveal packet when something was found; returns the number of positions sent. */
    public static int pulse(ServerPlayer player, int level) {
        if (level <= 0) {
            return 0;
        }
        List<BlockPos> found = scan(player, level);
        if (!found.isEmpty()) {
            VestigeNetwork.send(player, new VestigeNetwork.RevealPacket(VestigeNetwork.RevealPacket.KIND_GLYPHFINDER, REVEAL_TICKS, found));
        }
        return found.size();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.level().dimension() != VestigeKeys.THE_VESTIGE) {
            return;
        }
        int level = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.GLYPHFINDER.get(), player.getItemBySlot(EquipmentSlot.HEAD));
        if (level > 0 && player.tickCount % intervalTicks(level) == 0) {
            pulse(player, level);
        }
    }
}
