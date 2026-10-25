package com.relicweave.enchantment.effects;

import com.relicweave.RelicWeave;
import com.relicweave.registry.ModEnchantments;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Necromanced: non-boss undead ignore the wearer unless provoked, and the wearer is immune to Wither. */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class NecromancedEvents {
    private NecromancedEvents() {}

    public static boolean wearsHelmet(Player player) {
        return EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.NECROMANCED.get(), player.getItemBySlot(EquipmentSlot.HEAD)) > 0;
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.getMobType() != MobType.UNDEAD
                || mob.getType().is(Tags.EntityTypes.BOSSES) || NecromancerMinions.isMinion(mob)
                || !(event.getNewTarget() instanceof Player player)) {
            return;
        }
        if (wearsHelmet(player) && mob.getLastHurtByMob() != player) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Player player && event.getEffectInstance().getEffect() == MobEffects.WITHER
                && wearsHelmet(player)) {
            event.setResult(Event.Result.DENY);
        }
    }
}
