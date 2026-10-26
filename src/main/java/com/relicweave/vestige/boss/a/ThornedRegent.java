package com.relicweave.vestige.boss.a;

import com.relicweave.vestige.boss.VestigeBoss;
import com.relicweave.vestige.entity.Telegraph;
import com.relicweave.vestige.entity.beast.BeastMobs;
import com.relicweave.vestige.entity.beast.GlassrootStalker;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Glassroot Wilds boss. Moves (each has a wind-up telegraph of at least 20 ticks, shown by a synced pose, particles and
 * a subtitled sound): Thorn Volley, Root Wall, Root Strike (which exposes its core: x2 damage, no attacks) and, in
 * phase 2, Stalker Call. The move state machine is driven by {@link #tickBrain()} so tests can step it directly.
 */
public class ThornedRegent extends VestigeBoss {
    public static final int IDLE = 0;
    public static final int VOLLEY = 1;
    public static final int WALL = 2;
    public static final int STRIKE = 3;
    public static final int CALL = 4;

    public static final int VOLLEY_WINDUP = 20;
    public static final int WALL_WINDUP = 30;
    public static final int STRIKE_WINDUP = 25;
    public static final int CALL_WINDUP = 40;
    public static final int CORE_TICKS = 60;
    public static final double STRIKE_RADIUS = 2.5;
    public static final float STRIKE_DAMAGE = 14.0F;
    public static final int MAX_WALLS = 2;
    public static final int WALL_SEGMENTS = 15;
    public static final int MAX_STALKERS_ALIVE = 2;
    public static final int MAX_STALKERS_PER_FIGHT = 4;

    private static final int[] BASE_COOLDOWNS = {0, 100, 240, 160, 500};
    private static final int BASE_GAP = 40;

    private static final EntityDataAccessor<Integer> DATA_MOVE = SynchedEntityData.defineId(ThornedRegent.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_EXPOSED = SynchedEntityData.defineId(ThornedRegent.class, EntityDataSerializers.BOOLEAN);

    private int moveTimer;
    private int gap = BASE_GAP;
    private final int[] cooldowns = new int[5];
    private Vec3 mark = Vec3.ZERO;
    private Vec3 markDir = Vec3.ZERO;
    private int exposedTicks;
    private int wallCounter;
    private int stalkersSummoned;
    private final List<UUID> stalkers = new ArrayList<>();

    public ThornedRegent(EntityType<? extends ThornedRegent> type, Level level) {
        super(type, level);
        this.xpReward = 300;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected BossEvent.BossBarColor bossColor() {
        return BossEvent.BossBarColor.GREEN;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_MOVE, IDLE);
        entityData.define(DATA_EXPOSED, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new RegentMeleeGoal());
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** The boss bar (tests and the renderer-independent UI). */
    public net.minecraft.server.level.ServerBossEvent bar() {
        return bossEvent;
    }

    // ---- State accessors (synced for the renderer, readable by tests) ----

    public int move() {
        return entityData.get(DATA_MOVE);
    }

    private void setMove(int move) {
        entityData.set(DATA_MOVE, move);
    }

    /** True while the glowing core is exposed: x2 damage taken, no attacks. */
    public boolean isExposed() {
        return entityData.get(DATA_EXPOSED);
    }

    public void setExposed(int ticks) {
        exposedTicks = ticks;
        entityData.set(DATA_EXPOSED, ticks > 0);
    }

    public int exposedTicks() {
        return exposedTicks;
    }

    public int moveTimer() {
        return moveTimer;
    }

    public Vec3 mark() {
        return mark;
    }

    public int stalkersSummoned() {
        return stalkersSummoned;
    }

    public int cooldown(int move) {
        return cooldowns[move];
    }

    public void setCooldown(int move, int ticks) {
        cooldowns[move] = ticks;
    }

    private int scaledCooldown(int base) {
        return phase() >= 2 ? (int) (base * 0.8) : base;
    }

    // ---- Phases ----

    /** Phase 1 above 50% health, phase 2 at or below. */
    public void updatePhase() {
        if (!isAlive()) {
            return;
        }
        setPhase(getHealth() / getMaxHealth() > 0.5F ? 1 : 2);
    }

    // ---- Brain ----

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!isNoAi()) {
            tickBrain();
        }
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
        LivingEntity target = getTarget();
        if (target != null) {
            updatePhase();
        }
        if (exposedTicks > 0) {
            getNavigation().stop();
            if (level() instanceof ServerLevel server && exposedTicks % 4 == 0) {
                server.sendParticles(ParticleTypes.GLOW, getX(), getY() + 2.0, getZ(), 4, 0.5, 0.6, 0.5, 0.02);
            }
            setExposed(exposedTicks - 1);
            return;
        }
        if (move() != IDLE) {
            advanceMove();
            return;
        }
        if (gap > 0) {
            gap--;
            return;
        }
        if (target == null || !target.isAlive() || distanceTo(target) > 32.0) {
            return;
        }
        List<Integer> options = new ArrayList<>();
        double dist = distanceTo(target);
        if (cooldowns[VOLLEY] <= 0 && dist <= 28.0) {
            options.add(VOLLEY);
        }
        if (canWall() && dist >= 5.0) {
            options.add(WALL);
        }
        if (cooldowns[STRIKE] <= 0 && dist <= 22.0) {
            options.add(STRIKE);
        }
        if (canCall()) {
            options.add(CALL);
        }
        if (!options.isEmpty()) {
            beginMove(options.get(random.nextInt(options.size())));
        }
    }

    public boolean canWall() {
        return cooldowns[WALL] <= 0 && activeWallCount() < MAX_WALLS;
    }

    public boolean canCall() {
        return phase() >= 2 && cooldowns[CALL] <= 0 && summonableStalkers() > 0;
    }

    /** Starts a move's wind-up. Returns false if the move cannot start (no target, not allowed). */
    public boolean beginMove(int move) {
        LivingEntity target = getTarget();
        switch (move) {
            case VOLLEY -> {
                if (target == null) {
                    return false;
                }
                mark = target.getEyePosition();
                moveTimer = VOLLEY_WINDUP;
                Telegraph.windUp(this, ParticleTypes.COMPOSTER, BossesA.REGENT_VOLLEY.get(), 1.0F);
            }
            case WALL -> {
                if (target == null || activeWallCount() >= MAX_WALLS) {
                    return false;
                }
                Vec3 toTarget = BossACombat.flat(target.position().subtract(position()));
                Vec3 dir = toTarget.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : toTarget.normalize();
                mark = position().add(toTarget.scale(0.5));
                markDir = new Vec3(-dir.z, 0.0, dir.x);
                moveTimer = WALL_WINDUP;
                Telegraph.windUp(this, ParticleTypes.COMPOSTER, BossesA.REGENT_WALL.get(), 0.9F);
            }
            case STRIKE -> {
                if (target == null) {
                    return false;
                }
                mark = target.position();
                moveTimer = STRIKE_WINDUP;
                Telegraph.windUp(this, ParticleTypes.COMPOSTER, BossesA.REGENT_STRIKE.get(), 0.8F);
            }
            case CALL -> {
                if (summonableStalkers() <= 0 || phase() < 2) {
                    return false;
                }
                moveTimer = CALL_WINDUP;
                Telegraph.windUp(this, ParticleTypes.COMPOSTER, BossesA.REGENT_CALL.get(), 0.7F);
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
        LivingEntity target = getTarget();
        int move = move();
        if (target != null && move != STRIKE && move != WALL) {
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (move == VOLLEY) {
                mark = target.getEyePosition();
            }
        }
        moveTimer--;
        if (moveTimer > 0) {
            if (moveTimer % 5 == 0 && level() instanceof ServerLevel server) {
                switch (move) {
                    case WALL -> wallTelegraph(server);
                    case STRIKE -> Telegraph.ring(server, ParticleTypes.COMPOSTER, mark, STRIKE_RADIUS, 24);
                    case CALL -> Telegraph.windUp(this, ParticleTypes.COMPOSTER, SoundEvents.COMPOSTER_FILL, 0.6F);
                    default -> server.sendParticles(ParticleTypes.COMPOSTER, getX(), getY() + 3.0, getZ(), 3, 0.6, 0.3, 0.6, 0.0);
                }
            }
            return;
        }
        switch (move) {
            case VOLLEY -> fireVolley(mark);
            case WALL -> buildWall(mark, markDir);
            case STRIKE -> {
                strike(mark);
                setExposed(CORE_TICKS);
            }
            case CALL -> summonStalkers();
            default -> {}
        }
        setMove(IDLE);
        cooldowns[move] = scaledCooldown(BASE_COOLDOWNS[move]);
        gap = scaledCooldown(BASE_GAP);
    }

    private void wallTelegraph(ServerLevel server) {
        double half = WALL_SEGMENTS / 2.0;
        Vec3 a = mark.subtract(markDir.scale(half));
        Vec3 b = mark.add(markDir.scale(half));
        double ay = BossACombat.groundY(server, a.x, getY(), a.z);
        double by = BossACombat.groundY(server, b.x, getY(), b.z);
        Telegraph.line(server, ParticleTypes.HAPPY_VILLAGER, new Vec3(a.x, (Double.isNaN(ay) ? getY() : ay) + 0.15, a.z),
                new Vec3(b.x, (Double.isNaN(by) ? getY() : by) + 0.15, b.z), WALL_SEGMENTS * 2);
    }

    // ---- Moves ----

    /** Five thorns in a 40 degree fan towards {@code aim}. Returns the spikes. */
    public List<ThornSpike> fireVolley(Vec3 aim) {
        List<ThornSpike> spikes = new ArrayList<>();
        Vec3 origin = position().add(0.0, 2.6, 0.0);
        Vec3 base = aim.subtract(origin);
        base = base.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : base.normalize();
        for (int k = -2; k <= 2; k++) {
            Vec3 dir = base.yRot((float) Math.toRadians(k * 10.0));
            ThornSpike spike = ThornSpike.create(level(), this, origin.add(dir.scale(1.5)), dir.scale(1.1));
            level().addFreshEntity(spike);
            spikes.add(spike);
        }
        return spikes;
    }

    /** Raises a wall of up to 15 roots through {@code center} along {@code dir}; occupied or blocked spots are skipped. */
    public int buildWall(Vec3 center, Vec3 dir) {
        if (!(level() instanceof ServerLevel server)) {
            return 0;
        }
        int wallId = ++wallCounter;
        int placed = 0;
        int half = WALL_SEGMENTS / 2;
        for (int i = -half; i <= half; i++) {
            double x = center.x + dir.x * i;
            double z = center.z + dir.z * i;
            double y = BossACombat.groundY(server, x, center.y, z);
            if (Double.isNaN(y)) {
                continue;
            }
            AABB box = new AABB(x - 0.5, y, z - 0.5, x + 0.5, y + 2.0, z + 0.5);
            // Never inside a player. Blocks only for terrain: entity collision checks inflate the box slightly and
            // would reject neighbouring segments.
            if (!server.getEntitiesOfClass(Player.class, box.inflate(0.1)).isEmpty()
                    || !server.getEntitiesOfClass(ThornRoot.class, box.deflate(0.01)).isEmpty()
                    || server.getBlockCollisions(null, box).iterator().hasNext()) {
                continue;
            }
            ThornRoot root = new ThornRoot(BossesA.THORN_ROOT.get(), server);
            root.setPos(x, y, z);
            root.setOwner(getUUID(), wallId);
            server.addFreshEntity(root);
            placed++;
        }
        server.playSound(null, center.x, center.y, center.z, SoundEvents.COMPOSTER_READY, net.minecraft.sounds.SoundSource.HOSTILE, 1.2F, 0.6F);
        return placed;
    }

    /** Distinct walls of this boss currently standing. */
    public int activeWallCount() {
        Set<Integer> walls = new HashSet<>();
        for (ThornRoot root : ownedRoots()) {
            walls.add(root.wallId());
        }
        return walls.size();
    }

    private AABB arenaBox() {
        net.minecraft.core.BlockPos center = home() != null ? home() : blockPosition();
        double r = arenaRadius() + 8.0;
        return new AABB(center).inflate(r, 16.0, r);
    }

    public List<ThornRoot> ownedRoots() {
        return level().getEntitiesOfClass(ThornRoot.class, arenaBox(), r -> getUUID().equals(r.owner()) && r.isAlive());
    }

    /** Damage to everyone inside the ring at {@code center}. Returns the players hit. */
    public List<Player> strike(Vec3 center) {
        List<Player> hit = new ArrayList<>();
        if (level() instanceof ServerLevel server) {
            AABB box = new AABB(center.x - STRIKE_RADIUS, center.y - 2.0, center.z - STRIKE_RADIUS,
                    center.x + STRIKE_RADIUS, center.y + 3.0, center.z + STRIKE_RADIUS);
            for (Player player : BossACombat.victims(server, box)) {
                double dx = player.getX() - center.x;
                double dz = player.getZ() - center.z;
                if (dx * dx + dz * dz <= STRIKE_RADIUS * STRIKE_RADIUS) {
                    player.hurt(damageSources().mobAttack(this), scaledDamage(STRIKE_DAMAGE));
                    hit.add(player);
                }
            }
            server.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.3, center.z, 3, 1.0, 0.1, 1.0, 0.0);
            server.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.7F);
        }
        return hit;
    }

    // ---- Stalker Call ----

    private int aliveStalkers() {
        stalkers.removeIf(id -> !(level() instanceof ServerLevel server) || server.getEntity(id) == null || !server.getEntity(id).isAlive());
        return stalkers.size();
    }

    /** How many stalkers a call could summon right now (caps: 2 alive, 4 per fight). */
    public int summonableStalkers() {
        return Math.min(MAX_STALKERS_ALIVE - aliveStalkers(), MAX_STALKERS_PER_FIGHT - stalkersSummoned);
    }

    /** Summons up to 2 Glassroot Stalkers within the caps. Returns how many appeared. */
    public int summonStalkers() {
        if (!(level() instanceof ServerLevel server)) {
            return 0;
        }
        int count = Math.min(2, summonableStalkers());
        for (int i = 0; i < count; i++) {
            GlassrootStalker stalker = BeastMobs.GLASSROOT_STALKER.get().create(server);
            if (stalker == null) {
                break;
            }
            double angle = random.nextDouble() * Math.PI * 2.0;
            stalker.moveTo(getX() + Math.cos(angle) * 3.0, getY(), getZ() + Math.sin(angle) * 3.0, random.nextFloat() * 360.0F, 0.0F);
            stalker.setPersistenceRequired();
            stalker.setState(GlassrootStalker.ACTIVE);
            if (getTarget() != null) {
                stalker.setTarget(getTarget());
            }
            server.addFreshEntity(stalker);
            stalkers.add(stalker.getUUID());
            stalkersSummoned++;
        }
        return count;
    }

    private void cleanup() {
        if (level() instanceof ServerLevel server) {
            for (UUID id : stalkers) {
                Entity entity = server.getEntity(id);
                if (entity != null) {
                    entity.discard();
                }
            }
            for (ThornRoot root : ownedRoots()) {
                root.discard();
            }
        }
        stalkers.clear();
    }

    @Override
    protected void onArenaReset() {
        cleanup();
        stalkersSummoned = 0;
        wallCounter = 0;
        moveTimer = 0;
        gap = BASE_GAP;
        java.util.Arrays.fill(cooldowns, 0);
        setMove(IDLE);
        setExposed(0);
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) {
            cleanup();
        }
        super.die(source);
    }

    // ---- Combat overrides ----

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isExposed()) {
            amount = Math.min(amount * 2.0F, 1.0E6F);
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return move() == IDLE && !isExposed() && super.doHurtTarget(target);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WOOD_BREAK;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return isExposed() ? SoundEvents.AMETHYST_BLOCK_HIT : SoundEvents.WOOD_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AZALEA_LEAVES_BREAK;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.5F;
    }

    // ---- NBT ----

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("RegentMove", move());
        tag.putInt("MoveTimer", moveTimer);
        tag.putInt("Gap", gap);
        tag.putIntArray("Cooldowns", cooldowns);
        BossACombat.putVec(tag, "Mark", mark);
        BossACombat.putVec(tag, "MarkDir", markDir);
        tag.putInt("ExposedTicks", exposedTicks);
        tag.putInt("WallCounter", wallCounter);
        tag.putInt("StalkersSummoned", stalkersSummoned);
        ListTag list = new ListTag();
        for (UUID id : stalkers) {
            list.add(NbtUtils.createUUID(id));
        }
        tag.put("Stalkers", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setMove(tag.getInt("RegentMove"));
        moveTimer = tag.getInt("MoveTimer");
        gap = tag.contains("Gap") ? tag.getInt("Gap") : BASE_GAP;
        int[] saved = tag.getIntArray("Cooldowns");
        for (int i = 0; i < cooldowns.length; i++) {
            cooldowns[i] = i < saved.length ? saved[i] : 0;
        }
        Vec3 m = BossACombat.getVec(tag, "Mark");
        mark = m == null ? Vec3.ZERO : m;
        Vec3 d = BossACombat.getVec(tag, "MarkDir");
        markDir = d == null ? Vec3.ZERO : d;
        setExposed(tag.getInt("ExposedTicks"));
        wallCounter = tag.getInt("WallCounter");
        stalkersSummoned = tag.getInt("StalkersSummoned");
        stalkers.clear();
        for (Tag t : tag.getList("Stalkers", Tag.TAG_INT_ARRAY)) {
            if (t instanceof IntArrayTag) {
                stalkers.add(NbtUtils.loadUUID(t));
            }
        }
    }

    /** Melee only while idle and not exposed. */
    private final class RegentMeleeGoal extends MeleeAttackGoal {
        RegentMeleeGoal() {
            super(ThornedRegent.this, 1.0, true);
        }

        @Override
        public boolean canUse() {
            return move() == IDLE && !isExposed() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return move() == IDLE && !isExposed() && super.canContinueToUse();
        }
    }
}
