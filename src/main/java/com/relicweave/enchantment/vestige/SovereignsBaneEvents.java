package com.relicweave.enchantment.vestige;

import com.relicweave.RelicWeave;
import com.relicweave.registry.ModEnchantments;
import com.relicweave.vestige.VestigeKeys;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Sovereign's Bane (sword/axe): direct player melee against {@code #relicweave:vestige_elites} deals
 * {@code +1.5 * level}; against {@code #relicweave:vestige_bosses} the bonus is {@code min(2.0, 1.0 * level)}.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class SovereignsBaneEvents {
    public static final float ELITE_PER_LEVEL = 1.5F;
    public static final float BOSS_PER_LEVEL = 1.0F;
    public static final float BOSS_CAP = 2.0F;

    private SovereignsBaneEvents() {}

    /** Bonus damage against {@code type} at enchantment {@code level}; 0 for anything not tagged. Bosses win over elites. */
    public static float bonus(EntityType<?> type, int level) {
        if (level <= 0) {
            return 0.0F;
        }
        if (type.is(VestigeKeys.VESTIGE_BOSSES)) {
            return Math.min(BOSS_CAP, BOSS_PER_LEVEL * level);
        }
        if (type.is(VestigeKeys.VESTIGE_ELITES)) {
            return ELITE_PER_LEVEL * level;
        }
        return 0.0F;
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || event.getSource().getDirectEntity() != player) {
            return;
        }
        int level = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.SOVEREIGNS_BANE.get(), player.getItemBySlot(EquipmentSlot.MAINHAND));
        float bonus = bonus(event.getEntity().getType(), level);
        if (bonus > 0.0F) {
            event.setAmount(event.getAmount() + bonus);
        }
    }
}
