package com.relicweave.infusion.effects;

import com.relicweave.RelicWeave;
import com.relicweave.infusion.InfusionType;
import com.relicweave.infusion.Infusions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Pursuit (leggings): killing a hostile mob grants Speed II for {@value #PURSUIT_DURATION_TICKS} ticks, at most once
 * per {@value #PURSUIT_COOLDOWN_TICKS} ticks. The cooldown is longer than the burst and a running burst is never
 * extended, so it cannot be chained into permanent speed. Cooldowns are kept in memory and cleared when a server
 * starts or stops.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class PursuitEffect {
    public static final int PURSUIT_DURATION_TICKS = 60;
    public static final int PURSUIT_COOLDOWN_TICKS = 200;
    /** Speed II. */
    public static final int PURSUIT_AMPLIFIER = 1;

    private static final Map<UUID, Integer> LAST_BURST = new HashMap<>();

    private PursuitEffect() {}

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Enemy && event.getSource().getEntity() instanceof ServerPlayer player) {
            onKill(player);
        }
    }

    /** Applies the burst if {@code player} wears Pursuit and is off cooldown. Returns true if it was applied. */
    public static boolean onKill(ServerPlayer player) {
        if (!Infusions.isWearing(player, InfusionType.PURSUIT)) {
            return false;
        }
        int now = player.server.getTickCount();
        Integer last = LAST_BURST.get(player.getUUID());
        if (last != null && now - last < PURSUIT_COOLDOWN_TICKS) {
            return false;
        }
        LAST_BURST.put(player.getUUID(), now);
        return player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, PURSUIT_DURATION_TICKS, PURSUIT_AMPLIFIER, true, false, true));
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        LAST_BURST.clear();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        LAST_BURST.clear();
    }
}
