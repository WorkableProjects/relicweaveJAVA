package com.relicweave.vestige.boss.b;

import com.relicweave.vestige.boss.VestigeBoss;
import com.relicweave.vestige.entity.Telegraph;
import com.relicweave.vestige.hazard.field.StarfallImpacts;
import com.relicweave.vestige.hazard.field.StarfallMarker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The Starforged Colossus (Starfall Caldera, Foundry of Falling Stars): a huge forge golem.
 * <ul>
 *   <li><b>Armored</b>: takes only 25% damage except while vulnerable (x1.5).</li>
 *   <li><b>Vents</b>: on fight start it records the levers near its home. Roughly every 20 seconds it overheats; flipping a
 *   recorded lever ON during the overheat triggers a Vent Burst that makes it vulnerable and switches every lever off.
 *   With no levers recorded an overheat makes it vulnerable by itself, so it can never be unkillable.</li>
 *   <li><b>Slam</b>: 30-tick ring telegraph, then heavy damage to grounded players within 5 blocks.</li>
 *   <li><b>Heat Pulse</b>: 40-tick glow, then fire damage to every player with line of sight; cover blocks it.</li>
 *   <li><b>Starfall Barrage</b> (phase 2): a marker under every player.</li>
 * </ul>
 */
public class StarforgedColossus extends VestigeBoss {
    public static final int STATE_IDLE = 0;
    public static final int STATE_SLAM_WIND = 1;
    public static final int STATE_PULSE_WIND = 2;
    public static final int STATE_BARRAGE_WIND = 3;

    public static final int POSE_IDLE = 0;
    public static final int POSE_SLAM = 1;
    public static final int POSE_PULSE = 2;
    public static final int POSE_BARRAGE = 3;

    public static final int HEAT_NONE = 0;
    public static final int HEAT_OVERHEAT = 1;
    public static final int HEAT_VULNERABLE = 2;

    public static final int SLAM_WINDUP = 30;
    public static final double SLAM_RADIUS = 5.0;
    public static final int PULSE_WINDUP = 40;
    public static final int BARRAGE_WINDUP = 15;
    public static final int BARRAGE_WARNING = 60;
    public static final int MAX_BARRAGE_MARKERS = 12;
    public static final int OVERHEAT_WARNING = 40;
    public static final int OVERHEAT_TICKS = 160;
    public static final int VULNERABLE_TICKS = 200;
    public static final int LEVER_RADIUS = 24;
    public static final int MAX_LEVERS = 16;
    public static final float ARMORED_FACTOR = 0.25F;
    public static final float VULNERABLE_FACTOR = 1.5F;

    private static final EntityDataAccessor<Integer> DATA_POSE = SynchedEntityData.defineId(StarforgedColossus.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HEAT = SynchedEntityData.defineId(StarforgedColossus.class, EntityDataSerializers.INT);

    private int moveState = STATE_IDLE;
    private int stateTicks;
    private int slamCooldown = 60;
    private int pulseCooldown = 120;
    private int barrageCooldown = 200;
    private int ventCooldown = 300;
    private int overheatWarn;
    private int overheatTicks;
    private int vulnerableTicks;
    private boolean armorHintShown;
    private final List<BlockPos> levers = new ArrayList<>();
    /** Tests only: when true the Colossus never picks or advances moves or vents by itself. */
    private boolean scripted;

    public StarforgedColossus(EntityType<? extends StarforgedColossus> type, Level level) {
        super(type, level);
        this.xpReward = 350;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 420.0)
                .add(Attributes.ARMOR, 16.0)
                .add(Attributes.ATTACK_DAMAGE, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.18)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected BossEvent.BossBarColor bossColor() {
        return BossEvent.BossBarColor.RED;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_POSE, POSE_IDLE);
        this.entityData.define(DATA_HEAT, HEAT_NONE);
    }

    // ---- State accessors ----

    /** The boss bar shown to arena players (tests inspect its viewers). */
    public net.minecraft.server.level.ServerBossEvent bossBar() {
        return bossEvent;
    }

    public int getColossusPose() {
        return this.entityData.get(DATA_POSE);
    }

    public int getHeat() {
        return this.entityData.get(DATA_HEAT);
    }

    public int moveState() {
        return moveState;
    }

    public void setMoveState(int state, int ticks) {
        this.moveState = state;
        this.stateTicks = ticks;
    }

    public boolean isVulnerable() {
        return vulnerableTicks > 0;
    }

    public int vulnerableTicks() {
        return vulnerableTicks;
    }

    public void setVulnerableTicks(int ticks) {
        this.vulnerableTicks = ticks;
    }

    public boolean isOverheating() {
        return overheatTicks > 0;
    }

