package com.relicweave.relic;

import com.relicweave.RelicWeave;
import com.relicweave.config.RelicWeaveConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Shared temporary absorption for the Guardian Relic, Soulguard and Warding. All three go through {@link #grant}, so
 * together they never raise absorption above the configured {@code absorptionCap}, and any of their absorption that is
 * still left when the shield expires is removed again (absorption from other sources, e.g. golden apples, is kept).
 * <p>
 * Granted amounts live in the entity's (non-persisted) Forge data: dying clears them along with the absorption itself.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class AbsorptionShields {
    private static final String TAG = RelicWeave.MOD_ID + ":shield";
    private static final String AMOUNT = "amount";
    private static final String EXPIRES = "expires";

    private AbsorptionShields() {}

    private static int cap() {
        return RelicWeaveConfig.SPEC.isLoaded() ? RelicWeaveConfig.ABSORPTION_CAP.get() : 8;
    }

    /**
     * Adds up to {@code amount} absorption (half-hearts) for {@code durationTicks}, never exceeding the cap. Refreshes
     * the expiry of the shared shield. Returns the amount actually added (0 when already at the cap).
     */
    public static float grant(LivingEntity entity, float amount, int durationTicks) {
        float current = entity.getAbsorptionAmount();
        float added = Math.max(0.0F, Math.min(amount, cap() - current));
        CompoundTag shield = entity.getPersistentData().getCompound(TAG);
        if (added > 0.0F) {
            entity.setAbsorptionAmount(current + added);
            shield.putFloat(AMOUNT, shield.getFloat(AMOUNT) + added);
        }
        if (shield.getFloat(AMOUNT) > 0.0F) {
            shield.putLong(EXPIRES, entity.level().getGameTime() + durationTicks);
            entity.getPersistentData().put(TAG, shield);
        }
        return added;
    }

    /** Absorption granted through this class that has not expired or been used up yet. */
    public static float active(LivingEntity entity) {
        return Math.min(entity.getAbsorptionAmount(), entity.getPersistentData().getCompound(TAG).getFloat(AMOUNT));
    }

    /** Removes what is left of an expired shield. Called every tick for players; call it yourself for other mobs. */
    public static void tick(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(TAG)) {
            return;
        }
        CompoundTag shield = data.getCompound(TAG);
        float remaining = Math.min(entity.getAbsorptionAmount(), shield.getFloat(AMOUNT));
        if (remaining <= 0.0F) {
            data.remove(TAG); // used up by damage
        } else if (entity.level().getGameTime() >= shield.getLong(EXPIRES)) {
            entity.setAbsorptionAmount(entity.getAbsorptionAmount() - remaining);
            data.remove(TAG);
        } else if (remaining < shield.getFloat(AMOUNT)) {
            shield.putFloat(AMOUNT, remaining);
            data.put(TAG, shield);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            tick(event.player);
        }
    }
}
