package com.relicweave.enchantment.upbound;

import com.relicweave.RelicWeave;
import com.relicweave.registry.ModEnchantments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side Upbound validation. Player movement is client-authoritative, so the client applies the extra jump
 * velocity itself; the server only checks that the jump is legitimate (boots level, airborne, per-flight budget) and
 * clears fall distance so height gained before the air jump never turns into fall damage.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class UpboundHandler {
    /** Upward velocity of an air jump; shared with the client, which applies it locally. */
    public static final double JUMP_VELOCITY = 0.52;

    private static final Map<UUID, Integer> AIR_JUMPS_USED = new HashMap<>();

    private UpboundHandler() {}

    /** Validates and records an air jump. Returns true if accepted. Must run on the server thread. */
    public static boolean tryAirJump(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || player.onGround()
                || player.getAbilities().flying || player.isFallFlying() || player.isPassenger()) {
            return false;
        }
        int level = EnchantmentHelper.getItemEnchantmentLevel(
                ModEnchantments.UPBOUND.get(), player.getItemBySlot(EquipmentSlot.FEET));
        if (level <= 0) {
            return false;
        }
        int used = AIR_JUMPS_USED.getOrDefault(player.getUUID(), 0);
        if (used >= level) {
            return false;
        }

        AIR_JUMPS_USED.put(player.getUUID(), used + 1);
        player.resetFallDistance();

        ServerLevel serverLevel = player.serverLevel();
        serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(),
                8, 0.25, 0.05, 0.25, 0.02);
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WOOL_STEP, SoundSource.PLAYERS, 0.5F, 1.4F);
        return true;
    }

    /** Restores the air-jump budget; called when the player touches the ground. */
    public static void onLanded(ServerPlayer player) {
        AIR_JUMPS_USED.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player && player.onGround()) {
            onLanded(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        AIR_JUMPS_USED.clear();
    }
}
