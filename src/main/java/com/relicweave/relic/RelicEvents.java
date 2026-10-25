package com.relicweave.relic;

import com.relicweave.RelicWeave;
import com.relicweave.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Server-side passives of the Tidal Charm, Featherwoven Charm and Guardian Relic, plus re-applying the cosmetic
 * cooldown overlay after login, respawn and dimension change (the authoritative value is {@link RelicCooldowns}).
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class RelicEvents {
    /** Tidal: duration (ticks) of the Water Breathing it applies, refreshed every {@link #TIDAL_INTERVAL_TICKS}. */
    public static final int TIDAL_BREATHING_TICKS = 60;
    public static final int TIDAL_INTERVAL_TICKS = 10;
    /** Featherwoven: fall damage multiplier. */
    public static final float FEATHERWOVEN_FALL_MULTIPLIER = 0.5F;
    /** Featherwoven: fall distance (blocks) at which Slow Falling kicks in. */
    public static final float DANGEROUS_FALL = 12.0F;
    /** Featherwoven: Slow Falling duration in ticks. */
    public static final int SLOW_FALL_TICKS = 40;
    /** Guardian: shield triggers when a non-lethal hit leaves at most this much health (half-hearts). */
    public static final float CRITICAL_HEALTH = 6.0F;
    /** Guardian: absorption granted (half-hearts) and its duration (ticks). */
    public static final float GUARDIAN_SHIELD = 8.0F;
    public static final int GUARDIAN_SHIELD_TICKS = 200;

    private RelicEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % TIDAL_INTERVAL_TICKS == 0) {
            tidalTick(player);
        }
        featherwovenTick(player);
    }

    /** Tidal Charm: water breathing while the eyes are in water. Never shortens a longer existing effect. */
    public static void tidalTick(ServerPlayer player) {
        if (!player.isEyeInFluid(FluidTags.WATER) || !RelicLocator.has(player, RelicType.TIDAL)) {
            return;
        }
        MobEffectInstance existing = player.getEffect(MobEffects.WATER_BREATHING);
        if (existing == null || existing.getDuration() < TIDAL_BREATHING_TICKS) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, TIDAL_BREATHING_TICKS, 0, true, false, true));
        }
    }

    /** Featherwoven Charm: Slow Falling during a dangerous fall, once per cooldown. No hover or flight. */
    public static void featherwovenTick(ServerPlayer player) {
        if (player.fallDistance < DANGEROUS_FALL || player.getAbilities().flying || player.isFallFlying() || player.isInWater()
                || !RelicCooldowns.isReady(player, RelicType.FEATHERWOVEN) || !RelicLocator.has(player, RelicType.FEATHERWOVEN)) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALL_TICKS, 0, true, false, true));
        RelicCooldowns.start(player, RelicType.FEATHERWOVEN);
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && RelicLocator.has(player, RelicType.FEATHERWOVEN)) {
            event.setDamageMultiplier(event.getDamageMultiplier() * FEATHERWOVEN_FALL_MULTIPLIER);
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            guardianCheck(player, event.getAmount());
        }
    }

    /**
     * Guardian Relic: a hit of final damage {@code amount} that is not lethal but leaves at most
     * {@link #CRITICAL_HEALTH} grants a short absorption shield, then goes on cooldown. Never cancels or prevents death.
     */
    public static boolean guardianCheck(ServerPlayer player, float amount) {
        float remaining = player.getHealth() - amount;
        if (amount <= 0.0F || remaining <= 0.0F || remaining > CRITICAL_HEALTH
                || !RelicCooldowns.isReady(player, RelicType.GUARDIAN) || !RelicLocator.has(player, RelicType.GUARDIAN)) {
            return false;
        }
        AbsorptionShields.grant(player, GUARDIAN_SHIELD, GUARDIAN_SHIELD_TICKS);
        player.level().playSound(null, player.blockPosition(), ModSounds.RELIC_ACTIVATE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        RelicCooldowns.start(player, RelicType.GUARDIAN);
        return true;
    }

    // ---- Cosmetic cooldown overlay ----

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        reapplyOverlays(event.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        reapplyOverlays(event.getEntity());
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        reapplyOverlays(event.getEntity());
    }

    public static void reapplyOverlays(Player entity) {
        if (entity instanceof ServerPlayer player) {
            for (RelicType type : RelicType.values()) {
                if (type.hasCooldown()) {
                    RelicCooldowns.showOverlay(player, type, RelicCooldowns.remainingTicks(player, type));
                }
            }
        }
    }
}
