package com.relicweave.relic;

import com.relicweave.RelicWeave;
import com.relicweave.item.RelicTotemItem;
import com.relicweave.item.TotemTier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * RelicWeave totems: a passive while held, and a reusable death save that goes on cooldown instead of being consumed.
 * Only the highest qualifying tier's passive applies. A vanilla Totem of Undying in a hand triggers before this event
 * fires, so it naturally takes priority over these.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class TotemEvents {
    private static final UUID TOTEM_HEALTH_ID = UUID.fromString("5b0c1c3e-7a41-4d52-9a0e-0c6a1f2b3101");
    private static final String TOTEM_HEALTH_NAME = "relicweave:totem/max_health";
    private static final double KNOCKBACK_RADIUS = 4.0;
    private static final double KNOCKBACK_STRENGTH = 1.5;

    private TotemEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % CharmEvents.CHARM_CHECK_INTERVAL == 0) {
            updatePassive(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && tryDeathSave(player, event.getSource())) {
            event.setCanceled(true);
        }
    }

    private static TotemTier tierOf(ItemStack stack) {
        return ((RelicTotemItem) stack.getItem()).getTier();
    }

    /** Totems the player holds that currently work, best first (offhand first among equal tiers). */
    static List<ItemStack> qualifyingTotems(ServerPlayer player) {
        List<ItemStack> result = new ArrayList<>(2);
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof RelicTotemItem) {
            result.add(offhand);
        }
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof RelicTotemItem totem && totem.getTier().worksInMainHand()) {
            result.add(mainHand);
        }
        // Stable sort keeps the offhand ahead of the main hand when tiers tie.
        result.sort(Comparator.comparing(TotemEvents::tierOf).reversed());
        return result;
    }

    /** Applies the passive of the best held totem and removes stale bonuses when none is held. */
    public static void updatePassive(ServerPlayer player) {
        List<ItemStack> totems = qualifyingTotems(player);
        TotemTier tier = totems.isEmpty() ? null : tierOf(totems.get(0));

        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            double bonus = 0.0;
            if (tier == TotemTier.DIAMOND || tier == TotemTier.NETHERITE) {
                bonus = 4.0;
            } else if (tier == TotemTier.ULTRA) {
                bonus = 8.0;
            }
            AttributeModifier existing = health.getModifier(TOTEM_HEALTH_ID);
            if (existing != null && existing.getAmount() != bonus) {
                health.removeModifier(TOTEM_HEALTH_ID);
                existing = null;
            }
            if (bonus > 0.0 && existing == null) {
                health.addTransientModifier(new AttributeModifier(TOTEM_HEALTH_ID, TOTEM_HEALTH_NAME, bonus, AttributeModifier.Operation.ADDITION));
            }
        }

        if (tier == TotemTier.NETHERITE || tier == TotemTier.ULTRA) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, true, false, true));
        }
        if (tier == TotemTier.ULTRA) {
            // Refresh only when nearly expired so the regeneration timer is allowed to run down and actually heal.
            MobEffectInstance regen = player.getEffect(MobEffects.REGENERATION);
            if (regen == null || regen.getDuration() < 40) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, false, true));
            }
        }
        RelicAttributes.clampHealth(player);
    }

    /**
     * Tries to save the player from a lethal hit. Returns true when a totem fired (the caller cancels the death).
     * Damage that bypasses invulnerability (void, /kill) is never saved, matching vanilla totems.
     */
    public static boolean tryDeathSave(ServerPlayer player, DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        ItemStack used = null;
        for (ItemStack candidate : qualifyingTotems(player)) {
            if (!player.getCooldowns().isOnCooldown(candidate.getItem())) {
                used = candidate;
                break;
            }
        }
        if (used == null) {
            return false;
        }
        Item item = used.getItem();
        TotemTier tier = tierOf(used);

        switch (tier) {
            case DIAMOND -> {
                player.setHealth(4.0F);
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            }
            case NETHERITE -> {
                player.setHealth(player.getMaxHealth() / 2.0F);
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0));
                for (LivingEntity nearby : player.level().getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(KNOCKBACK_RADIUS), e -> e != player)) {
                    nearby.knockback(KNOCKBACK_STRENGTH, player.getX() - nearby.getX(), player.getZ() - nearby.getZ());
                }
            }
            case ULTRA -> {
                player.setHealth(player.getMaxHealth());
                for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
                    MobEffect type = effect.getEffect();
                    if (type.getCategory() == MobEffectCategory.HARMFUL) {
                        player.removeEffect(type);
                    }
                }
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 3));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 1));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 2));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0));
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200, 0));
            }
        }

        player.getCooldowns().addCooldown(item, tier.cooldownTicks());
        player.level().broadcastEntityEvent(player, (byte) 35);
        player.awardStat(Stats.ITEM_USED.get(item));
        return true;
    }
}
