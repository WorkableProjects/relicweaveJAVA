package com.relicweave.enchantment.attunement;

import com.relicweave.RelicWeave;
import com.relicweave.registry.ModEnchantments;
import com.relicweave.relic.AbsorptionShields;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Soulguard (chestplate): killing a hostile mob grants {@code 2 * level} absorption (half-hearts) for 10 seconds,
 * through the shared {@link AbsorptionShields} cap. At most one grant per player every {@link #COOLDOWN_TICKS}.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class SoulguardEvents {
    public static final float ABSORPTION_PER_LEVEL = 2.0F;
    public static final int DURATION_TICKS = 200;
    public static final int COOLDOWN_TICKS = 40;

    private static final Map<UUID, Integer> READY_AT_TICK = new HashMap<>();

    private SoulguardEvents() {}

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            onKill(player, event.getEntity(), player.server.getTickCount());
        }
    }

    /** Returns the absorption actually granted (0 when not applicable, on cooldown or at the cap). */
    public static float onKill(ServerPlayer player, LivingEntity victim, int nowTick) {
        if (!(victim instanceof Enemy)) {
            return 0.0F;
        }
        int level = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.SOULGUARD.get(), player.getItemBySlot(EquipmentSlot.CHEST));
        if (level <= 0) {
            return 0.0F;
        }
        Integer readyAt = READY_AT_TICK.get(player.getUUID());
        if (readyAt != null && nowTick < readyAt) {
            return 0.0F;
        }
        READY_AT_TICK.put(player.getUUID(), nowTick + COOLDOWN_TICKS);
        return AbsorptionShields.grant(player, ABSORPTION_PER_LEVEL * level, DURATION_TICKS);
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
