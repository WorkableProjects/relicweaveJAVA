package com.relicweave.vestige.entity.ethereal;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.VestigeConfig;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.cycle.CyclePhase;
import com.relicweave.vestige.cycle.VestigeCycleEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Spawns Cycle Heralds when the Concordance Cycle enters Convergence. */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class HeraldEvents {
    public static final double SPACING = 64.0;
    private static final int ATTEMPTS = 12;

    private HeraldEvents() {}

    /** True if a living Herald is within {@link #SPACING} blocks of {@code pos}. */
    public static boolean heraldNear(ServerLevel level, Vec3 pos) {
        return !level.getEntitiesOfClass(CycleHerald.class, new AABB(pos, pos).inflate(SPACING),
                h -> h.isAlive() && h.position().distanceToSqr(pos) <= SPACING * SPACING).isEmpty();
    }

    /** Spawn rule: no other Herald within 64 blocks of the candidate position. */
    public static boolean canSpawnAt(ServerLevel level, Vec3 pos) {
        return !heraldNear(level, pos);
    }

    /** Tries to spawn an event-bound Herald 16 to 24 blocks from {@code player} in open air. */
    public static CycleHerald trySpawn(ServerLevel level, ServerPlayer player, RandomSource random) {
        for (int i = 0; i < ATTEMPTS; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = 16.0 + random.nextDouble() * 8.0;
            Vec3 pos = new Vec3(player.getX() + Math.cos(angle) * dist, player.getY() + 4.0 + random.nextDouble() * 6.0,
                    player.getZ() + Math.sin(angle) * dist);
            if (!canSpawnAt(level, pos)) {
                return null;
            }
            CycleHerald herald = EtherealMobs.CYCLE_HERALD.get().create(level);
            if (herald == null) {
                return null;
            }
            herald.moveTo(pos.x, pos.y, pos.z, random.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(herald, herald.getBoundingBox().inflate(0.5)) || level.containsAnyLiquid(herald.getBoundingBox())) {
                herald.discard();
                continue;
            }
            herald.setEventBound(true);
            level.addFreshEntity(herald);
            return herald;
        }
        return null;
    }

    @SubscribeEvent
    public static void onPhaseChanged(VestigeCycleEvent.PhaseChanged event) {
        ServerLevel level = event.getLevel();
        if (event.getTo() != CyclePhase.CONVERGENCE || level.dimension() != VestigeKeys.THE_VESTIGE) {
            return;
        }
        double chance = VestigeConfig.SPEC.isLoaded() ? VestigeConfig.HERALD_CHANCE.get() : 0.25;
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) {
                continue;
            }
            if (level.random.nextDouble() < chance) {
                trySpawn(level, player, level.random);
            }
        }
    }
}
