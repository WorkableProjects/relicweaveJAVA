package com.relicweave.vestige.boss.a;

import com.relicweave.vestige.boss.VestigeBoss;
import com.relicweave.vestige.entity.Telegraph;
import com.relicweave.vestige.hazard.field.HushNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Hushfen boss: a floating veiled figure ringed by voice masks. Moves: Sonic Lance, Silence Zones and (phase 2+) Tone
 * Shift. Every move shows its area with particles, has a pose and a subtitled sound, and the tone shift also states
 * the safe half in an action-bar message. Loud players draw its attention (Hushfield noise). The state machine runs in
 * {@link #tickBrain()} so tests can drive it.
 */
public class ChoirUnheard extends VestigeBoss {
    public static final int IDLE = 0;
    public static final int LANCE = 1;
    public static final int ZONES = 2;
    public static final int TONE = 3;

    public static final int LANCE_WINDUP = 20;
    public static final int ZONES_WINDUP = 40;
    public static final int TONE_WINDUP = 40;
    public static final int MIN_WINDUP = 15;
    public static final float LANCE_DAMAGE = 9.0F;
    public static final float ZONE_DAMAGE = 10.0F;
    public static final float TONE_DAMAGE = 8.0F;
    public static final double ZONE_RADIUS = 4.0;
    public static final int ZONE_COUNT = 3;
    public static final double TONE_RADIUS = 20.0;
    public static final double HOVER_HEIGHT = 2.5;

    private static final int[] BASE_COOLDOWNS = {0, 60, 160, 200};
    private static final int BASE_GAP = 40;

    private static final EntityDataAccessor<Integer> DATA_MOVE = SynchedEntityData.defineId(ChoirUnheard.class, EntityDataSerializers.INT);

    private int moveTimer;
    private int gap = BASE_GAP;
    private final int[] cooldowns = new int[4];
    private Vec3 lanceFrom = Vec3.ZERO;
    private Vec3 lanceTo = Vec3.ZERO;
    private final List<Vec3> zones = new ArrayList<>();
    private boolean toneHigh;
    private double toneAngle;
    private Vec3 toneOrigin = Vec3.ZERO;

    public ChoirUnheard(EntityType<? extends ChoirUnheard> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.xpReward = 280;
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 280.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FLYING_SPEED, 0.35)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected BossEvent.BossBarColor bossColor() {
        return BossEvent.BossBarColor.BLUE;
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
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_MOVE, IDLE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new HoverGoal());
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    /** The boss bar (tests and the renderer-independent UI). */
    public net.minecraft.server.level.ServerBossEvent bar() {
        return bossEvent;
    }

    // ---- State ----

    public int move() {
        return entityData.get(DATA_MOVE);
    }

    private void setMove(int move) {
        entityData.set(DATA_MOVE, move);
    }

    public int moveTimer() {
        return moveTimer;
    }

    public List<Vec3> zones() {
        return zones;
    }

    public boolean toneHigh() {
        return toneHigh;
    }

    public Vec3 toneOrigin() {
        return toneOrigin;
    }

    public Vec3 lanceFrom() {
        return lanceFrom;
    }

    public Vec3 lanceTo() {
        return lanceTo;
    }

    public int cooldown(int move) {
        return cooldowns[move];
    }

    public void setCooldown(int move, int ticks) {
        cooldowns[move] = ticks;
    }

    /** Unit normal of the tone dividing line: positive side is the high (NOTE) half. */
    public Vec3 toneNormal() {
        return new Vec3(-Math.sin(toneAngle), 0.0, Math.cos(toneAngle));
    }

    /** True if {@code pos} is on the half that is safe for the current tone. */
    public boolean isSafeForTone(Vec3 pos) {
        boolean highSide = pos.subtract(toneOrigin).dot(toneNormal()) >= 0.0;
        return highSide == toneHigh;
    }

    /** Wind-up never below 15 ticks. */
    private static int windup(int base) {
        return Math.max(MIN_WINDUP, base);
    }

    private int scaledCooldown(int base) {
        return phase() >= 3 ? (int) (base * 0.75) : base;
    }

    // ---- Phases ----

    /** Phase 1 above 60% health, phase 2 from 60% down to 25%, phase 3 below 25%. */
    public void updatePhase() {
        if (!isAlive()) {
            return;
        }
        float frac = getHealth() / getMaxHealth();
        setPhase(frac > 0.6F ? 1 : frac >= 0.25F ? 2 : 3);
    }

    // ---- Brain ----

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!isNoAi()) {
            tickBrain();
        }
    }

    /** The loudest player when someone is loud, else the current target. */
    @Nullable
    public LivingEntity aimTarget() {
        if (level() instanceof ServerLevel server) {
            ServerPlayer loud = HushNoise.loudestPlayerNear(server, position(), arenaRadius());
            if (loud != null && HushNoise.isLoud(loud) && loud.isAlive() && !loud.isSpectator()) {
                return loud;
            }
        }
        return getTarget();
    }

    /** One server tick of the move state machine. Public so GameTests can drive it. */
    public void tickBrain() {
        if (level().isClientSide || !isAlive()) {
            return;
        }
        for (int i = 0; i < cooldowns.length; i++) {
            if (cooldowns[i] > 0) {
                cooldowns[i]--;
            }
        }
        LivingEntity aim = aimTarget();
        if (aim != null) {
            updatePhase();
        }
        if (move() != IDLE) {
            advanceMove();
            return;
        }
        if (gap > 0) {
            gap--;
            return;
        }
        if (aim == null || !aim.isAlive() || distanceTo(aim) > 34.0) {
            return;
        }
        List<Integer> options = new ArrayList<>();
        if (cooldowns[LANCE] <= 0) {
            options.add(LANCE);
        }
        if (cooldowns[ZONES] <= 0) {
            options.add(ZONES);
        }
        if (phase() >= 2 && cooldowns[TONE] <= 0) {
            options.add(TONE);
        }
        if (!options.isEmpty()) {
            beginMove(options.get(random.nextInt(options.size())));
        }
    }

    /** Starts a move's wind-up. Returns false if it cannot start (no target, tone shift before phase 2). */
    public boolean beginMove(int move) {
        LivingEntity aim = aimTarget();
        switch (move) {
            case LANCE -> {
                if (aim == null) {
                    return false;
                }
                lanceFrom = position().add(0.0, getBbHeight() * 0.7, 0.0);
                Vec3 to = aim.position().add(0.0, aim.getBbHeight() * 0.5, 0.0);
                Vec3 dir = to.subtract(lanceFrom);
                double length = Math.max(6.0, dir.length()) + 3.0;
                dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : dir.normalize();
                lanceTo = lanceFrom.add(dir.scale(length));
                moveTimer = windup(LANCE_WINDUP);
                Telegraph.windUp(this, ParticleTypes.END_ROD, BossesA.CHOIR_LANCE.get(), 1.0F);
            }
            case ZONES -> {
                if (aim == null) {
                    return false;
                }
                zones.clear();
                zones.add(aim.position());
                Vec3 center = home() != null ? Vec3.atBottomCenterOf(home()) : position();
                for (int i = 1; i < ZONE_COUNT; i++) {
                    double angle = random.nextDouble() * Math.PI * 2.0;
                    double dist = 5.0 + random.nextDouble() * 7.0;
                    double x = center.x + Math.cos(angle) * dist;
                    double z = center.z + Math.sin(angle) * dist;
                    double y = BossACombat.groundY(level(), x, aim.getY(), z);
                    zones.add(new Vec3(x, Double.isNaN(y) ? aim.getY() : y, z));
                }
                moveTimer = windup(ZONES_WINDUP);
                Telegraph.windUp(this, ParticleTypes.SCULK_SOUL, BossesA.CHOIR_ZONES.get(), 0.8F);
            }
            case TONE -> {
                if (phase() < 2) {
                    return false;
                }
                toneAngle = random.nextDouble() * Math.PI;
                toneHigh = random.nextBoolean();
                toneOrigin = BossACombat.flat(position()).add(0.0, position().y, 0.0);
                moveTimer = windup(TONE_WINDUP);
                announce(Component.translatable(toneHigh ? "boss.relicweave.choir_unheard.tone_high" : "boss.relicweave.choir_unheard.tone_low"));
                Telegraph.windUp(this, toneHigh ? ParticleTypes.NOTE : ParticleTypes.SOUL,
                        toneHigh ? BossesA.CHOIR_TONE_HIGH.get() : BossesA.CHOIR_TONE_LOW.get(), toneHigh ? 1.4F : 0.6F);
            }
            default -> {
                return false;
            }
        }
        getNavigation().stop();
        setMove(move);
        return true;
    }

    private void advanceMove() {
        getNavigation().stop();
        int move = move();
        moveTimer--;
        if (moveTimer > 0) {
            if (level() instanceof ServerLevel server && moveTimer % 4 == 0) {
                telegraph(server, move);
            }
            return;
        }
        switch (move) {
            case LANCE -> fireLance();
            case ZONES -> detonateZones();
            case TONE -> toneStrike();
            default -> {}
        }
        setMove(IDLE);
        cooldowns[move] = scaledCooldown(BASE_COOLDOWNS[move]);
        gap = scaledCooldown(BASE_GAP);
    }

    private void telegraph(ServerLevel server, int move) {
        switch (move) {
            case LANCE -> Telegraph.line(server, ParticleTypes.END_ROD, lanceFrom, lanceTo, 24);
            case ZONES -> {
                for (Vec3 c : zones) {
                    Telegraph.ring(server, ParticleTypes.SCULK_SOUL, c, ZONE_RADIUS, 32);
                    Telegraph.ring(server, ParticleTypes.SCULK_SOUL, c, ZONE_RADIUS * 0.5, 16);
                }
            }
            case TONE -> {
                Vec3 normal = toneNormal();
                for (int i = 0; i < 40; i++) {
                    double along = (random.nextDouble() * 2.0 - 1.0) * TONE_RADIUS;
                    double across = (random.nextDouble() * 2.0 - 1.0) * TONE_RADIUS;
                    Vec3 axis = new Vec3(normal.z, 0.0, -normal.x);
                    Vec3 p = toneOrigin.add(axis.scale(along)).add(normal.scale(across));
                    double y = BossACombat.groundY(server, p.x, toneOrigin.y, p.z);
                    server.sendParticles(across >= 0.0 ? ParticleTypes.NOTE : ParticleTypes.SOUL,
                            p.x, (Double.isNaN(y) ? toneOrigin.y : y) + 0.2, p.z, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
            default -> {}
        }
    }

    // ---- Moves ----

    /** Damages players within 1 block of the lance line. Returns the players hit. */
    public List<Player> fireLance() {
        List<Player> hit = new ArrayList<>();
        AABB box = new AABB(lanceFrom, lanceTo).inflate(2.0);
        for (Player player : BossACombat.victims(level(), box)) {
            Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
            if (BossACombat.distToSegment(center, lanceFrom, lanceTo) <= 1.0) {
                player.hurt(damageSources().sonicBoom(this), scaledDamage(LANCE_DAMAGE));
                hit.add(player);
            }
        }
        if (level() instanceof ServerLevel server) {
            Telegraph.line(server, ParticleTypes.SONIC_BOOM, lanceFrom, lanceTo, 6);
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.5F, 1.4F);
        }
        return hit;
    }

    /** Each marked circle detonates: damage plus Darkness for players inside. Returns the players hit. */
    public List<Player> detonateZones() {
        Set<Player> hit = new HashSet<>();
        for (Vec3 c : zones) {
            AABB box = new AABB(c.x - ZONE_RADIUS, c.y - 3.0, c.z - ZONE_RADIUS, c.x + ZONE_RADIUS, c.y + 4.0, c.z + ZONE_RADIUS);
            for (Player player : BossACombat.victims(level(), box)) {
                double dx = player.getX() - c.x;
                double dz = player.getZ() - c.z;
                if (dx * dx + dz * dz <= ZONE_RADIUS * ZONE_RADIUS && hit.add(player)) {
                    player.hurt(damageSources().indirectMagic(this, this), scaledDamage(ZONE_DAMAGE));
                    player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
                }
            }
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SCULK_SOUL, c.x, c.y + 0.5, c.z, 24, ZONE_RADIUS * 0.5, 0.4, ZONE_RADIUS * 0.5, 0.05);
                server.playSound(null, c.x, c.y, c.z, BossesA.CHOIR_DETONATE.get(), SoundSource.HOSTILE, 1.2F, 1.0F);
            }
        }
        zones.clear();
        return new ArrayList<>(hit);
    }

    /** Players on the wrong half for the current tone take damage. Returns the players hit. */
    public List<Player> toneStrike() {
        List<Player> hit = new ArrayList<>();
        BlockPos center = home() != null ? home() : blockPosition();
        AABB box = new AABB(center).inflate(arenaRadius(), 16.0, arenaRadius());
        for (Player player : BossACombat.victims(level(), box)) {
            if (!isSafeForTone(player.position())) {
                player.hurt(damageSources().indirectMagic(this, this), scaledDamage(TONE_DAMAGE));
                hit.add(player);
            }
        }
        if (level() instanceof ServerLevel server) {
            server.playSound(null, getX(), getY(), getZ(), BossesA.CHOIR_DETONATE.get(), SoundSource.HOSTILE, 1.0F, toneHigh ? 1.5F : 0.6F);
        }
        return hit;
    }

    @Override
    protected void onArenaReset() {
        moveTimer = 0;
        gap = BASE_GAP;
        java.util.Arrays.fill(cooldowns, 0);
        zones.clear();
        setMove(IDLE);
    }

    // ---- Sounds ----

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SCULK_CLICKING;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_BLOCK_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WARDEN_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.8F;
    }

    // ---- NBT ----

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ChoirMove", move());
        tag.putInt("MoveTimer", moveTimer);
        tag.putInt("Gap", gap);
        tag.putIntArray("Cooldowns", cooldowns);
        BossACombat.putVec(tag, "LanceFrom", lanceFrom);
        BossACombat.putVec(tag, "LanceTo", lanceTo);
        ListTag list = new ListTag();
        for (Vec3 z : zones) {
            CompoundTag entry = new CompoundTag();
            BossACombat.putVec(entry, "Pos", z);
            list.add(entry);
        }
        tag.put("Zones", list);
        tag.putBoolean("ToneHigh", toneHigh);
        tag.putDouble("ToneAngle", toneAngle);
        BossACombat.putVec(tag, "ToneOrigin", toneOrigin);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setMove(tag.getInt("ChoirMove"));
        moveTimer = tag.getInt("MoveTimer");
        gap = tag.contains("Gap") ? tag.getInt("Gap") : BASE_GAP;
        int[] saved = tag.getIntArray("Cooldowns");
        for (int i = 0; i < cooldowns.length; i++) {
            cooldowns[i] = i < saved.length ? saved[i] : 0;
        }
        Vec3 from = BossACombat.getVec(tag, "LanceFrom");
        lanceFrom = from == null ? Vec3.ZERO : from;
        Vec3 to = BossACombat.getVec(tag, "LanceTo");
        lanceTo = to == null ? Vec3.ZERO : to;
        zones.clear();
        for (Tag t : tag.getList("Zones", Tag.TAG_COMPOUND)) {
            Vec3 v = BossACombat.getVec((CompoundTag) t, "Pos");
            if (v != null) {
                zones.add(v);
            }
        }
        toneHigh = tag.getBoolean("ToneHigh");
        toneAngle = tag.getDouble("ToneAngle");
        Vec3 origin = BossACombat.getVec(tag, "ToneOrigin");
        toneOrigin = origin == null ? Vec3.ZERO : origin;
    }

    /** Hovers 2-3 blocks above the floor, keeping 6-14 blocks from its target and inside the arena. */
    private final class HoverGoal extends Goal {
        HoverGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public void tick() {
            double floor = BossACombat.groundY(level(), getX(), getY(), getZ());
            double y = (Double.isNaN(floor) ? getY() : floor) + HOVER_HEIGHT;
            LivingEntity target = getTarget();
            Vec3 home = ChoirUnheard.this.home() != null ? Vec3.atBottomCenterOf(ChoirUnheard.this.home()) : position();
            if (move() != IDLE) {
                moveControl.setWantedPosition(getX(), y, getZ(), 0.5);
                return;
            }
            double homeDist = Math.sqrt(BossACombat.flat(position().subtract(home)).lengthSqr());
            if (homeDist > arenaRadius() - 4.0) {
                moveControl.setWantedPosition(home.x, y, home.z, 0.8);
            } else if (target != null) {
                double dist = distanceTo(target);
                if (dist > 14.0 || dist < 6.0) {
                    Vec3 away = BossACombat.flat(position().subtract(target.position()));
                    away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
                    Vec3 goal = target.position().add(away.scale(10.0));
                    moveControl.setWantedPosition(goal.x, y, goal.z, 0.8);
                } else {
                    moveControl.setWantedPosition(getX(), y, getZ(), 0.5);
                }
            } else {
                moveControl.setWantedPosition(home.x, y, home.z, 0.4);
            }
        }
    }
}
