package com.relicweave.quest;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.cycle.CyclePhase;
import com.relicweave.vestige.cycle.VestigeCycleEvent;
import com.relicweave.vestige.hazard.RiftRegistry;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The quest-line steps fired through {@link QuestTrigger}, and the listeners for steps that come from events. The
 * quest line itself is data: the advancement tabs {@code relicweave:relicweave/*} (machines, 0.0.1-0.0.3) and
 * {@code relicweave:vestige/*} (0.0.4).
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class QuestEvents {
    public static final QuestTrigger TRIGGER = new QuestTrigger();

    public static final String RELIC_ACTIVATED = "relic_activated";
    public static final String GATE_ACTIVATED = "gate_activated";
    public static final String RETURNED_FROM_VESTIGE = "returned_from_vestige";
    public static final String WITNESSED_RESONANCE = "witnessed_resonance";
    public static final String OUTLASTED_CONVERGENCE = "outlasted_convergence";
    public static final String RIFT_SICKNESS = "rift_sickness";
    public static final String TERMINAL_DECODED = "terminal_decoded";
    public static final String LOOM_AUGMENT = "loom_augment";
    public static final String BOSS_SUMMONED = "boss_summoned";
    public static final String SEAL_GATE_PASSED = "seal_gate_passed";

    private QuestEvents() {}

    /** Registers the trigger; call once during common setup. */
    public static void register() {
        CriteriaTriggers.register(TRIGGER);
    }

    public static void fire(ServerPlayer player, String event) {
        TRIGGER.trigger(player, event);
    }

    @SubscribeEvent
    public static void onPhaseChanged(VestigeCycleEvent.PhaseChanged event) {
        for (ServerPlayer player : event.getLevel().players()) {
            if (event.getTo() == CyclePhase.RESONANCE) {
                fire(player, WITNESSED_RESONANCE);
            }
            if (event.getFrom() == CyclePhase.CONVERGENCE && player.isAlive()) {
                fire(player, OUTLASTED_CONVERGENCE);
            }
        }
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getEffectInstance().getEffect() == RiftRegistry.RIFT_SICKNESS.get()
                && player.level().dimension() == VestigeKeys.THE_VESTIGE) {
            fire(player, RIFT_SICKNESS);
        }
    }
}
