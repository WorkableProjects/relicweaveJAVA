package com.relicweave.vestige.hazard.field;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.VestigeConfig;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.hazard.HazardProtection;
import com.relicweave.vestige.registry.VestigeSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Hushfield: in {@code #relicweave:hushfield} biomes each player has a noise level from 0 to 100. Sprinting, jumping,
 * fighting, breaking blocks and shooting raise it, it decays steadily, and crouching halves every gain. At
 * {@link #LOUD} or more, Hushfield mobs (Phase 2) hear the player. Server-side only; state is transient.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class HushNoise {
    public static final float MAX = 100.0F;
    public static final float LOUD = 50.0F;
    public static final float SPRINT_GAIN = 2.0F;
    public static final float JUMP_GAIN = 5.0F;
    public static final float ATTACK_GAIN = 15.0F;
    public static final float BREAK_GAIN = 10.0F;
    public static final float SHOOT_GAIN = 8.0F;
    /** Decay per {@link FieldHazards#INTERVAL}-tick step. */
    public static final float DECAY = 2.5F;

    private static final Map<UUID, Float> NOISE = new HashMap<>();
    private static final Set<UUID> IN_HUSHFIELD = new HashSet<>();

    private HushNoise() {}

    static void clear(UUID id) {
        NOISE.remove(id);
        IN_HUSHFIELD.remove(id);
    }

    static void clearAll() {
        NOISE.clear();
        IN_HUSHFIELD.clear();
    }

    static boolean isInHushfield(UUID id) {
        return IN_HUSHFIELD.contains(id);
    }

    private static boolean enabled() {
        return !VestigeConfig.SPEC.isLoaded() || VestigeConfig.HUSHFIELD.get();
    }

    // ---- Pure arithmetic ----

    /** Noise after adding {@code amount}: crouching halves it, protection scales it, clamped to 0..100. */
    public static float gain(float current, float amount, boolean crouching, double reduction) {
        float scaled = (float) (amount * (crouching ? 0.5 : 1.0) * (1.0 - Mth.clamp(reduction, 0.0, 1.0)));
        return Mth.clamp(current + scaled, 0.0F, MAX);
    }

    /** Noise after one {@link FieldHazards#INTERVAL}-tick step: sprint gain (if any), then decay. */
    public static float step(float current, boolean sprinting, boolean crouching, double reduction) {
        float next = sprinting ? gain(current, SPRINT_GAIN, crouching, reduction) : current;
        return Mth.clamp(next - DECAY, 0.0F, MAX);
    }

    // ---- Public API for Phase 2 mobs ----

    /** Current noise of {@code player} (0 outside a Hushfield). */
    public static float level(Player player) {
        return NOISE.getOrDefault(player.getUUID(), 0.0F);
    }

    /** True if the player is loud enough for Hushfield mobs to hear. */
    public static boolean isLoud(Player player) {
        return level(player) >= LOUD;
    }

    /** The loudest player within {@code radius} of {@code pos} who has any noise, or null. */
    @Nullable
    public static ServerPlayer loudestPlayerNear(ServerLevel level, Vec3 pos, double radius) {
        ServerPlayer best = null;
        float bestLevel = 0.0F;
        for (ServerPlayer player : level.players()) {
            float noise = level(player);
            if (noise > bestLevel && player.position().distanceToSqr(pos) <= radius * radius) {
                best = player;
                bestLevel = noise;
            }
        }
        return best;
    }

    // ---- Server logic ----

    private static boolean inHushfield(ServerPlayer player) {
        return enabled() && FieldHazards.isAffected(player)
                && player.serverLevel().getBiome(player.blockPosition()).is(VestigeKeys.HUSHFIELD);
    }

    /** Adds noise (event gains) and raises the alert when crossing {@link #LOUD} upward. */
    static void addNoise(ServerPlayer player, float amount) {
        UUID id = player.getUUID();
        float old = NOISE.getOrDefault(id, 0.0F);
        double reduction = HazardProtection.reduction(player, HazardProtection.Hazard.HUSHFIELD);
        float next = gain(old, amount, player.isShiftKeyDown(), reduction);
        store(player, old, next);
    }

    private static void store(ServerPlayer player, float old, float next) {
        UUID id = player.getUUID();
        if (next <= 0.0F) {
            NOISE.remove(id);
        } else {
            NOISE.put(id, next);
        }
        if (old < LOUD && next >= LOUD) {
            player.displayClientMessage(Component.translatable("message.relicweave.hush.heard"), true);
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                    VestigeSounds.HUSH_ALERT.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        }
    }

    static void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        if (!inHushfield(player)) {
            clear(id);
            return;
        }
        IN_HUSHFIELD.add(id);
        float old = NOISE.getOrDefault(id, 0.0F);
        double reduction = HazardProtection.reduction(player, HazardProtection.Hazard.HUSHFIELD);
        store(player, old, step(old, player.isSprinting(), player.isShiftKeyDown(), reduction));
    }

    private static void event(@Nullable Player player, float amount) {
        if (player instanceof ServerPlayer sp && inHushfield(sp)) {
            addNoise(sp, amount);
        }
    }

    // ---- Events ----

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        event(event.getEntity() instanceof Player p ? p : null, JUMP_GAIN);
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        event(event.getEntity(), ATTACK_GAIN);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        event(event.getPlayer(), BREAK_GAIN);
    }

    @SubscribeEvent
    public static void onShoot(ArrowLooseEvent event) {
        event(event.getEntity(), SHOOT_GAIN);
    }
}
