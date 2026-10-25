package com.relicweave.enchantment.effects;

import com.relicweave.RelicWeave;
import com.relicweave.registry.ModEnchantments;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Oxidation: direct player melee hits slow the victim (and weaken it at level III). Same melee test as Keen Edge. */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class OxidationEvents {
    public static final int BASE_SLOW_TICKS = 40;
    public static final int SLOW_TICKS_PER_LEVEL = 20;
    public static final int WEAKNESS_MIN_LEVEL = 3;
    public static final int WEAKNESS_TICKS = 60;

    private OxidationEvents() {}

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (!source.is(DamageTypes.PLAYER_ATTACK)
                || !(source.getEntity() instanceof Player player)
                || source.getDirectEntity() != player) {
            return;
        }

        ItemStack weapon = player.getMainHandItem();
        Enchantment oxidation = ModEnchantments.OXIDATION.get();
        int level = EnchantmentHelper.getItemEnchantmentLevel(oxidation, weapon);
        if (level <= 0 || !oxidation.canEnchant(weapon)) {
            return;
        }
        event.getEntity().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                BASE_SLOW_TICKS + SLOW_TICKS_PER_LEVEL * level, 0), player);
        if (level >= WEAKNESS_MIN_LEVEL) {
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_TICKS, 0), player);
        }
    }
}
