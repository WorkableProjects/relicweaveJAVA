package com.relicweave.enchantment.effects;

import com.relicweave.RelicWeave;
import com.relicweave.registry.ModEnchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Conductive: faster mining with the right tool. Break speed is predicted on both sides, so this is not side-guarded. */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class ConductiveEvents {
    public static final float SPEED_BONUS_PER_LEVEL = 0.25F;

    private ConductiveEvents() {}

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack tool = event.getEntity().getMainHandItem();
        int level = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.CONDUCTIVE.get(), tool);
        if (level > 0 && tool.isCorrectToolForDrops(event.getState())) {
            event.setNewSpeed(event.getNewSpeed() * (1.0F + SPEED_BONUS_PER_LEVEL * level));
        }
    }
}
