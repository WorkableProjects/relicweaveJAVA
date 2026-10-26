package com.relicweave.infusion.effects.vestige;

import com.relicweave.RelicWeave;
import com.relicweave.infusion.InfusionType;
import com.relicweave.infusion.Infusions;
import com.relicweave.vestige.hazard.field.StarfallImpacts;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Sealguard (chestplate): cancels one {@code relicweave:starfall} damage event, then recharges for
 * {@value #RECHARGE_TICKS} ticks. The recharge deadline is stored in the player's persistent data. Never touches any
 * other damage type; Warding remains the combat shield.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class SealguardEffect {
    public static final int RECHARGE_TICKS = 900;

    private static final String TAG = RelicWeave.MOD_ID + ":sealguard";
    private static final String READY_AT = "ready_at";
    private static final String RECHARGING = "recharging";
    private static final int CHECK_INTERVAL_TICKS = 10;

    private SealguardEffect() {}

    /** True if the player may absorb a hit right now. */
    public static boolean isReady(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData().getCompound(TAG);
        return !tag.getBoolean(RECHARGING) || player.level().getGameTime() >= tag.getLong(READY_AT);
    }

    /** Starts the recharge. */
    public static void startRecharge(ServerPlayer player) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(RECHARGING, true);
        tag.putLong(READY_AT, player.level().getGameTime() + RECHARGE_TICKS);
        player.getPersistentData().put(TAG, tag);
    }

    public static boolean isStarfall(DamageSource source) {
        return source.typeHolder().is(StarfallImpacts.DAMAGE_TYPE);
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isStarfall(event.getSource())
                || !Infusions.isWearing(player, InfusionType.SEALGUARD) || !isReady(player)) {
            return;
        }
        event.setCanceled(true);
        startRecharge(player);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.6F, 1.4F);
        player.displayClientMessage(Component.translatable("message.relicweave.sealguard.blocked"), true);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.tickCount % CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        CompoundTag tag = player.getPersistentData().getCompound(TAG);
        if (tag.getBoolean(RECHARGING) && player.level().getGameTime() >= tag.getLong(READY_AT)) {
            tag.putBoolean(RECHARGING, false);
            player.getPersistentData().put(TAG, tag);
            if (Infusions.isWearing(player, InfusionType.SEALGUARD)) {
                player.displayClientMessage(Component.translatable("message.relicweave.sealguard.ready"), true);
            }
        }
    }
}
