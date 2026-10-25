package com.relicweave.enchantment.effects;

import com.relicweave.RelicWeave;
import com.relicweave.registry.ModEnchantments;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Villager's Best Friend: grants Hero of the Village when the wearer interacts with a villager. Vanilla reads the
 * effect when the trade screen opens, which happens after this event, so the discount applies to that trade.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class VillagersBestFriendEvents {
    public static final int EFFECT_TICKS = 100;

    private VillagersBestFriendEvents() {}

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !(event.getTarget() instanceof Villager)) {
            return;
        }
        int level = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.VILLAGERS_BEST_FRIEND.get(),
                event.getEntity().getItemBySlot(EquipmentSlot.LEGS));
        if (level > 0) {
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, EFFECT_TICKS, level - 1,
                    true, false, false));
        }
    }
}
