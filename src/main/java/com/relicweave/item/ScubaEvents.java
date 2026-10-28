package com.relicweave.item;

import com.relicweave.RelicWeave;
import com.relicweave.registry.ModItems;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingBreatheEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-side Scuba Gear effects: endless air (Air Tank) and full mining speed underwater (Mask). */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class ScubaEvents {
    private ScubaEvents() {}

    @SubscribeEvent
    public static void onBreathe(LivingBreatheEvent event) {
        if (!event.canBreathe() && ScubaArmorItem.wearing(event.getEntity(), ModItems.SCUBA_TANK.get())) {
            event.setCanBreathe(true);
            event.setCanRefillAir(true);
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        event.setNewSpeed(underwaterSpeed(event.getEntity(), event.getNewSpeed()));
    }

    /** Undoes vanilla's two underwater mining penalties (eyes in water, not on the ground) for a Mask wearer. */
    public static float underwaterSpeed(Player player, float speed) {
        if (!ScubaArmorItem.wearing(player, ModItems.SCUBA_MASK.get())) {
            return speed;
        }
        if (player.isEyeInFluid(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(player)) {
            speed *= 5.0F;
        }
        if (!player.onGround() && player.isInWater()) {
            speed *= 5.0F;
        }
        return speed;
    }
}
