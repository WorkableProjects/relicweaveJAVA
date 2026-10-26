package com.relicweave.enchantment.vestige;

import com.relicweave.RelicWeave;
import com.relicweave.registry.ModEnchantments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Archivist's Mark (bow/crossbow): an arrow or bolt fired by a player who holds the enchantment marks the living entity
 * it hits with the {@code relicweave:archivists_mark} effect and vanilla Glowing for {@code 20 + 40 * level} ticks
 * (60 / 100). While marked, damage from any player is multiplied by {@value #DAMAGE_MULTIPLIER}; the multiplier is
 * flat, so it never stacks, and applies to bosses too. The level is stored on the arrow when it is fired.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class ArchivistsMarkEvents {
    public static final float DAMAGE_MULTIPLIER = 1.10F;
    private static final String ARROW_TAG = RelicWeave.MOD_ID + ":archivists_mark_level";

    private ArchivistsMarkEvents() {}

    /** Mark duration in ticks: 60 for level I, 100 for level II. */
    public static int durationTicks(int level) {
        return 20 + 40 * level;
    }

    /** Applies the mark (effect plus Glowing) to {@code target}. */
    public static void mark(LivingEntity target, int level) {
        if (level <= 0) {
            return;
        }
        int duration = durationTicks(level);
        target.addEffect(new MobEffectInstance(VestigeEnchantEffects.ARCHIVISTS_MARK.get(), duration, 0, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, false, false));
    }

    /** Enchantment level on whatever the player holds (bow or crossbow in either hand). */
    public static int heldLevel(Player player) {
        return Math.max(
                EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.ARCHIVISTS_MARK.get(), player.getItemBySlot(EquipmentSlot.MAINHAND)),
                EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.ARCHIVISTS_MARK.get(), player.getItemBySlot(EquipmentSlot.OFFHAND)));
    }

    /** Damage after the mark bonus: {@code amount * 1.10} when {@code victim} is marked, else unchanged. */
    public static float adjustDamage(LivingEntity victim, float amount) {
        return victim.hasEffect(VestigeEnchantEffects.ARCHIVISTS_MARK.get()) ? amount * DAMAGE_MULTIPLIER : amount;
    }

    @SubscribeEvent
    public static void onArrowJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow)
                || !(arrow.getOwner() instanceof ServerPlayer player)) {
            return;
        }
        int level = heldLevel(player);
        if (level > 0) {
            arrow.getPersistentData().putInt(ARROW_TAG, level);
        }
    }

    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow) || arrow.level().isClientSide
                || !(event.getRayTraceResult() instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof LivingEntity target)) {
            return;
        }
        CompoundTag data = arrow.getPersistentData();
        if (data.contains(ARROW_TAG)) {
            mark(target, data.getInt(ARROW_TAG));
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player) {
            event.setAmount(adjustDamage(event.getEntity(), event.getAmount()));
        }
    }
}
