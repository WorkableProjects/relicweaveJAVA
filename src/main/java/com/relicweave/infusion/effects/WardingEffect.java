package com.relicweave.infusion.effects;

import com.relicweave.RelicWeave;
import com.relicweave.infusion.InfusionType;
import com.relicweave.infusion.Infusions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Warding (chestplate): one ward charge that softens the next hit by {@value #WARD_REDUCTION} (2 hearts), then
 * recharges {@value #WARD_RECHARGE_TICKS} ticks (20 s) after it was used. Never more than one charge, so it never
 * stacks. The charge belongs to the player, not the item: taking the chestplate off keeps the timer. It lives in the
 * player's non-persisted Forge data, so dying resets it. Damage that bypasses invulnerability is not reduced.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class WardingEffect {
    /** Ticks until a used (or newly started) charge is ready again. */
    public static final int WARD_RECHARGE_TICKS = 400;
    /** Damage removed from one hit, in half-hearts. */
    public static final float WARD_REDUCTION = 4.0F;

    private static final String TAG = RelicWeave.MOD_ID + ":warding";
    private static final String READY = "ready";
    private static final String READY_AT = "ready_at";
    private static final int CHECK_INTERVAL_TICKS = 10;

    private WardingEffect() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player
                && player.tickCount % CHECK_INTERVAL_TICKS == 0) {
            refresh(player);
        }
    }

    /**
     * Starts the recharge timer if the wearer has no record yet and flips the charge to ready once its time has come
     * (with a single End Rod puff). Does nothing unless the player wears Warding.
     */
    public static void refresh(ServerPlayer player) {
        if (!Infusions.isWearing(player, InfusionType.WARDING)) {
            return;
        }
        long now = player.level().getGameTime();
        CompoundTag data = player.getPersistentData();
        if (!data.contains(TAG)) {
            store(player, false, now + WARD_RECHARGE_TICKS);
            return;
        }
        CompoundTag ward = data.getCompound(TAG);
        if (!ward.getBoolean(READY) && now >= ward.getLong(READY_AT)) {
            store(player, true, 0L);
            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
            }
        }
    }

    /** True if {@code player} has a ready charge (as of the last {@link #refresh}). */
    public static boolean isCharged(ServerPlayer player) {
        return player.getPersistentData().getCompound(TAG).getBoolean(READY);
    }

    /** Marks the charge as ready right now (no particles). */
    public static void charge(ServerPlayer player) {
        store(player, true, 0L);
    }

    private static void store(ServerPlayer player, boolean ready, long readyAt) {
        CompoundTag ward = new CompoundTag();
        ward.putBoolean(READY, ready);
        ward.putLong(READY_AT, readyAt);
        player.getPersistentData().put(TAG, ward);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0.0F
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || !Infusions.isWearing(player, InfusionType.WARDING)) {
            return;
        }
        refresh(player);
        if (!isCharged(player)) {
            return;
        }
        event.setAmount(Math.max(0.0F, event.getAmount() - WARD_REDUCTION));
        store(player, false, player.level().getGameTime() + WARD_RECHARGE_TICKS);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.6F, 1.0F);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.4, 0.5, 0.4, 0.1);
        }
    }
}
