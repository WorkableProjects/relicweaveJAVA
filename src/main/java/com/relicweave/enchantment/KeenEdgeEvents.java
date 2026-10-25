package com.relicweave.enchantment;

import com.relicweave.RelicWeave;
import com.relicweave.registry.ModEnchantments;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Adds Keen Edge's flat bonus to direct player melee hits.
 * <p>
 * {@link LivingHurtEvent} fires once per damage application on the logical server, before armor reduction, so
 * the bonus is added exactly once per hit. The bonus is deliberately not implemented through
 * {@code Enchantment#getDamageBonus}, which vanilla also applies to mob attacks.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class KeenEdgeEvents {
    public static final float DAMAGE_PER_LEVEL = 1.0F;

    private KeenEdgeEvents() {}

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        // PLAYER_ATTACK covers direct and sweep melee hits; projectiles, thorns, and other sources use other types.
        if (!source.is(DamageTypes.PLAYER_ATTACK)
                || !(source.getEntity() instanceof Player player)
                || source.getDirectEntity() != player) {
            return;
        }

        ItemStack weapon = player.getMainHandItem();
        Enchantment keenEdge = ModEnchantments.KEEN_EDGE.get();
        int level = EnchantmentHelper.getItemEnchantmentLevel(keenEdge, weapon);
        if (level > 0 && keenEdge.canEnchant(weapon)) {
            event.setAmount(event.getAmount() + level * DAMAGE_PER_LEVEL);
        }
    }
}
