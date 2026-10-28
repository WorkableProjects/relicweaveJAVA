package com.relicweave.vestige.entity.ethereal;

import com.relicweave.vestige.cycle.ConcordanceCycle;
import com.relicweave.vestige.cycle.CyclePhase;
import com.relicweave.vestige.entity.Telegraph;
import com.relicweave.vestige.hazard.FieldRegistry;
import com.relicweave.vestige.hazard.RiftRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Harmless ambient flier. In Resonance and Convergence it flies to the nearest rift fissure, null ward or gravity
 * anchor within 48 blocks and hovers there, leading observant players to phenomena; in Quiet it just wanders.
 */
public class EclipseMoth extends PathfinderMob {
    public static final int SEARCH_RADIUS = 48;
    private static final int SEARCH_INTERVAL = 40;

    @Nullable
    private BlockPos poi;

    public EclipseMoth(EntityType<? extends EclipseMoth> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SeekPoiGoal(this));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomFlyingGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return EclipseMoth.this.poi == null && super.canUse();
            }
        });
    }

    /**
     * Chooses the POI to visit. Quiet: none. Resonance: the nearest fissure or ward in range, else the nearest anchor.
     * Convergence: the nearest of all three. Only candidates within {@value #SEARCH_RADIUS} blocks count.
     */
    public static Optional<BlockPos> selectTarget(CyclePhase phase, BlockPos from, Collection<BlockPos> fissuresAndWards, Collection<BlockPos> anchors) {
        if (phase == CyclePhase.QUIET) {
            return Optional.empty();
        }
        if (phase == CyclePhase.RESONANCE) {
            Optional<BlockPos> primary = nearest(from, fissuresAndWards);
            return primary.isPresent() ? primary : nearest(from, anchors);
        }
        List<BlockPos> all = new ArrayList<>(fissuresAndWards);
        all.addAll(anchors);
        return nearest(from, all);
    }

    private static Optional<BlockPos> nearest(BlockPos from, Collection<BlockPos> candidates) {
        BlockPos best = null;
        double bestDist = SEARCH_RADIUS * (double) SEARCH_RADIUS;
        for (BlockPos p : candidates) {
            double d = p.distSqr(from);
            if (d <= bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return Optional.ofNullable(best);
    }

    @Nullable
    public BlockPos poi() {
        return poi;
    }

    private void refreshPoi() {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        CyclePhase phase = ConcordanceCycle.phase(server);
        if (phase == CyclePhase.QUIET) {
            poi = null;
            return;
        }
        PoiManager manager = server.getPoiManager();
        List<BlockPos> fissuresAndWards = manager.getInSquare(
                h -> h.value() == RiftRegistry.RIFT_FISSURE_POI.get() || h.value() == FieldRegistry.NULL_WARD_POI.get(),
                blockPosition(), SEARCH_RADIUS, PoiManager.Occupancy.ANY).map(PoiRecord::getPos).toList();
        List<BlockPos> anchors = manager.getInSquare(h -> h.value() == FieldRegistry.GRAVITY_ANCHOR_POI.get(),
                blockPosition(), SEARCH_RADIUS, PoiManager.Occupancy.ANY).map(PoiRecord::getPos).toList();
        poi = selectTarget(phase, blockPosition(), fissuresAndWards, anchors).orElse(null);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % SEARCH_INTERVAL == 0) {
            refreshPoi();
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && random.nextInt(4) == 0) {
            int count = Telegraph.clientParticleCount(2);
            for (int i = 0; i < count; i++) {
                level().addParticle(ParticleTypes.GLOW, getRandomX(0.6), getY(0.6), getRandomZ(0.6), 0, 0.01, 0);
            }
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return net.minecraft.sounds.SoundEvents.BAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return net.minecraft.sounds.SoundEvents.BAT_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.6F;
    }

    /** Flies to the chosen POI and hovers around it. */
    private static final class SeekPoiGoal extends Goal {
        private final EclipseMoth moth;

        SeekPoiGoal(EclipseMoth moth) {
            this.moth = moth;
            setFlags(java.util.EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return moth.poi != null;
        }

        @Override
        public boolean canContinueToUse() {
            return moth.poi != null;
        }

        @Override
        public void tick() {
            BlockPos p = moth.poi;
            if (p == null) {
                return;
            }
            double t = moth.tickCount * 0.05;
            moth.getMoveControl().setWantedPosition(p.getX() + 0.5 + Math.cos(t) * 2.0, p.getY() + 2.0 + Math.sin(t * 1.7) * 0.7,
                    p.getZ() + 0.5 + Math.sin(t) * 2.0, 0.7);
        }
    }
}
