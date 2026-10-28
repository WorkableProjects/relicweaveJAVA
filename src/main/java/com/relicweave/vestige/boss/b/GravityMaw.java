package com.relicweave.vestige.boss.b;

import com.relicweave.vestige.boss.VestigeBoss;
import com.relicweave.vestige.entity.Telegraph;
import com.relicweave.vestige.hazard.FieldRegistry;
import com.relicweave.vestige.hazard.HazardProtection;
import com.relicweave.vestige.hazard.field.StarfallImpacts;
import com.relicweave.vestige.hazard.field.StarfallMarker;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * The Gravity Maw (Sundered Reach, Inverted Vault): a floating maw ringed by orbiting rock.
 * <ul>
 *   <li><b>Pull</b>: 30-tick inhale telegraph, then 60 ticks of pulling every arena player toward it (anchors within
 *   2 blocks hold a player in place). Afterwards the Maw is <b>gorged</b> for 50 ticks: takes x1.5 damage and cannot attack.</li>
 *   <li><b>Repulse</b>: 20-tick ring telegraph (radius 5), then shoves nearby players outward and grants Slow Falling.</li>
 *   <li><b>Bite</b>: melee after a 12-tick jaw-open pose.</li>
 *   <li><b>Debris</b> (phase 2): 15-tick rumble, then 4-6 telegraphed starfall impact zones near players.</li>
 * </ul>
 * The move state machine lives in {@link #tickMoves()}; {@code start*} methods begin a move. Everything is saved in NBT.
 */
public class GravityMaw extends VestigeBoss {
    public static final int STATE_IDLE = 0;
    public static final int STATE_PULL_WIND = 1;
    public static final int STATE_PULLING = 2;
    public static final int STATE_GORGED = 3;
    public static final int STATE_REPULSE_WIND = 4;
    public static final int STATE_BITE_WIND = 5;
    public static final int STATE_DEBRIS_WIND = 6;

    public static final int POSE_IDLE = 0;
    public static final int POSE_INHALE = 1;
    public static final int POSE_REPULSE = 2;
    public static final int POSE_JAW = 3;
    public static final int POSE_GORGED = 4;
    public static final int POSE_DEBRIS = 5;

    public static final int PULL_WINDUP = 30;
    public static final int PULL_TICKS = 60;
    public static final int GORGED_TICKS = 50;
    public static final int REPULSE_WINDUP = 20;
    public static final int BITE_WINDUP = 12;
    public static final int DEBRIS_WINDUP = 15;
    public static final double PULL_ACCEL = 0.12;
    public static final double ANCHOR_RADIUS = 2.0;
    public static final double REPULSE_RADIUS = 5.0;
    public static final double REPULSE_STRENGTH = 0.9;
    public static final double BITE_REACH = 4.0;
    public static final int DEBRIS_WARNING = 50;
    /** Hard cap on live debris markers in the arena. */
    public static final int MAX_DEBRIS_MARKERS = 12;

    private static final EntityDataAccessor<Integer> DATA_POSE = SynchedEntityData.defineId(GravityMaw.class, EntityDataSerializers.INT);

    private int moveState = STATE_IDLE;
    private int stateTicks;
    private int pullCooldown;
    private int repulseCooldown;
    private int biteCooldown;
    private int debrisCooldown;
    /** Tests only: when true the Maw never picks or advances moves by itself. */
    private boolean scripted;

    public GravityMaw(EntityType<? extends GravityMaw> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.xpReward = 300;
        this.pullCooldown = 100;
        this.repulseCooldown = 60;
        this.biteCooldown = 20;
        this.debrisCooldown = 120;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 340.0)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.ATTACK_DAMAGE, 14.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FLYING_SPEED, 0.5)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected BossEvent.BossBarColor bossColor() {
        return BossEvent.BossBarColor.PURPLE;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_POSE, POSE_IDLE);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    // ---- State accessors ----

    /** The boss bar shown to arena players (tests inspect its viewers). */
    public net.minecraft.server.level.ServerBossEvent bossBar() {
        return bossEvent;
    }

    public int getMawPose() {
        return this.entityData.get(DATA_POSE);
    }

    public int moveState() {
        return moveState;
    }

    public int stateTicks() {
        return stateTicks;
    }

    public void setMoveState(int state, int ticks) {
        this.moveState = state;
        this.stateTicks = ticks;
    }

    public boolean isGorged() {
        return moveState == STATE_GORGED;
    }

    public void setScripted(boolean scripted) {
        this.scripted = scripted;
    }

    public void setCooldowns(int pull, int repulse, int bite, int debris) {
        this.pullCooldown = pull;
        this.repulseCooldown = repulse;
        this.biteCooldown = bite;
        this.debrisCooldown = debris;
    }

    public int pullCooldown() {
        return pullCooldown;
    }

    // ---- Damage ----

    /** Gorged: x1.5 damage taken. Sources that bypass invulnerability (void, /kill) are never changed. */
    public float modifyIncomingDamage(DamageSource source, float amount) {
        if (isGorged() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return Math.min(amount * 1.5F, Float.MAX_VALUE);
        }
        return amount;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, modifyIncomingDamage(source, amount));
    }

    // ---- Phases and AI ----

    /** Phase 1 above 50% health, phase 2 at or below. */
    public void updatePhase() {
        setPhase(getHealth() <= getMaxHealth() * 0.5F ? 2 : 1);
    }

    private int scaledCooldown(int base) {
        return phase() >= 2 ? Math.round(base * 0.8F) : base;
    }

    @Override
    protected void onFightStart(int players) {
        setCooldowns(100, 60, 20, 120);
        setMoveState(STATE_IDLE, 0);
    }

    @Override
    protected void onArenaReset() {
        setMoveState(STATE_IDLE, 0);
        setCooldowns(100, 60, 20, 120);
        entityData.set(DATA_POSE, POSE_IDLE);
        if (level() instanceof ServerLevel server) {
            for (StarfallMarker marker : server.getEntitiesOfClass(StarfallMarker.class, getBoundingBox().inflate(arenaRadius() + 8.0))) {
                if (getUUID().equals(marker.ownerId())) {
                    marker.discard();
                }
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (scripted) {
            return;
        }
        if (isScaled()) {
            updatePhase();
        }
        tickMoves();
        if (moveState == STATE_IDLE && isScaled()) {
            chooseMove();
        }
    }

    private ServerPlayer nearestPlayer() {
        return arenaPlayers().stream().min(Comparator.comparingDouble(p -> p.distanceToSqr(this))).orElse(null);
    }

    private void chooseMove() {
        ServerPlayer target = nearestPlayer();
        if (target == null) {
            setTarget(null);
            return;
        }
        setTarget(target);
        double dist = distanceTo(target);
        if (repulseCooldown <= 0 && dist <= REPULSE_RADIUS - 0.5) {
            startRepulse();
        } else if (biteCooldown <= 0 && dist <= BITE_REACH - 0.5) {
            startBite();
        } else if (pullCooldown <= 0 && dist >= 4.0) {
            startPull();
        } else if (phase() >= 2 && debrisCooldown <= 0) {
            startDebris();
        } else if (dist > 2.5) {
            getMoveControl().setWantedPosition(target.getX(), target.getY() + 1.0, target.getZ(), 1.0);
        }
    }

    // ---- Move starts (each plays its telegraph cue immediately) ----

    public void startPull() {
        setMoveState(STATE_PULL_WIND, 0);
        pullCooldown = scaledCooldown(300);
        Telegraph.windUp(this, ParticleTypes.REVERSE_PORTAL, BossesB.MAW_INHALE.get(), 0.8F);
    }

    public void startRepulse() {
        setMoveState(STATE_REPULSE_WIND, 0);
        repulseCooldown = scaledCooldown(160);
        Telegraph.windUp(this, ParticleTypes.PORTAL, BossesB.MAW_REPULSE.get(), 1.0F);
    }

    public void startBite() {
        setMoveState(STATE_BITE_WIND, 0);
        biteCooldown = scaledCooldown(50);
        Telegraph.windUp(this, ParticleTypes.CRIT, BossesB.MAW_BITE.get(), 1.0F);
    }

    public void startDebris() {
        setMoveState(STATE_DEBRIS_WIND, 0);
        debrisCooldown = scaledCooldown(260);
        Telegraph.windUp(this, ParticleTypes.ASH, BossesB.MAW_DEBRIS.get(), 0.9F);
    }

    // ---- Move state machine ----

    /** Advances cooldowns and the current move by one tick. Server side only. */
    public void tickMoves() {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        if (pullCooldown > 0) {
            pullCooldown--;
        }
        if (repulseCooldown > 0) {
            repulseCooldown--;
        }
        if (biteCooldown > 0) {
            biteCooldown--;
        }
        if (debrisCooldown > 0) {
            debrisCooldown--;
        }
        Vec3 center = position().add(0.0, getBbHeight() / 2.0, 0.0);
        switch (moveState) {
            case STATE_PULL_WIND -> {
                stopMoving();
                spiral(server, center, stateTicks);
                if (++stateTicks >= PULL_WINDUP) {
                    setMoveState(STATE_PULLING, 0);
                }
            }
            case STATE_PULLING -> {
                stopMoving();
                applyPull(server, center);
                if (++stateTicks >= PULL_TICKS) {
                    setMoveState(STATE_GORGED, 0);
                    announceGorged();
                }
            }
            case STATE_GORGED -> {
                stopMoving();
                if (tickCount % 4 == 0) {
                    server.sendParticles(ParticleTypes.SMOKE, getX(), center.y + 0.8, getZ(), 4, 0.6, 0.3, 0.6, 0.01);
                }
                if (++stateTicks >= GORGED_TICKS) {
                    setMoveState(STATE_IDLE, 0);
                }
            }
            case STATE_REPULSE_WIND -> {
                stopMoving();
                if (stateTicks % 5 == 0) {
                    Telegraph.ring(server, ParticleTypes.PORTAL, position(), REPULSE_RADIUS, 32);
                }
                if (++stateTicks >= REPULSE_WINDUP) {
                    repulse(server);
                    setMoveState(STATE_IDLE, 0);
                }
            }
            case STATE_BITE_WIND -> {
                stopMoving();
                if (++stateTicks >= BITE_WINDUP) {
                    bite();
                    setMoveState(STATE_IDLE, 0);
                }
            }
            case STATE_DEBRIS_WIND -> {
                stopMoving();
                if (stateTicks % 3 == 0) {
                    server.sendParticles(ParticleTypes.ASH, getX(), center.y + 1.0, getZ(), 10, 2.0, 0.8, 2.0, 0.02);
                }
                if (++stateTicks >= DEBRIS_WINDUP) {
                    markDebris(server);
                    setMoveState(STATE_IDLE, 0);
                }
            }
            default -> { }
        }
        int pose = switch (moveState) {
            case STATE_PULL_WIND, STATE_PULLING -> POSE_INHALE;
            case STATE_GORGED -> POSE_GORGED;
            case STATE_REPULSE_WIND -> POSE_REPULSE;
            case STATE_BITE_WIND -> POSE_JAW;
            case STATE_DEBRIS_WIND -> POSE_DEBRIS;
            default -> POSE_IDLE;
        };
        if (pose != getMawPose()) {
            this.entityData.set(DATA_POSE, pose);
        }
    }

    private void stopMoving() {
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
    }

    private void announceGorged() {
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EAT, net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 0.5F);
    }

    /** Converging spiral of reverse-portal particles: shows where the pull comes from. */
    private void spiral(ServerLevel server, Vec3 center, int t) {
        double radius = 7.0 * (1.0 - t / (double) PULL_WINDUP) + 1.0;
        for (int k = 0; k < 3; k++) {
            double angle = t * 0.45 + k * Math.PI * 2.0 / 3.0;
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x + Math.cos(angle) * radius,
                    center.y + Math.sin(angle * 2.0) * 0.6, center.z + Math.sin(angle) * radius, 1, 0, 0, 0, 0.0);
        }
    }

    /** True if a gravity anchor lies within {@link #ANCHOR_RADIUS} blocks of the player. */
    public static boolean isAnchoredNear(ServerPlayer player) {
        return player.serverLevel().getPoiManager().findClosest(holder -> holder.value() == FieldRegistry.GRAVITY_ANCHOR_POI.get(),
                player.blockPosition(), (int) Math.ceil(ANCHOR_RADIUS), PoiManager.Occupancy.ANY)
                .filter(pos -> pos.distSqr(player.blockPosition()) <= ANCHOR_RADIUS * ANCHOR_RADIUS + 1.0).isPresent();
    }

    private void applyPull(ServerLevel server, Vec3 center) {
        double strength = PULL_ACCEL * arenaHazardStrength();
        for (ServerPlayer player : arenaPlayers()) {
            if (isAnchoredNear(player)) {
                continue;
            }
            Vec3 toward = center.subtract(player.position().add(0.0, player.getBbHeight() / 2.0, 0.0));
            double dist = toward.length();
            if (dist < 1.5) {
                continue;
            }
            double accel = strength * (1.0 - HazardProtection.reduction(player, HazardProtection.Hazard.GRAVITY_SHEAR));
            player.setDeltaMovement(player.getDeltaMovement().add(toward.scale(accel / dist)));
            player.hurtMarked = true;
            player.fallDistance = 0.0F;
        }
        if (stateTicks % 2 == 0) {
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 8, 2.5, 1.5, 2.5, 0.3);
        }
    }

    private void repulse(ServerLevel server) {
        server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + getBbHeight() / 2.0, getZ(), 3, 1.0, 0.5, 1.0, 0.0);
        Telegraph.ring(server, ParticleTypes.END_ROD, position(), REPULSE_RADIUS, 32);
        for (ServerPlayer player : arenaPlayers()) {
            if (player.distanceToSqr(this) > REPULSE_RADIUS * REPULSE_RADIUS) {
                continue;
            }
            Vec3 away = player.position().subtract(position());
            Vec3 flat = new Vec3(away.x, 0.0, away.z);
            flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : flat.normalize();
            player.hurt(damageSources().mobAttack(this), scaledDamage(8.0F));
            player.setDeltaMovement(flat.x * REPULSE_STRENGTH, 0.35, flat.z * REPULSE_STRENGTH);
            player.hurtMarked = true;
            player.fallDistance = 0.0F;
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false, true));
        }
    }

    private void bite() {
        for (ServerPlayer player : arenaPlayers()) {
            if (player.distanceToSqr(this) <= BITE_REACH * BITE_REACH) {
                swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                doHurtTarget(player);
            }
        }
    }

    private void markDebris(ServerLevel server) {
        List<ServerPlayer> players = arenaPlayers();
        if (players.isEmpty()) {
            return;
        }
        int existing = server.getEntitiesOfClass(StarfallMarker.class, getBoundingBox().inflate(arenaRadius() + 8.0)).size();
        int count = 4 + random.nextInt(3);
        for (int i = 0; i < count && existing < MAX_DEBRIS_MARKERS; i++, existing++) {
            ServerPlayer player = players.get(i % players.size());
            Vec3 pos = i < players.size() ? player.position()
                    : player.position().add((random.nextDouble() - 0.5) * 6.0, 0.0, (random.nextDouble() - 0.5) * 6.0);
            StarfallImpacts.spawnMarker(server, pos, DEBRIS_WARNING, scaledDamage(9.0F), this);
        }
    }

    // ---- Sounds ----

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WARDEN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WARDEN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WARDEN_DEATH;
    }

    // ---- Save data ----

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("MoveState", moveState);
        tag.putInt("StateTicks", stateTicks);
        tag.putInt("PullCooldown", pullCooldown);
        tag.putInt("RepulseCooldown", repulseCooldown);
        tag.putInt("BiteCooldown", biteCooldown);
        tag.putInt("DebrisCooldown", debrisCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        moveState = tag.getInt("MoveState");
        stateTicks = tag.getInt("StateTicks");
        pullCooldown = tag.getInt("PullCooldown");
        repulseCooldown = tag.getInt("RepulseCooldown");
        biteCooldown = tag.getInt("BiteCooldown");
        debrisCooldown = tag.getInt("DebrisCooldown");
    }
}
