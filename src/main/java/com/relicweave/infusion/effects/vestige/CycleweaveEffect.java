package com.relicweave.infusion.effects.vestige;

import com.relicweave.RelicWeave;
import com.relicweave.infusion.InfusionType;
import com.relicweave.infusion.Infusions;
import com.relicweave.vestige.cycle.VestigeCycleEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Cycleweave (leggings): when the Concordance Cycle changes phase, every wearer in that level gets Speed II and Jump
 * Boost I for {@value #DURATION_TICKS} ticks, at most once per {@value #COOLDOWN_TICKS} ticks (persisted).
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class CycleweaveEffect {
    public static final int DURATION_TICKS = 200;
    public static final int COOLDOWN_TICKS = 1200;

    private static final String TAG = RelicWeave.MOD_ID + ":cycleweave";
    private static final String READY_AT = "ready_at";

    private CycleweaveEffect() {}

    /** Grants the burst if the wearer has Cycleweave and the cooldown is over. Returns whether it was granted. */
    public static boolean trigger(ServerPlayer player) {
        if (!Infusions.isWearing(player, InfusionType.CYCLEWEAVE)) {
            return false;
        }
        long now = player.level().getGameTime();
        CompoundTag tag = player.getPersistentData().getCompound(TAG);
        if (tag.contains(READY_AT) && now < tag.getLong(READY_AT)) {
            return false;
        }
        CompoundTag next = new CompoundTag();
        next.putLong(READY_AT, now + COOLDOWN_TICKS);
        player.getPersistentData().put(TAG, next);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DURATION_TICKS, 1, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, DURATION_TICKS, 0, false, true, true));
        return true;
    }

    @SubscribeEvent
    public static void onPhaseChanged(VestigeCycleEvent.PhaseChanged event) {
        for (ServerPlayer player : event.getLevel().players()) {
            trigger(player);
        }
    }
}
