package com.relicweave.enchantment.vestige;

import com.relicweave.RelicWeave;
import com.relicweave.registry.ModEnchantments;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.cycle.ConcordanceCycle;
import com.relicweave.vestige.cycle.CyclePhase;
import com.relicweave.vestige.cycle.VestigeCycleEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Cyclebound (leggings): while in The Vestige during Resonance, a transient movement-speed modifier of +5 % / +10 %
 * ({@code MULTIPLY_BASE}, fixed UUID). Re-evaluated every 20 ticks and on phase, equipment and dimension changes and on
 * logout. A one-line action-bar note appears when it switches on.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class CycleboundEvents {
    public static final UUID MODIFIER_ID = UUID.fromString("5b1c7a3e-4d92-4f0a-9e6b-0c7a1d2e8f41");
    public static final double SPEED_PER_LEVEL = 0.05;
    private static final int CHECK_INTERVAL_TICKS = 20;

    private CycleboundEvents() {}

    /** Re-evaluates {@code player} against the live Concordance Cycle phase; only active inside The Vestige. */
    public static void update(ServerPlayer player) {
        update(player, player.level().dimension() == VestigeKeys.THE_VESTIGE ? ConcordanceCycle.phase(player.level()) : null);
    }

    /**
     * Adds, replaces or removes the modifier for {@code phase} (null means "not in The Vestige"; callers that pass a
     * phase vouch for the dimension). Returns true if the modifier is active afterwards.
     */
    public static boolean update(ServerPlayer player, @Nullable CyclePhase phase) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return false;
        }
        int level = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.CYCLEBOUND.get(), player.getItemBySlot(EquipmentSlot.LEGS));
        boolean active = level > 0 && phase == CyclePhase.RESONANCE;
        AttributeModifier existing = speed.getModifier(MODIFIER_ID);
        if (!active) {
            if (existing != null) {
                speed.removeModifier(MODIFIER_ID);
            }
            return false;
        }
        double amount = SPEED_PER_LEVEL * level;
        if (existing != null && existing.getAmount() == amount) {
            return true;
        }
        boolean wasOn = existing != null;
        if (wasOn) {
            speed.removeModifier(MODIFIER_ID);
        }
        speed.addTransientModifier(new AttributeModifier(MODIFIER_ID, RelicWeave.MOD_ID + ":cyclebound", amount, AttributeModifier.Operation.MULTIPLY_BASE));
        if (!wasOn) {
            player.displayClientMessage(Component.translatable("message.relicweave.cyclebound.on"), true);
        }
        return true;
    }

    /** Removes the modifier unconditionally. */
    public static void clear(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(MODIFIER_ID);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player
                && player.tickCount % CHECK_INTERVAL_TICKS == 0) {
            update(player);
        }
    }

    @SubscribeEvent
    public static void onPhaseChanged(VestigeCycleEvent.PhaseChanged event) {
        for (ServerPlayer player : event.getLevel().players()) {
            update(player, event.getTo());
        }
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getSlot() == EquipmentSlot.LEGS && event.getEntity() instanceof ServerPlayer player) {
            update(player);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            update(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }
}
