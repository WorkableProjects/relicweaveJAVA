package com.relicweave.vestige.entity.beast;

import com.relicweave.vestige.entity.Telegraph;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * A telegraphed straight-line charge: a wind-up with an aim line to where the dash will end, then a dash of fixed
 * length that stops at walls and hits the first living target it touches. State is serialisable.
 */
final class LineCharge {
    /** Result of one {@link #tick}. */
    enum Phase { IDLE, WINDING_UP, DASHING, FINISHED }

    private int windup;
    private boolean dashing;
    private boolean hit;
    private double remaining;
    private double speed = 0.8;
    private Vec3 dir = Vec3.ZERO;
    private Vec3 lastPos = Vec3.ZERO;
    private int stuck;

    boolean active() {
        return windup > 0 || dashing;
    }

    boolean windingUp() {
        return windup > 0;
    }

    /** Begin the wind-up toward {@code target}; the dash covers {@code distance} blocks at {@code dashSpeed}. */
    void start(Mob mob, LivingEntity target, int windupTicks, double distance, double dashSpeed,
               ParticleOptions telegraphParticle, SoundEvent sound) {
        this.dir = BeastUtil.flatDirection(mob.position(), target.position());
        if (dir == Vec3.ZERO) {
            dir = BeastUtil.facing(mob);
        }
        this.windup = windupTicks;
        this.remaining = distance;
        this.speed = dashSpeed;
        this.dashing = false;
        this.hit = false;
        float yaw = (float) (Math.toDegrees(Math.atan2(-dir.x, dir.z)));
        mob.setYRot(yaw);
        mob.yBodyRot = yaw;
        mob.yHeadRot = yaw;
        Telegraph.windUp(mob, telegraphParticle, sound, 1.0F);
    }

    void cancel() {
        windup = 0;
        dashing = false;
    }

    /** Advance one tick; {@code onHit} is called at most once with the first living entity the dash touches. */
    Phase tick(Mob mob, ParticleOptions telegraphParticle, Consumer<LivingEntity> onHit) {
        if (windup > 0) {
            mob.getNavigation().stop();
            mob.setDeltaMovement(0.0, mob.getDeltaMovement().y, 0.0);
            if (mob.level() instanceof ServerLevel level && windup % 3 == 0) {
                Vec3 from = mob.position().add(0, 0.2, 0);
                Telegraph.line(level, telegraphParticle, from, from.add(dir.scale(remaining)), (int) Math.max(4, remaining * 2));
            }
            windup--;
            if (windup == 0) {
                dashing = true;
                lastPos = mob.position();
                stuck = 0;
                return Phase.DASHING;
            }
            return Phase.WINDING_UP;
        }
        if (dashing) {
            mob.getNavigation().stop();
            Vec3 now = mob.position();
            double moved = Math.sqrt((now.x - lastPos.x) * (now.x - lastPos.x) + (now.z - lastPos.z) * (now.z - lastPos.z));
            lastPos = now;
            remaining -= moved;
            if (!hit) {
                for (LivingEntity victim : mob.level().getEntitiesOfClass(LivingEntity.class,
                        mob.getBoundingBox().inflate(0.4), e -> e != mob && e.isAlive() && e.getType() != mob.getType())) {
                    hit = true;
                    onHit.accept(victim);
                    break;
                }
            }
            stuck = moved < 0.02 ? stuck + 1 : 0;
            if (remaining <= 0.0 || mob.horizontalCollision || stuck >= 3) {
                dashing = false;
                return Phase.FINISHED;
            }
            mob.setDeltaMovement(dir.x * speed, mob.getDeltaMovement().y, dir.z * speed);
            mob.hasImpulse = true;
            return Phase.DASHING;
        }
        return Phase.IDLE;
    }

    void save(CompoundTag tag) {
        tag.putInt("ChargeWindup", windup);
        tag.putBoolean("ChargeDashing", dashing);
        tag.putDouble("ChargeRemaining", remaining);
        tag.putDouble("ChargeDirX", dir.x);
        tag.putDouble("ChargeDirZ", dir.z);
    }

    void load(CompoundTag tag) {
        windup = tag.getInt("ChargeWindup");
        dashing = tag.getBoolean("ChargeDashing");
        remaining = tag.getDouble("ChargeRemaining");
        dir = new Vec3(tag.getDouble("ChargeDirX"), 0.0, tag.getDouble("ChargeDirZ"));
        lastPos = Vec3.ZERO;
        if (dashing) {
            // Do not resume mid-dash after a reload: just stop.
            dashing = false;
        }
    }
}
