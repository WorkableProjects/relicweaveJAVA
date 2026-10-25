package com.relicweave.enchantment.effects;

import com.relicweave.RelicWeave;
import com.relicweave.registry.ModEnchantments;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Piglinified: piglins (not brutes) will not start targeting a wearer, unless the wearer hurt them first. */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class PiglinifiedEvents {
    private PiglinifiedEvents() {}

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof AbstractPiglin piglin) || piglin instanceof PiglinBrute
                || !(event.getNewTarget() instanceof Player player)) {
            return;
        }
        if (EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.PIGLINIFIED.get(), player.getItemBySlot(EquipmentSlot.HEAD)) > 0
                && piglin.getLastHurtByMob() != player) {
            event.setCanceled(true);
        }
    }
}
