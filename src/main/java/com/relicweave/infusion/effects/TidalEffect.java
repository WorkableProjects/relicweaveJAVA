package com.relicweave.infusion.effects;

import com.relicweave.RelicWeave;
import com.relicweave.infusion.InfusionType;
import com.relicweave.infusion.Infusions;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Tidal (helmet): while the wearer's eyes are in water, Water Breathing is refreshed every
 * {@value #CHECK_INTERVAL_TICKS} ticks. It never shortens a longer Water Breathing already active (potion, Conduit).
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class TidalEffect {
    /** How often the effect is checked, in ticks. */
    public static final int CHECK_INTERVAL_TICKS = 10;
    /** Duration of each Water Breathing refresh, in ticks (longer than the check interval, so it never flickers). */
    public static final int DURATION_TICKS = 60;

    private TidalEffect() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (event.phase == TickEvent.Phase.END && !player.level().isClientSide && player.tickCount % CHECK_INTERVAL_TICKS == 0) {
            apply(player);
        }
    }

    /** Grants the effect if {@code player} wears Tidal with the eyes in water. Returns true if it was (re)applied. */
    public static boolean apply(Player player) {
        if (!Infusions.isWearing(player, InfusionType.TIDAL) || !player.isEyeInFluid(FluidTags.WATER)) {
            return false;
        }
        MobEffectInstance existing = player.getEffect(MobEffects.WATER_BREATHING);
        if (existing != null && existing.getDuration() > DURATION_TICKS) {
            return false;
        }
        return player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, DURATION_TICKS, 0, true, false, true));
    }
}
