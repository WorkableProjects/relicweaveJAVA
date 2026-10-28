package com.relicweave.enchantment.vestige;

import com.relicweave.RelicWeave;
import com.relicweave.registry.ModEnchantments;
import com.relicweave.vestige.VestigeKeys;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Sealbreaker (pickaxe): breaking a block in {@code #relicweave:breakable_seals} is {@code 1 + 3 * level} times faster.
 * Any other block is untouched (boss altars and Seal Gates are not in the tag).
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class SealbreakerEvents {
    private SealbreakerEvents() {}

    public static float multiplier(int level) {
        return 1.0F + 3.0F * Math.max(0, level);
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!event.getState().is(VestigeKeys.BREAKABLE_SEALS)) {
            return;
        }
        int level = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.SEALBREAKER.get(), event.getEntity().getItemBySlot(EquipmentSlot.MAINHAND));
        if (level > 0) {
            event.setNewSpeed(event.getNewSpeed() * multiplier(level));
        }
    }
}
