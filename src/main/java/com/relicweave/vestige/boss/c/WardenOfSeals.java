package com.relicweave.vestige.boss.c;

import com.relicweave.vestige.boss.VestigeBoss;
import com.relicweave.vestige.entity.Telegraph;
import com.relicweave.vestige.entity.ethereal.WindupState;
import com.relicweave.vestige.entity.humanoid.HumanoidMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Warden of Seals (Hall of Nine Seals). Its rule is the <b>Seal Ward</b>: while any of its three seal nodes lives it
 * is immune; the nodes take turns being exposed. Breaking all three leaves it vulnerable for 15 seconds, then it
 * re-seals (new nodes) until three cycles have been broken or it is below 30% health. Moves: Seal Slam (ring), Rune
 * Lances (three fanned lines) and, in phase 2, Summon Wardens (reliquary sentinels, capped). All state is saved in NBT.
 */
public class WardenOfSeals extends VestigeBoss implements WindupState {
    public static final int MOVE_NONE = 0;
    public static final int MOVE_SLAM = 1;
    public static final int MOVE_LANCES = 2;
    public static final int MOVE_SUMMON = 3;

    public static final int NODE_COUNT = 3;
    public static final double NODE_DISTANCE = 9.0;
    public static final int EXPOSE_TICKS = 120;
    public static final int VULNERABLE_TICKS = 300;
    public static final int MAX_CYCLES = 3;
    public static final float STOP_RESEAL_FRACTION = 0.30F;

    public static final int SLAM_WINDUP = 25;
    public static final double SLAM_RADIUS = 4.0;
    public static final float SLAM_DAMAGE = 12.0F;
    public static final int LANCE_WINDUP = 20;
    public static final int LANCE_COUNT = 3;
    public static final double LANCE_LENGTH = 16.0;
    public static final double LANCE_WIDTH = 1.0;
    public static final double LANCE_SPREAD = Math.toRadians(20.0);
    public static final float LANCE_DAMAGE = 9.0F;
    public static final int SUMMON_WINDUP = 30;
    public static final int SUMMON_BATCH = 2;
    public static final int MAX_SUMMONS_ALIVE = 4;
    public static final int MAX_SUMMONS_TOTAL = 8;
    public static final int RECOVER_TICKS = 30;
    public static final int COOLDOWN_TICKS = 50;
    public static final int SUMMON_COOLDOWN_TICKS = 300;

