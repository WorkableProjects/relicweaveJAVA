package com.relicweave.vestige.hazard.rift;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.VestigeConfig;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.cycle.CyclePhase;
import com.relicweave.vestige.cycle.CycleTicker;
import com.relicweave.vestige.hazard.HazardProtection;
import com.relicweave.vestige.hazard.HazardStatus;
import com.relicweave.vestige.hazard.RiftRegistry;
import com.relicweave.vestige.network.VestigeNetwork;
import com.relicweave.vestige.network.VestigeNetwork.HazardSyncPacket;
import com.relicweave.vestige.registry.VestigeSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Rift Saturation: a 0-100 meter that rises while a player is exposed (outdoors in a {@code rift_saturated} biome, or
 * within 5 blocks of a rift fissure) inside The Vestige and decays otherwise. Full meter: Rift Sickness for a few
 * seconds and the meter drops to 60. It never deals damage.
 * <p>
 * The meter lives in the player's persisted data ({@link Player#PERSISTED_NBT_TAG}, key {@code relicweave:rift_exposure}),
 * so logging out, restarting the server or changing dimension never clears it. Dying does: a respawn is a fresh start
 * (see {@link #onClone}).
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class RiftSaturation {
    public static final String KEY = RelicWeave.MOD_ID + ":rift_exposure";
    public static final float WARNING_1 = 50.0F;
    public static final float WARNING_2 = 80.0F;
    public static final float MAX = 100.0F;
    public static final float AFTER_SICKNESS = 60.0F;
    /** Radius (blocks) in which a rift fissure exposes a player. */
    public static final int FISSURE_RADIUS = 5;
    /** Ticks between meter updates. */
    public static final int INTERVAL = 10;

    private static final Map<UUID, HazardSyncPacket> LAST_SENT = new HashMap<>();

    private RiftSaturation() {}

    // ---- Storage ----

    public static float getExposure(Player player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getFloat(KEY);
    }

    public static void setExposure(Player player, float value) {
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putFloat(KEY, Mth.clamp(value, 0.0F, MAX));
        root.put(Player.PERSISTED_NBT_TAG, persisted);
    }

    // ---- Pure math ----

    private static double perSecondGain() {
        return VestigeConfig.SPEC.isLoaded() ? VestigeConfig.RIFT_EXPOSURE_PER_SECOND.get() : 2.0;
    }

    private static double perSecondDecay() {
        return VestigeConfig.SPEC.isLoaded() ? VestigeConfig.RIFT_DECAY_PER_SECOND.get() : 3.0;
    }

    private static double convergenceMultiplier() {
        return VestigeConfig.SPEC.isLoaded() ? VestigeConfig.CONVERGENCE_HAZARD_MULTIPLIER.get() : 2.0;
    }

    /**
     * One update (every {@link #INTERVAL} ticks) of the meter, clamped to 0..100.
     *
     * @param reduction protection fraction in [0, 1] already combined by {@link HazardProtection}
     */
    public static float step(float current, boolean exposed, boolean convergence, double reduction) {
        double next;
        if (exposed) {
            double gain = perSecondGain() / 2.0 * (convergence ? convergenceMultiplier() : 1.0);
            next = current + gain * (1.0 - Mth.clamp(reduction, 0.0, 1.0));
        } else {
            next = current - perSecondDecay() / 2.0;
        }
        return (float) Mth.clamp(next, 0.0, (double) MAX);
    }

    // ---- Server logic ----

    /** True if Rift Saturation is enabled in the server config. */
    public static boolean enabled() {
        return !VestigeConfig.SPEC.isLoaded() || VestigeConfig.RIFT_SATURATION.get();
    }

    /** Whether {@code player} currently stands somewhere that builds Rift Saturation. */
    public static boolean isExposed(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (level.dimension() != VestigeKeys.THE_VESTIGE) {
            return false;
        }
        BlockPos pos = player.blockPosition();
        if (level.getBiome(pos).is(VestigeKeys.RIFT_SATURATED) && level.canSeeSky(pos.above())) {
            return true;
        }
        return nearFissure(level, pos);
    }

    /** True if a rift fissure point of interest is within {@link #FISSURE_RADIUS} blocks of {@code pos}. */
    public static boolean nearFissure(ServerLevel level, BlockPos pos) {
        return level.getPoiManager().findClosest(holder -> holder.value() == RiftRegistry.RIFT_FISSURE_POI.get(),
                pos, FISSURE_RADIUS, PoiManager.Occupancy.ANY).isPresent();
    }

    /**
     * Applies one {@link #step} to {@code player} and handles warnings and the full-meter debuff. Returns the new
     * meter value. Exposed so tests can drive the meter without a Vestige level.
     */
    public static float update(ServerPlayer player, boolean exposed, boolean convergence) {
        float old = getExposure(player);
        boolean gaining = exposed && enabled();
        double reduction = gaining ? HazardProtection.reduction(player, HazardProtection.Hazard.RIFT_SATURATION) : 0.0;
        float next = step(old, gaining, convergence, reduction);
        if (next >= MAX) {
            int seconds = VestigeConfig.SPEC.isLoaded() ? VestigeConfig.RIFT_DEBUFF_SECONDS.get() : 8;
            player.addEffect(new MobEffectInstance(RiftRegistry.RIFT_SICKNESS.get(), seconds * 20, 0, false, true, true));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    VestigeSounds.RIFT_SICKNESS.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            next = AFTER_SICKNESS;
        } else if (old < WARNING_2 && next >= WARNING_2) {
            warn(player, "message.relicweave.rift.warning_80");
        } else if (old < WARNING_1 && next >= WARNING_1) {
            warn(player, "message.relicweave.rift.warning_50");
        }
        if (next != old) {
            setExposure(player, next);
        }
        return next;
    }

    private static void warn(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                VestigeSounds.RIFT_WARNING.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    private static HazardSyncPacket packetFor(ServerPlayer player, boolean inVestige, boolean exposed) {
        if (!inVestige) {
            return new HazardSyncPacket(0, 0, 0);
        }
        int flags = HazardStatus.extraFlags(player);
        if (exposed) {
            flags |= HazardSyncPacket.IN_RIFT_ZONE;
        }
        if (player.hasEffect(RiftRegistry.RIFT_SICKNESS.get())) {
            flags |= HazardSyncPacket.RIFT_SICKNESS;
        }
        int protection = (int) Math.round(HazardProtection.reduction(player, HazardProtection.Hazard.RIFT_SATURATION) * 100.0);
        return new HazardSyncPacket(Math.round(getExposure(player)), flags, protection);
    }

    private static void tick(ServerPlayer player) {
        boolean inVestige = player.serverLevel().dimension() == VestigeKeys.THE_VESTIGE;
        float exposure = getExposure(player);
        UUID id = player.getUUID();
        HazardSyncPacket last = LAST_SENT.get(id);
        if (!inVestige && exposure <= 0.0F && last == null) {
            return;
        }
        boolean exposed = false;
        boolean active = !player.isCreative() && !player.isSpectator();
        if (inVestige && active) {
            exposed = isExposed(player);
        }
        if (exposure > 0.0F || exposed) {
            boolean convergence = inVestige && CycleTicker.currentPhase(player.server) == CyclePhase.CONVERGENCE;
            update(player, exposed, convergence);
        }
        HazardSyncPacket packet = packetFor(player, inVestige, exposed);
        if (!packet.equals(last)) {
            VestigeNetwork.send(player, packet);
            if (inVestige || packet.riftExposure() != 0 || packet.flags() != 0) {
                LAST_SENT.put(id, packet);
            } else {
                LAST_SENT.remove(id);
            }
        }
    }

    // ---- Events ----

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player
                && player.tickCount % INTERVAL == 0) {
            tick(player);
        }
    }

    /** Dying resets the meter: a respawn is a fresh start. Everything else (relog, restart, dimension) keeps it. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            setExposure(event.getEntity(), 0.0F);
        }
        LAST_SENT.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        LAST_SENT.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        LAST_SENT.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SENT.remove(event.getEntity().getUUID());
    }
}
