package com.relicweave.enchantment.attunement;

import com.relicweave.RelicWeave;
import com.relicweave.registry.ModEnchantments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Reprisal (shield): blocking a melee attack shortly after raising the shield (within {@link #TIMING_WINDOW_TICKS})
 * weakens and slows the attacker. Bosses get Weakness only. Per-player cooldown of {@link #COOLDOWN_TICKS}.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class ReprisalEvents {
    public static final int TIMING_WINDOW_TICKS = 10;
    public static final int COOLDOWN_TICKS = 60;
    public static final int BASE_TICKS = 40;
    public static final int TICKS_PER_LEVEL = 20;

    private static final Map<UUID, Integer> READY_AT_TICK = new HashMap<>();

    private ReprisalEvents() {}

    @SubscribeEvent
    public static void onShieldBlock(ShieldBlockEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)
                || !(event.getDamageSource().getDirectEntity() instanceof LivingEntity attacker)) {
            return;
        }
        int level = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.REPRISAL.get(), player.getUseItem());
        if (level > 0) {
            onBlock(player, attacker, level, player.getTicksUsingItem(), player.server.getTickCount());
        }
    }

    /** Returns whether the attacker was debuffed. */
    public static boolean onBlock(ServerPlayer player, LivingEntity attacker, int level, int ticksUsingShield, int nowTick) {
        if (ticksUsingShield > TIMING_WINDOW_TICKS) {
            return false;
        }
        Integer readyAt = READY_AT_TICK.get(player.getUUID());
        if (readyAt != null && nowTick < readyAt) {
            return false;
        }
        READY_AT_TICK.put(player.getUUID(), nowTick + COOLDOWN_TICKS);
        int duration = BASE_TICKS + TICKS_PER_LEVEL * level;
        attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0), player);
        if (!attacker.getType().is(Tags.EntityTypes.BOSSES)) {
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, level - 1), player);
        }
        return true;
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        READY_AT_TICK.clear();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        READY_AT_TICK.clear();
    }
}