    private static final EntityDataAccessor<Float> DATA_PROGRESS = SynchedEntityData.defineId(WardenOfSeals.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_SEALED = SynchedEntityData.defineId(WardenOfSeals.class, EntityDataSerializers.BOOLEAN);

    private final List<UUID> nodes = new ArrayList<>();
    private final List<UUID> summons = new ArrayList<>();
    private int summonedTotal;
    private int cycles;
    private int vulnerableTicks;
    private int exposeTicks;
    private int exposeIndex = -1;
    private boolean wasSealed;
    private int move;
    private int moveTicks;
    private int moveTotal = 1;
    private int recover;
    private int cooldown = COOLDOWN_TICKS;
    private int summonCooldown = SUMMON_COOLDOWN_TICKS;
    private double lanceAngle;

    public WardenOfSeals(EntityType<? extends WardenOfSeals> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400.0)
                .add(Attributes.ARMOR, 14.0)
                .add(Attributes.ATTACK_DAMAGE, 13.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected BossEvent.BossBarColor bossColor() {
        return BossEvent.BossBarColor.YELLOW;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_PROGRESS, 0.0F);
        entityData.define(DATA_SEALED, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new BossCombat.BossMelee(this, this::isBusy));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** The boss bar (tests and commands). */
    public net.minecraft.server.level.ServerBossEvent bossBar() {
        return bossEvent;
    }

    // ---- Accessors (also used by tests and the client model) ----

    public int move() {
        return move;
    }

    public int moveTicks() {
        return moveTicks;
    }

    public int recoverTicks() {
        return recover;
    }

    public int cycles() {
        return cycles;
    }

    public void setCycles(int cycles) {
        this.cycles = cycles;
    }

    public int vulnerableTicks() {
        return vulnerableTicks;
    }

    public void setVulnerableTicks(int ticks) {
        this.vulnerableTicks = ticks;
    }

    public int summonedTotal() {
        return summonedTotal;
    }

    public boolean isBusy() {
        return move != MOVE_NONE || recover > 0;
    }

    @Override
    public float windupProgress() {
        return entityData.get(DATA_PROGRESS);
    }

    /** Server: true while any seal node lives (immune). Client: the synced flag. */
    public boolean isSealed() {
        return level().isClientSide ? entityData.get(DATA_SEALED) : !livingNodes().isEmpty();
    }

    /** Living seal nodes this Warden raised. */
    public List<SealNode> livingNodes() {
        List<SealNode> out = new ArrayList<>();
        if (level() instanceof ServerLevel server) {
            for (UUID id : nodes) {
                if (server.getEntity(id) instanceof SealNode node && node.isAlive()) {
                    out.add(node);
                }
            }
        }
        return out;
    }

    public List<Mob> livingSummons() {
        List<Mob> out = new ArrayList<>();
        if (level() instanceof ServerLevel server) {
            for (UUID id : summons) {
                if (server.getEntity(id) instanceof Mob mob && mob.isAlive()) {
                    out.add(mob);
                }
            }
        }
        return out;
    }

    // ---- Seal Ward ----

    @Override
    protected void onFightStart(int players) {
        raiseNodes();
    }

    /** Raises three fresh seal nodes evenly spaced {@value #NODE_DISTANCE} blocks from home and exposes the first. */
    public void raiseNodes() {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        discardNodes();
        BlockPos home = home() != null ? home() : blockPosition();
        double start = Math.PI / 2.0 + cycles * Math.toRadians(60.0);
        for (int i = 0; i < NODE_COUNT; i++) {
            double a = start + i * 2.0 * Math.PI / NODE_COUNT;
            SealNode node = BossesC.SEAL_NODE.get().create(server);
            if (node == null) {
                continue;
            }
            node.moveTo(home.getX() + 0.5 + Math.cos(a) * NODE_DISTANCE, home.getY(), home.getZ() + 0.5 + Math.sin(a) * NODE_DISTANCE, 0.0F, 0.0F);
            node.setOwner(getUUID());
            server.addFreshEntity(node);
            nodes.add(node.getUUID());
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, node.getX(), node.getY() + 1.0, node.getZ(), 20, 0.4, 0.8, 0.4, 0.05);
        }
        wasSealed = !nodes.isEmpty();
        exposeIndex = -1;
        rotateExposure();
        entityData.set(DATA_SEALED, wasSealed);
        announce(Component.translatable("boss.relicweave.warden_of_seals.sealed"));
    }

    /** Exposes the next living node (and covers the others) for {@value #EXPOSE_TICKS} ticks. */
    public void rotateExposure() {
        List<SealNode> living = livingNodes();
        if (living.isEmpty()) {
            return;
        }
        exposeIndex = (exposeIndex + 1) % living.size();
        for (int i = 0; i < living.size(); i++) {
            living.get(i).setExposed(i == exposeIndex);
        }
        exposeTicks = EXPOSE_TICKS;
    }

    /** One tick of the Seal Ward: notices broken seals, rotates exposure, counts down vulnerability and re-seals. */
    public void updateSeal() {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        List<SealNode> living = livingNodes();
        boolean sealed = !living.isEmpty();
        boolean broken = wasSealed && !sealed;
        if (broken) {
            onSealBroken();
        }
        wasSealed = sealed;
        entityData.set(DATA_SEALED, sealed);
        if (broken) {
            return;
        }
        if (sealed) {
            if (--exposeTicks <= 0 || living.stream().noneMatch(SealNode::isExposed)) {
                rotateExposure();
            }
            if (tickCount % 5 == 0) {
                for (int i = 0; i < 6; i++) {
                    double a = tickCount * 0.15 + i * Math.PI / 3.0;
                    server.sendParticles(ParticleTypes.ENCHANT, getX() + Math.cos(a) * 1.6, getY() + 1.0 + (i % 3) * 0.6, getZ() + Math.sin(a) * 1.6,
                            1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        } else if (vulnerableTicks > 0 && --vulnerableTicks <= 0) {
            endVulnerability();
        }
    }

    private void onSealBroken() {
        cycles++;
        vulnerableTicks = VULNERABLE_TICKS;
        announce(Component.translatable("boss.relicweave.warden_of_seals.broken"));
        if (level() instanceof ServerLevel server) {
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.5F, 0.8F);
            server.sendParticles(ParticleTypes.CRIT, getX(), getY() + 1.5, getZ(), 30, 0.6, 0.9, 0.6, 0.2);
        }
    }

    private void endVulnerability() {
        if (canReseal()) {
            raiseNodes();
        } else {
            announce(Component.translatable("boss.relicweave.warden_of_seals.unsealed"));
        }
    }

    /** False after {@value #MAX_CYCLES} broken cycles or below 30% health. */
    public boolean canReseal() {
        return cycles < MAX_CYCLES && getHealth() >= getMaxHealth() * STOP_RESEAL_FRACTION;
    }

    public void updatePhase() {
        setPhase(getHealth() <= getMaxHealth() * 0.5F ? 2 : 1);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && isSealed() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            BossCombat.clink(this);
            return false;
        }
        return super.hurt(source, amount);
    }

    // ---- Moves ----

    @Override
    public boolean doHurtTarget(Entity target) {
        return !isBusy() && super.doHurtTarget(target);
    }

    /** Starts a move: plays its telegraph cue and shows the marked area. */
    public void beginMove(int newMove) {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        move = newMove;
        getNavigation().stop();
        switch (newMove) {
            case MOVE_SLAM -> {
                moveTicks = SLAM_WINDUP;
                Telegraph.windUp(this, ParticleTypes.ENCHANT, BossesC.WARDEN_SLAM.get(), 0.9F);
            }
            case MOVE_LANCES -> {
                moveTicks = LANCE_WINDUP;
                LivingEntity target = getTarget();
                lanceAngle = target != null ? BossCombat.angleTo(position(), target.position()) : Math.toRadians(yBodyRot + 90.0);
                Telegraph.windUp(this, ParticleTypes.ELECTRIC_SPARK, BossesC.WARDEN_LANCES.get(), 1.0F);
            }
            case MOVE_SUMMON -> {
                moveTicks = SUMMON_WINDUP;
                Telegraph.windUp(this, ParticleTypes.SOUL_FIRE_FLAME, BossesC.WARDEN_SUMMON.get(), 0.8F);
            }
            default -> {
                move = MOVE_NONE;
                return;
            }
        }
        moveTotal = moveTicks;
        drawTelegraph(server);
    }

    private void drawTelegraph(ServerLevel server) {
        switch (move) {
            case MOVE_SLAM -> Telegraph.ring(server, ParticleTypes.ENCHANT, position(), SLAM_RADIUS, 32);
            case MOVE_LANCES -> {
                Vec3 from = position().add(0.0, 0.3, 0.0);
                for (int i = 0; i < LANCE_COUNT; i++) {
                    BossCombat.drawLine(server, ParticleTypes.ELECTRIC_SPARK, from, lanceAngle + (i - 1) * LANCE_SPREAD, LANCE_LENGTH);
                }
            }
            case MOVE_SUMMON -> server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 1.0, getZ(), 8, 1.5, 0.8, 1.5, 0.02);
            default -> {}
        }
    }

    /** One AI tick of the active move (public so tests can drive it). */
    public void tickMove() {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        if (move == MOVE_NONE) {
            if (recover > 0) {
                getNavigation().stop();
                if (--recover <= 0) {
                    cooldown = COOLDOWN_TICKS + random.nextInt(20);
                }
            }
            entityData.set(DATA_PROGRESS, 0.0F);
            return;
        }
        getNavigation().stop();
        moveTicks--;
        entityData.set(DATA_PROGRESS, Math.min(1.0F, 1.0F - moveTicks / (float) moveTotal));
        if (moveTicks > 0) {
            if (moveTicks % 5 == 0) {
                drawTelegraph(server);
            }
            return;
        }
        int finished = move;
        move = MOVE_NONE;
        recover = RECOVER_TICKS;
        entityData.set(DATA_PROGRESS, 0.0F);
        switch (finished) {
            case MOVE_SLAM -> resolveSlam(server);
            case MOVE_LANCES -> resolveLances(server);
            case MOVE_SUMMON -> spawnSummons(SUMMON_BATCH);
            default -> {}
        }
    }

    private void resolveSlam(ServerLevel server) {
        server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 4, SLAM_RADIUS * 0.4, 0.1, SLAM_RADIUS * 0.4, 0.0);
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.0F, 0.9F);
        for (LivingEntity victim : BossCombat.inRing(this, position(), SLAM_RADIUS)) {
            victim.hurt(damageSources().mobAttack(this), scaledDamage(SLAM_DAMAGE));
        }
    }

    private void resolveLances(ServerLevel server) {
        Vec3 from = position().add(0.0, 0.3, 0.0);
        Set<LivingEntity> hit = new LinkedHashSet<>();
        for (int i = 0; i < LANCE_COUNT; i++) {
            double angle = lanceAngle + (i - 1) * LANCE_SPREAD;
            BossCombat.drawLine(server, ParticleTypes.CRIT, from, angle, LANCE_LENGTH);
            hit.addAll(BossCombat.alongLine(this, from, angle, LANCE_LENGTH, LANCE_WIDTH));
        }
        for (LivingEntity victim : hit) {
            victim.hurt(damageSources().indirectMagic(this, this), scaledDamage(LANCE_DAMAGE));
        }
    }

    /** Summons up to {@code count} reliquary sentinels, respecting {@value #MAX_SUMMONS_ALIVE} alive and {@value #MAX_SUMMONS_TOTAL} per fight. */
    public int spawnSummons(int count) {
        if (!(level() instanceof ServerLevel server)) {
            return 0;
        }
        summons.removeIf(id -> !(server.getEntity(id) instanceof Mob mob) || !mob.isAlive());
        int room = Math.min(count, Math.min(MAX_SUMMONS_ALIVE - summons.size(), MAX_SUMMONS_TOTAL - summonedTotal));
        int spawned = 0;
        for (int i = 0; i < room; i++) {
            Mob sentinel = HumanoidMobs.RELIQUARY_SENTINEL.get().create(server);
            if (sentinel == null) {
                continue;
            }
            double a = random.nextDouble() * Math.PI * 2.0;
            sentinel.moveTo(getX() + Math.cos(a) * 3.0, getY(), getZ() + Math.sin(a) * 3.0, random.nextFloat() * 360.0F, 0.0F);
            sentinel.setPersistenceRequired();
            sentinel.setTarget(getTarget());
            server.addFreshEntity(sentinel);
            summons.add(sentinel.getUUID());
            summonedTotal++;
            spawned++;
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, sentinel.getX(), sentinel.getY() + 1.0, sentinel.getZ(), 12, 0.3, 0.6, 0.3, 0.03);
        }
        return spawned;
    }

    private void chooseMove() {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || distanceToSqr(target) > 24.0 * 24.0) {
            return;
        }
        if (phase() >= 2 && summonCooldown <= 0 && livingSummons().size() < MAX_SUMMONS_ALIVE && summonedTotal < MAX_SUMMONS_TOTAL) {
            summonCooldown = SUMMON_COOLDOWN_TICKS;
            beginMove(MOVE_SUMMON);
        } else if (distanceToSqr(target) <= 7.0 * 7.0 && random.nextFloat() < 0.6F) {
            beginMove(MOVE_SLAM);
        } else {
            beginMove(MOVE_LANCES);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!isScaled() || !isAlive()) {
            return;
        }
        updatePhase();
        updateSeal();
        if (summonCooldown > 0) {
            summonCooldown--;
        }
        if (isBusy()) {
            tickMove();
        } else if (cooldown > 0) {
            cooldown--;
        } else {
            chooseMove();
        }
    }

    // ---- Reset and death ----

    private void discardNodes() {
        if (level() instanceof ServerLevel server) {
            for (UUID id : nodes) {
                Entity node = server.getEntity(id);
                if (node != null) {
                    node.discard();
                }
            }
        }
        nodes.clear();
    }

    private void discardSummons() {
        if (level() instanceof ServerLevel server) {
            for (UUID id : summons) {
                Entity mob = server.getEntity(id);
                if (mob != null) {
                    mob.discard();
                }
            }
        }
        summons.clear();
    }

    @Override
    protected void onArenaReset() {
        discardNodes();
        discardSummons();
        summonedTotal = 0;
        cycles = 0;
        vulnerableTicks = 0;
        exposeTicks = 0;
        exposeIndex = -1;
        wasSealed = false;
        move = MOVE_NONE;
        moveTicks = 0;
        recover = 0;
        cooldown = COOLDOWN_TICKS;
        summonCooldown = SUMMON_COOLDOWN_TICKS;
        entityData.set(DATA_SEALED, false);
        entityData.set(DATA_PROGRESS, 0.0F);
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) {
            discardNodes();
            discardSummons();
        }
        super.die(source);
    }

    // ---- NBT ----

    private static ListTag uuids(List<UUID> list) {
        ListTag tag = new ListTag();
        for (UUID id : list) {
            tag.add(NbtUtils.createUUID(id));
        }
        return tag;
    }

    private static void readUuids(ListTag tag, List<UUID> into) {
        into.clear();
        for (Tag t : tag) {
            into.add(NbtUtils.loadUUID(t));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("SealNodes", uuids(nodes));
        tag.put("Summons", uuids(summons));
        tag.putInt("SummonedTotal", summonedTotal);
        tag.putInt("SealCycles", cycles);
        tag.putInt("VulnerableTicks", vulnerableTicks);
        tag.putInt("ExposeTicks", exposeTicks);
        tag.putInt("ExposeIndex", exposeIndex);
        tag.putBoolean("WasSealed", wasSealed);
        tag.putInt("Move", move);
        tag.putInt("MoveTicks", moveTicks);
        tag.putInt("MoveTotal", moveTotal);
        tag.putInt("Recover", recover);
        tag.putInt("Cooldown", cooldown);
        tag.putInt("SummonCooldown", summonCooldown);
        tag.putDouble("LanceAngle", lanceAngle);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        readUuids(tag.getList("SealNodes", Tag.TAG_INT_ARRAY), nodes);
        readUuids(tag.getList("Summons", Tag.TAG_INT_ARRAY), summons);
        summonedTotal = tag.getInt("SummonedTotal");
        cycles = tag.getInt("SealCycles");
        vulnerableTicks = tag.getInt("VulnerableTicks");
        exposeTicks = tag.getInt("ExposeTicks");
        exposeIndex = tag.contains("ExposeIndex") ? tag.getInt("ExposeIndex") : -1;
        wasSealed = tag.getBoolean("WasSealed");
        move = tag.getInt("Move");
        moveTicks = tag.getInt("MoveTicks");
        moveTotal = Math.max(1, tag.getInt("MoveTotal"));
        recover = tag.getInt("Recover");
        cooldown = tag.getInt("Cooldown");
        summonCooldown = tag.getInt("SummonCooldown");
        lanceAngle = tag.getDouble("LanceAngle");
    }
}
