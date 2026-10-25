package com.relicweave.enchantment.attunement;

import com.relicweave.RelicWeave;
import com.relicweave.registry.ModEnchantments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Undertow (trident): melee or thrown hits on a target that is in water slow it (amplifier {@code level - 1}), and a
 * wielder who is in water gets Dolphin's Grace. Bosses are never slowed. The thrown item is read from the trident's
 * saved data ({@code "Trident"} key) because the {@code tridentItem} field and {@code getPickupItem()} are not public.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class UndertowEvents {
    public static final int BASE_SLOW_TICKS = 40;
    public static final int SLOW_TICKS_PER_LEVEL = 20;
    public static final int DOLPHINS_GRACE_TICKS = 60;

    private UndertowEvents() {}

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        Entity direct = source.getDirectEntity();
        LivingEntity wielder;
        ItemStack weapon;
        if (direct instanceof ThrownTrident trident && source.getEntity() instanceof LivingEntity owner) {
            wielder = owner;
            weapon = ItemStack.of(trident.saveWithoutId(new CompoundTag()).getCompound("Trident"));
        } else if (source.is(DamageTypes.PLAYER_ATTACK) && source.getEntity() instanceof Player player && direct == player) {
            wielder = player;
            weapon = player.getMainHandItem();
        } else {
            return;
        }
        int level = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.UNDERTOW.get(), weapon);
        if (level > 0) {
            applyEffects(event.getEntity(), wielder, level, event.getEntity().isInWater(), wielder.isInWater());
        }
    }

    /** Core of the effect, with the water checks passed in so it can be tested without real water. */
    public static void applyEffects(LivingEntity target, LivingEntity wielder, int level, boolean targetInWater, boolean wielderInWater) {
        if (!targetInWater) {
            return;
        }
        if (!target.getType().is(Tags.EntityTypes.BOSSES)) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    BASE_SLOW_TICKS + SLOW_TICKS_PER_LEVEL * level, level - 1), wielder);
        }
        if (wielderInWater) {
            wielder.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, DOLPHINS_GRACE_TICKS, 0));
        }
    }
}