    public int overheatTicks() {
        return overheatTicks;
    }

    public int overheatWarn() {
        return overheatWarn;
    }

    public List<BlockPos> levers() {
        return List.copyOf(levers);
    }

    public void setScripted(boolean scripted) {
        this.scripted = scripted;
    }

    public void setVentCooldown(int ticks) {
        this.ventCooldown = ticks;
    }

    public boolean armorHintShown() {
        return armorHintShown;
    }

    // ---- Damage ----

    /** Armored: x0.25; vulnerable: x1.5. Sources that bypass invulnerability (void, /kill) are never changed. */
    public float modifyIncomingDamage(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return amount;
        }
        return isVulnerable() ? Math.min(amount * VULNERABLE_FACTOR, Float.MAX_VALUE) : amount * ARMORED_FACTOR;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean armored = !isVulnerable() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        boolean result = super.hurt(source, modifyIncomingDamage(source, amount));
        if (armored && level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + getBbHeight() * 0.6, getZ(), 8, 0.6, 0.6, 0.6, 0.2);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_PLACE, SoundSource.HOSTILE, 0.5F, 1.6F);
            if (!armorHintShown) {
                armorHintShown = true;
                announce(Component.translatable("message.relicweave.colossus.armored"));
            }
        }
        return result;
    }

    // ---- Phases and AI ----

    /** Phase 1 above 50% health, phase 2 at or below. */
    public void updatePhase() {
        setPhase(getHealth() <= getMaxHealth() * 0.5F ? 2 : 1);
    }

    private int ventInterval() {
        return phase() >= 2 ? 320 : 400;
    }

    @Override
    protected void onFightStart(int players) {
        armorHintShown = false;
        recordLevers();
        ventCooldown = 300;
    }

    @Override
    protected void onArenaReset() {
        setMoveState(STATE_IDLE, 0);
        slamCooldown = 60;
        pulseCooldown = 120;
        barrageCooldown = 200;
        ventCooldown = 300;
        overheatWarn = 0;
        overheatTicks = 0;
        vulnerableTicks = 0;
        armorHintShown = false;
        resetLevers(false);
        levers.clear();
        syncVisuals();
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
        if (scripted || !isScaled()) {
            return;
        }
        updatePhase();
        tickVents();
        tickMoves();
        if (moveState == STATE_IDLE) {
            chooseMove();
        }
    }

    private void chooseMove() {
        ServerPlayer target = arenaPlayers().stream().min(Comparator.comparingDouble(p -> p.distanceToSqr(this))).orElse(null);
        if (target == null) {
            setTarget(null);
            return;
        }
        setTarget(target);
        double dist = distanceTo(target);
        if (slamCooldown <= 0 && dist <= SLAM_RADIUS - 0.5) {
            startSlam();
        } else if (pulseCooldown <= 0 && dist >= 5.0 && dist <= 28.0) {
            startPulse();
        } else if (phase() >= 2 && barrageCooldown <= 0) {
            startBarrage();
        } else if (dist > 3.5) {
            getNavigation().moveTo(target, 1.0);
        }
    }

    // ---- Move starts ----

    public void startSlam() {
        setMoveState(STATE_SLAM_WIND, 0);
        slamCooldown = 100;
        Telegraph.windUp(this, ParticleTypes.CRIT, BossesB.COLOSSUS_SLAM.get(), 0.7F);
    }

    public void startPulse() {
        setMoveState(STATE_PULSE_WIND, 0);
        pulseCooldown = 240;
        Telegraph.windUp(this, ParticleTypes.FLAME, BossesB.COLOSSUS_HEAT_PULSE.get(), 0.8F);
    }

    public void startBarrage() {
        setMoveState(STATE_BARRAGE_WIND, 0);
        barrageCooldown = 300;
        Telegraph.windUp(this, ParticleTypes.END_ROD, BossesB.COLOSSUS_BARRAGE.get(), 0.8F);
    }

    // ---- Move state machine ----

    /** Advances cooldowns and the current move by one tick. Server side only. */
    public void tickMoves() {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        if (slamCooldown > 0) {
            slamCooldown--;
        }
        if (pulseCooldown > 0) {
            pulseCooldown--;
        }
        if (barrageCooldown > 0) {
            barrageCooldown--;
        }
        switch (moveState) {
            case STATE_SLAM_WIND -> {
                halt();
                if (stateTicks % 10 == 0) {
                    Telegraph.ring(server, ParticleTypes.CRIT, position(), SLAM_RADIUS, 32);
                }
                if (++stateTicks >= SLAM_WINDUP) {
                    slam(server);
                    setMoveState(STATE_IDLE, 0);
                }
            }
            case STATE_PULSE_WIND -> {
                halt();
                server.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.5 + (stateTicks % 20) * 0.2, getZ(), 6, 0.9, 0.2, 0.9, 0.03);
                if (++stateTicks >= PULSE_WINDUP) {
                    heatPulse(server);
                    setMoveState(STATE_IDLE, 0);
                }
            }
            case STATE_BARRAGE_WIND -> {
                halt();
                server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + getBbHeight() + 0.5, getZ(), 4, 1.0, 0.5, 1.0, 0.05);
                if (++stateTicks >= BARRAGE_WINDUP) {
                    barrage(server);
                    setMoveState(STATE_IDLE, 0);
                }
            }
            default -> { }
        }
        syncVisuals();
    }

    private void halt() {
        getNavigation().stop();
        setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
    }

    private void syncVisuals() {
        int pose = switch (moveState) {
            case STATE_SLAM_WIND -> POSE_SLAM;
            case STATE_PULSE_WIND -> POSE_PULSE;
            case STATE_BARRAGE_WIND -> POSE_BARRAGE;
            default -> POSE_IDLE;
        };
        int heat = vulnerableTicks > 0 ? HEAT_VULNERABLE : (overheatWarn > 0 || overheatTicks > 0) ? HEAT_OVERHEAT : HEAT_NONE;
        if (pose != getColossusPose()) {
            this.entityData.set(DATA_POSE, pose);
        }
        if (heat != getHeat()) {
            this.entityData.set(DATA_HEAT, heat);
        }
    }

    private void slam(ServerLevel server) {
        server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 3, 1.5, 0.1, 1.5, 0.0);
        Telegraph.ring(server, ParticleTypes.FLAME, position(), SLAM_RADIUS, 32);
        for (ServerPlayer player : arenaPlayers()) {
            double dx = player.getX() - getX();
            double dz = player.getZ() - getZ();
            if (dx * dx + dz * dz > SLAM_RADIUS * SLAM_RADIUS || player.getY() - getY() > 1.0) {
                continue;   // outside the ring, or jumped over the shockwave
            }
            player.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
            double len = Math.max(0.1, Math.sqrt(dx * dx + dz * dz));
            player.setDeltaMovement(dx / len * 0.8, 0.45, dz / len * 0.8);
            player.hurtMarked = true;
        }
    }

    /** True if blocks stand between the Colossus's eyes and the player's eyes (cover). */
    public boolean isCovered(ServerPlayer player) {
        Vec3 from = getEyePosition();
        Vec3 to = player.getEyePosition();
        HitResult hit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() != HitResult.Type.MISS;
    }

    private void heatPulse(ServerLevel server) {
        server.sendParticles(ParticleTypes.LAVA, getX(), getY() + getBbHeight() * 0.7, getZ(), 12, 1.0, 0.5, 1.0, 0.0);
        for (ServerPlayer player : arenaPlayers()) {
            if (isCovered(player)) {
                continue;
            }
            player.hurt(damageSources().inFire(), scaledDamage(6.0F));
            player.setSecondsOnFire(3);
        }
    }

    private void barrage(ServerLevel server) {
        int existing = server.getEntitiesOfClass(StarfallMarker.class, getBoundingBox().inflate(arenaRadius() + 8.0)).size();
        for (ServerPlayer player : arenaPlayers()) {
            if (existing >= MAX_BARRAGE_MARKERS) {
                break;
            }
            StarfallImpacts.spawnMarker(server, player.position(), BARRAGE_WARNING, scaledDamage(10.0F), this);
            existing++;
        }
    }

    // ---- Vents ----

    /** Records every lever within {@link #LEVER_RADIUS} blocks of home (called at fight start; saved). */
    public void recordLevers() {
        levers.clear();
        BlockPos center = home() != null ? home() : blockPosition();
        BlockPos.betweenClosedStream(center.offset(-LEVER_RADIUS, -LEVER_RADIUS, -LEVER_RADIUS),
                center.offset(LEVER_RADIUS, LEVER_RADIUS, LEVER_RADIUS)).forEach(pos -> {
            if (levers.size() < MAX_LEVERS && level().getBlockState(pos).is(Blocks.LEVER)) {
                levers.add(pos.immutable());
            }
        });
    }

    /** Begins an overheat: 40-tick glowing telegraph, then {@link #OVERHEAT_TICKS} of open vents. */
    public void startOverheat() {
        overheatWarn = OVERHEAT_WARNING;
        ventCooldown = ventInterval();
        Telegraph.windUp(this, ParticleTypes.FLAME, BossesB.COLOSSUS_OVERHEAT.get(), 0.9F);
        syncVisuals();
    }

    /** Advances the overheat cycle and vulnerability timer by one tick. Server side only. */
    public void tickVents() {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        if (vulnerableTicks > 0) {
            vulnerableTicks--;
            if (tickCount % 5 == 0) {
                server.sendParticles(ParticleTypes.LAVA, getX(), getY() + getBbHeight(), getZ(), 3, 0.7, 0.2, 0.7, 0.0);
            }
        }
        if (overheatWarn > 0) {
            server.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1.0 + random.nextDouble() * 3.0, getZ(), 4, 0.9, 0.3, 0.9, 0.03);
            if (--overheatWarn == 0) {
                beginOverheat();
            }
        } else if (overheatTicks > 0) {
            if (tickCount % 4 == 0) {
                server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + getBbHeight(), getZ(), 3, 0.8, 0.2, 0.8, 0.02);
            }
            if (leverFlipped()) {
                ventBurst();
            } else {
                overheatTicks--;
            }
        } else if (vulnerableTicks <= 0) {
            if (ventCooldown > 0) {
                ventCooldown--;
            } else {
                startOverheat();
            }
        }
        syncVisuals();
    }

    private void beginOverheat() {
        overheatTicks = OVERHEAT_TICKS;
        resetLevers(false);
        if (levers.isEmpty()) {
            makeVulnerable();   // spawned outside its arena: never unkillable
        }
    }

    private boolean leverFlipped() {
        for (BlockPos pos : levers) {
            BlockState state = level().getBlockState(pos);
            if (state.is(Blocks.LEVER) && state.getValue(LeverBlock.POWERED)) {
                return true;
            }
        }
        return false;
    }

    /** Vent Burst: every recorded lever switches off and the Colossus becomes vulnerable. */
    public void ventBurst() {
        resetLevers(true);
        makeVulnerable();
    }

    private void makeVulnerable() {
        overheatTicks = 0;
        vulnerableTicks = VULNERABLE_TICKS;
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.5F, 0.6F);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + getBbHeight() * 0.6, getZ(), 4, 1.0, 1.0, 1.0, 0.0);
        }
        announce(Component.translatable("message.relicweave.colossus.vulnerable"));
        syncVisuals();
    }

    private void resetLevers(boolean sound) {
        for (BlockPos pos : levers) {
            BlockState state = level().getBlockState(pos);
            if (!state.is(Blocks.LEVER) || !state.getValue(LeverBlock.POWERED)) {
                continue;
            }
            level().setBlock(pos, state.setValue(LeverBlock.POWERED, false), 3);
            level().updateNeighborsAt(pos, Blocks.LEVER);
            Direction connected = switch (state.getValue(BlockStateProperties.ATTACH_FACE)) {
                case CEILING -> Direction.DOWN;
                case FLOOR -> Direction.UP;
                default -> state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            };
            level().updateNeighborsAt(pos.relative(connected.getOpposite()), Blocks.LEVER);
            if (sound) {
                level().playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.8F, 0.5F);
            }
        }
    }

    // ---- Sounds ----

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.IRON_GOLEM_STEP;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_DAMAGE;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    // ---- Save data ----

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("MoveState", moveState);
        tag.putInt("StateTicks", stateTicks);
        tag.putInt("SlamCooldown", slamCooldown);
        tag.putInt("PulseCooldown", pulseCooldown);
        tag.putInt("BarrageCooldown", barrageCooldown);
        tag.putInt("VentCooldown", ventCooldown);
        tag.putInt("OverheatWarn", overheatWarn);
        tag.putInt("OverheatTicks", overheatTicks);
        tag.putInt("VulnerableTicks", vulnerableTicks);
        tag.putBoolean("ArmorHintShown", armorHintShown);
        long[] packed = new long[levers.size()];
        for (int i = 0; i < packed.length; i++) {
            packed[i] = levers.get(i).asLong();
        }
        tag.putLongArray("Levers", packed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        moveState = tag.getInt("MoveState");
        stateTicks = tag.getInt("StateTicks");
        slamCooldown = tag.getInt("SlamCooldown");
        pulseCooldown = tag.getInt("PulseCooldown");
        barrageCooldown = tag.getInt("BarrageCooldown");
        ventCooldown = tag.getInt("VentCooldown");
        overheatWarn = tag.getInt("OverheatWarn");
        overheatTicks = tag.getInt("OverheatTicks");
        vulnerableTicks = tag.getInt("VulnerableTicks");
        armorHintShown = tag.getBoolean("ArmorHintShown");
        levers.clear();
        for (long packed : tag.getLongArray("Levers")) {
            levers.add(BlockPos.of(packed));
        }
    }
}
