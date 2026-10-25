package com.relicweave.relic;

import com.relicweave.RelicWeave;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Lodestone Charm: while toggled on and carried, dropped items within {@link #RANGE} blocks drift toward the player.
 * Only pulls; the actual pickup stays vanilla (inventory space, pickup delay and ownership rules still apply). Items
 * that are still in their pickup delay, or that are reserved for another player, are left alone.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class LodestoneCharm {
    /** Pull radius in blocks. */
    public static final double RANGE = 6.0;
    /** Ticks between pulls. */
    public static final int INTERVAL_TICKS = 2;
    /** Velocity added per pull, in blocks/tick. */
    public static final double PULL = 0.08;
    /** Maximum item speed in blocks/tick after a pull. */
    public static final double MAX_SPEED = 0.6;

    private LodestoneCharm() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.tickCount % INTERVAL_TICKS != 0) {
            return;
        }
        pull(player);
    }

    /** Runs one pull step for {@code player}. Does nothing unless enabled, alive, not a spectator and carrying the charm. */
    public static void pull(ServerPlayer player) {
        if (!RelicActivation.isLodestoneEnabled(player) || player.isSpectator() || !player.isAlive()
                || !RelicLocator.has(player, RelicType.LODESTONE)) {
            return;
        }
        Vec3 eye = player.getEyePosition();
        for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(RANGE),
                e -> !e.hasPickUpDelay() && e.isAlive() && usableBy(e, player.getUUID()))) {
            Vec3 toPlayer = eye.subtract(item.position());
            if (toPlayer.lengthSqr() > RANGE * RANGE || toPlayer.lengthSqr() < 1.0E-4) {
                continue;
            }
            Vec3 velocity = item.getDeltaMovement().add(toPlayer.normalize().scale(PULL));
            if (velocity.length() > MAX_SPEED) {
                velocity = velocity.normalize().scale(MAX_SPEED);
            }
            item.setDeltaMovement(velocity);
            item.hasImpulse = true;
        }
    }

    /** True if the item is not reserved for somebody else. */
    private static boolean usableBy(ItemEntity item, UUID player) {
        // The reserved-for UUID ("target" in code, "Owner" in NBT) has no public getter in 1.20.1.
        CompoundTag tag = new CompoundTag();
        item.addAdditionalSaveData(tag);
        return !tag.hasUUID("Owner") || tag.getUUID("Owner").equals(player);
    }
}
