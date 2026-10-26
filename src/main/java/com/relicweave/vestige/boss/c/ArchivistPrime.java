package com.relicweave.vestige.boss.c;

import com.relicweave.vestige.boss.VestigeBoss;
import com.relicweave.vestige.cycle.CyclePhase;
import com.relicweave.vestige.entity.Telegraph;
import com.relicweave.vestige.entity.ethereal.WindupState;
import com.relicweave.vestige.hazard.FieldRegistry;
import com.relicweave.vestige.hazard.field.StarfallImpacts;
import com.relicweave.vestige.hazard.field.StarfallMarker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Archivist-Prime, the final boss of the Grand Index. Stage 1: Index Lances and Glyph Volley. Stage 2 (66%): Cycle
 * Shift, an arena phase imposed every 20 seconds (Quiet / Resonance rune rings / Convergence starfall markers).
 * Stage 3 (33%): Final Seal (two always-exposed index nodes make it immune) and the Index Aura (2 damage per second
 * within 4 blocks), with attacks 15% faster. All state is saved in NBT.
 */
public class ArchivistPrime extends VestigeBoss implements WindupState {
    public static final int MOVE_NONE = 0;
    public static final int MOVE_LANCES = 1;
    public static final int MOVE_VOLLEY = 2;

    public static final float STAGE2_FRACTION = 0.66F;
    public static final float STAGE3_FRACTION = 0.33F;

    public static final int LANCE_WINDUP = 20;
    public static final int LANCE_COUNT = 2;
    public static final double LANCE_LENGTH = 18.0;
    public static final double LANCE_WIDTH = 1.0;
    public static final double LANCE_SPREAD = Math.toRadians(20.0);
    public static final float LANCE_DAMAGE = 10.0F;
    public static final int VOLLEY_WINDUP = 15;
    public static final int VOLLEY_PAGES = 5;
    public static final double VOLLEY_SPREAD = Math.toRadians(10.0);
    public static final double PAGE_SPEED = 0.9;
    public static final int RECOVER_TICKS = 25;
    public static final int BASE_COOLDOWN_TICKS = 70;
    public static final double STAGE3_COOLDOWN_FACTOR = 0.85;

    public static final int CYCLE_TICKS = 400;
    public static final int RING_COUNT = 3;
    public static final int RING_MAX = 6;
    public static final int RING_WARNING = 40;
    public static final double RING_RADIUS = 3.0;
    public static final float RING_DAMAGE = 8.0F;
    public static final int RING_WAVE_TICKS = 100;
    public static final int MARKER_WARNING = 60;
    public static final float MARKER_DAMAGE = 10.0F;
    public static final int MARKER_WAVE_TICKS = 120;

    public static final int NODE_COUNT = 2;
    public static final double NODE_FALLBACK_DISTANCE = 12.0;
    public static final double AURA_RADIUS = 4.0;
    public static final float AURA_DAMAGE = 2.0F;

    private static final EntityDataAccessor<Float> DATA_PROGRESS = SynchedEntityData.defineId(ArchivistPrime.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_SEALED = SynchedEntityData.defineId(ArchivistPrime.class, EntityDataSerializers.BOOLEAN);

    /** A telegraphed Resonance ring that is waiting to strike. */
    public static final class Ring {
        final Vec3 center;
        int ticks;

        Ring(Vec3 center, int ticks) {
            this.center = center;
            this.ticks = ticks;
        }

        public Vec3 center() {
            return center;
        }

        public int ticks() {
            return ticks;
        }
    }

    private final List<UUID> nodes = new ArrayList<>();
    private final List<Ring> rings = new ArrayList<>();
    private int move;
    private int moveTicks;
    private int moveTotal = 1;
    private int recover;
    private int cooldown = BASE_COOLDOWN_TICKS;
    private double lanceAngle;
    private int lanceSide = 1;
    private int cycleIndex = -1;
    private int cycleTicks;
    private int waveTicks;
    private boolean finalSealRaised;

    public ArchivistPrime(EntityType<? extends ArchivistPrime> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 600.0)
                .add(Attributes.ARMOR, 16.0)
                .add(Attributes.ATTACK_DAMAGE, 14.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected BossEvent.BossBarColor bossColor() {
        return BossEvent.BossBarColor.PINK;
    }

    @Override
    public double arenaRadius() {
        return 40.0;
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

    // ---- Accessors ----

    public int move() {
        return move;
    }

    public int moveTicks() {
        return moveTicks;
    }

    public int recoverTicks() {
        return recover;
    }

    public boolean isBusy() {
        return move != MOVE_NONE || recover > 0;
    }

    public List<Ring> rings() {
        return rings;
    }

    public int cycleIndex() {
        return cycleIndex;
    }

    public int cycleTicks() {
        return cycleTicks;
    }

    public boolean finalSealRaised() {
        return finalSealRaised;
    }

    @Override
    public float windupProgress() {
        return entityData.get(DATA_PROGRESS);
    }

    /** Stage 1, 2 or 3 for a health fraction. */
    public static int stageFor(float fraction) {
        if (fraction < STAGE3_FRACTION) {
            return 3;
        }
        return fraction <= STAGE2_FRACTION ? 2 : 1;
    }

    public int stage() {
        return stageFor(getHealth() / getMaxHealth());
    }

    /** Ticks between attacks: 70, or 59 in stage 3. */
    public int cooldownTicks() {
        return (int) Math.round(BASE_COOLDOWN_TICKS * (phase() >= 3 ? STAGE3_COOLDOWN_FACTOR : 1.0));
    }

    /** Sets the stage from current health, starting Cycle Shift / Final Seal on first entry. */
    public void updateStage() {
        int stage = stage();
        setPhase(stage);
        if (stage >= 2 && cycleIndex < 0) {
            cycleIndex = -1;
            if (cycleTicks <= 0) {
                cycleTicks = 100;
            }
        }
        if (stage >= 3 && !finalSealRaised) {
            raiseFinalSeal();
        }
    }

    // ---- Final Seal ----

    public List<IndexNode> livingNodes() {
        List<IndexNode> out = new ArrayList<>();
        if (level() instanceof ServerLevel server) {
            for (UUID id : nodes) {
                if (server.getEntity(id) instanceof IndexNode node && node.isAlive()) {
                    out.add(node);
                }
            }
        }
        return out;
    }

    /** Server: true while an index node lives (immune). Client: the synced flag. */
    public boolean isSealed() {
        return level().isClientSide ? entityData.get(DATA_SEALED) : !livingNodes().isEmpty();
    }

    /** Where the two nodes go: beside {@code null_ward} blocks at the arena edge if present, else {@value #NODE_FALLBACK_DISTANCE} blocks from home. */
    public List<Vec3> nodePositions() {
        BlockPos home = home() != null ? home() : blockPosition();
        double hx = home.getX() + 0.5;
        double hz = home.getZ() + 0.5;
        List<Vec3> out = new ArrayList<>();
        if (level() instanceof ServerLevel server) {
            server.getPoiManager().getInSquare(holder -> holder.value() == FieldRegistry.NULL_WARD_POI.get(), home,
                            (int) arenaRadius(), net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY)
                    .map(record -> record.getPos())
                    .filter(p -> Math.abs(p.getY() - home.getY()) <= 8)
                    .sorted((a, b) -> Double.compare(b.distSqr(home), a.distSqr(home)))
                    .limit(NODE_COUNT)
                    .forEach(p -> out.add(new Vec3(p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5)));
        }
        for (int i = out.size(); i < NODE_COUNT; i++) {
            double a = i == 0 ? 0.0 : Math.PI;
            if (!out.isEmpty()) {
                a = Math.atan2(hz - out.get(0).z, hx - out.get(0).x);
            }
            out.add(new Vec3(hx + Math.cos(a) * NODE_FALLBACK_DISTANCE, home.getY(), hz + Math.sin(a) * NODE_FALLBACK_DISTANCE));
        }
        return out;
    }

    /** Raises the two index nodes; the Prime is immune until both are destroyed. */
    public void raiseFinalSeal() {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        discardNodes();
        for (Vec3 pos : nodePositions()) {
            IndexNode node = BossesC.INDEX_NODE.get().create(server);
            if (node == null) {
                continue;
            }
            node.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
            node.setOwner(getUUID());
            server.addFreshEntity(node);
            nodes.add(node.getUUID());
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 1.0, pos.z, 20, 0.4, 0.8, 0.4, 0.05);
        }
        finalSealRaised = true;
        entityData.set(DATA_SEALED, !nodes.isEmpty());
        server.playSound(null, getX(), getY(), getZ(), BossesC.PRIME_FINAL_SEAL.get(), SoundSource.HOSTILE, 2.0F, 0.9F);
        announce(Component.translatable("boss.relicweave.archivist_prime.final_seal"));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && isSealed() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            BossCombat.clink(this);
            return false;
        }
        return super.hurt(source, amount);
    }

    /** Index Aura: 2 damage (times arena hazard strength) to everything within 4 blocks. Returns how many were hurt. */
    public int applyAura() {
        int hurt = 0;
        for (LivingEntity victim : BossCombat.inRing(this, position(), AURA_RADIUS)) {
            if (victim.hurt(damageSources().indirectMagic(this, this), AURA_DAMAGE * (float) arenaHazardStrength())) {
                hurt++;
            }
        }
        return hurt;
    }

    private void drawAuraDome(ServerLevel server) {
        for (int i = 0; i < 28; i++) {
            double a = random.nextDouble() * Math.PI * 2.0;
            double h = random.nextDouble() * Math.PI / 2.0;
            double r = AURA_RADIUS * Math.cos(h);
            server.sendParticles(ParticleTypes.ENCHANT, getX() + Math.cos(a) * r, getY() + AURA_RADIUS * Math.sin(h) * 0.8 + 0.2,
                    getZ() + Math.sin(a) * r, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    // ---- Cycle Shift ----

    /** The arena phase currently imposed, or QUIET before the first shift. */
    public CyclePhase cyclePhase() {
        return cycleIndex < 0 ? CyclePhase.QUIET : CyclePhase.values()[cycleIndex % CyclePhase.values().length];
    }

    /** Imposes {@code phase}: action-bar announcement, colored particle wash, and its hazard. */
    public void applyCyclePhase(CyclePhase phase, List<ServerPlayer> players) {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        cycleIndex = phase.ordinal();
        cycleTicks = CYCLE_TICKS;
        waveTicks = phase == CyclePhase.RESONANCE ? RING_WAVE_TICKS : MARKER_WAVE_TICKS;
        Component name = Component.translatable("boss.relicweave.archivist_prime.cycle", phase.displayName());
        DustParticleOptions dust = new DustParticleOptions(washColor(phase), 1.4F);
        for (ServerPlayer player : players) {
            player.displayClientMessage(name, true);
            server.sendParticles(dust, player.getX(), player.getY() + 1.0, player.getZ(), 40, 3.0, 1.5, 3.0, 0.0);
        }
        server.playSound(null, getX(), getY(), getZ(), BossesC.PRIME_CYCLE_SHIFT.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
        runWave(phase, players);
    }

    private static Vector3f washColor(CyclePhase phase) {
        return switch (phase) {
            case QUIET -> new Vector3f(0.35F, 0.85F, 1.0F);
            case RESONANCE -> new Vector3f(1.0F, 0.7F, 0.15F);
            case CONVERGENCE -> new Vector3f(0.85F, 0.35F, 1.0F);
        };
    }

    private void runWave(CyclePhase phase, List<ServerPlayer> players) {
        if (phase == CyclePhase.RESONANCE) {
            spawnRings(players);
        } else if (phase == CyclePhase.CONVERGENCE) {
            if (ownedMarkers() < 2 * Math.max(1, players.size())) {
                spawnConvergenceMarkers(players);
            }
        }
    }

    private int ownedMarkers() {
        if (!(level() instanceof ServerLevel server)) {
            return 0;
        }
        return server.getEntitiesOfClass(StarfallMarker.class, getBoundingBox().inflate(arenaRadius() + 8.0),
                m -> getUUID().equals(m.ownerId())).size();
    }

    /** Convergence: one Starfall marker (warning 60 ticks, scaled 10 damage) on each player. Returns how many. */
    public int spawnConvergenceMarkers(List<ServerPlayer> players) {
        if (!(level() instanceof ServerLevel server)) {
            return 0;
        }
        for (ServerPlayer player : players) {
            StarfallImpacts.spawnMarker(server, player.position(), MARKER_WARNING, scaledDamage(MARKER_DAMAGE), this);
        }
        return players.size();
    }

    /** Resonance: {@value #RING_COUNT} rune rings around the players, struck after {@value #RING_WARNING} ticks. Returns how many were laid. */
    public int spawnRings(List<ServerPlayer> players) {
        if (!(level() instanceof ServerLevel server) || players.isEmpty()) {
            return 0;
        }
        int laid = 0;
        for (int i = 0; i < RING_COUNT && rings.size() < RING_MAX; i++) {
            Vec3 at = players.get(i % players.size()).position();
            if (i > 0) {
                double a = random.nextDouble() * Math.PI * 2.0;
                at = at.add(Math.cos(a) * 3.0, 0.0, Math.sin(a) * 3.0);
            }
            rings.add(new Ring(at, RING_WARNING));
            Telegraph.ring(server, ParticleTypes.GLOW, at, RING_RADIUS, 24);
            laid++;
        }
        server.playSound(null, getX(), getY(), getZ(), BossesC.PRIME_RINGS.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
        return laid;
    }

    /** One tick of every pending ring: redraw the warning, strike when the timer ends. */
    public void tickRings() {
        if (!(level() instanceof ServerLevel server) || rings.isEmpty()) {
            return;
        }
        List<Ring> done = new ArrayList<>();
        for (Ring ring : rings) {
            ring.ticks--;
            if (ring.ticks <= 0) {
                done.add(ring);
            } else if (ring.ticks % 5 == 0) {
                Telegraph.ring(server, ParticleTypes.GLOW, ring.center, RING_RADIUS, 24);
            }
        }
        for (Ring ring : done) {
            rings.remove(ring);
            server.sendParticles(ParticleTypes.FLASH, ring.center.x, ring.center.y + 0.3, ring.center.z, 1, 0.0, 0.0, 0.0, 0.0);
            server.sendParticles(ParticleTypes.ENCHANTED_HIT, ring.center.x, ring.center.y + 0.3, ring.center.z, 30, RING_RADIUS * 0.5, 0.2, RING_RADIUS * 0.5, 0.1);
            server.playSound(null, ring.center.x, ring.center.y, ring.center.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2.0F, 0.7F);
            for (LivingEntity victim : BossCombat.inRing(this, ring.center, RING_RADIUS)) {
                victim.hurt(damageSources().indirectMagic(this, this), scaledDamage(RING_DAMAGE) * (float) arenaHazardStrength());
            }
        }
    }

    private void tickCycle(List<ServerPlayer> players) {
        if (cycleIndex < 0 && cycleTicks <= 0) {
            return;
        }
        if (--cycleTicks <= 0) {
            applyCyclePhase(CyclePhase.values()[(cycleIndex + 1) % CyclePhase.values().length], players);
        } else if (cycleIndex >= 0 && --waveTicks <= 0) {
            CyclePhase phase = cyclePhase();
            waveTicks = phase == CyclePhase.RESONANCE ? RING_WAVE_TICKS : MARKER_WAVE_TICKS;
            runWave(phase, players);
        }
    }

    // ---- Moves ----

    @Override
    public boolean doHurtTarget(Entity target) {
        return !isBusy() && super.doHurtTarget(target);
    }

    public void beginMove(int newMove) {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        move = newMove;
        getNavigation().stop();
        switch (newMove) {
            case MOVE_LANCES -> {
                moveTicks = LANCE_WINDUP;
                lanceSide = random.nextBoolean() ? 1 : -1;
                LivingEntity target = getTarget();
                lanceAngle = target != null ? BossCombat.angleTo(position(), target.position()) : Math.toRadians(yBodyRot + 90.0);
                Telegraph.windUp(this, ParticleTypes.ELECTRIC_SPARK, BossesC.PRIME_LANCES.get(), 1.0F);
            }
            case MOVE_VOLLEY -> {
                moveTicks = VOLLEY_WINDUP;
                Telegraph.windUp(this, ParticleTypes.ENCHANT, BossesC.PRIME_VOLLEY.get(), 1.2F);
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
        if (move == MOVE_LANCES) {
            Vec3 from = position().add(0.0, 0.3, 0.0);
            for (int i = 0; i < LANCE_COUNT; i++) {
                BossCombat.drawLine(server, ParticleTypes.ELECTRIC_SPARK, from, lanceAngle + i * lanceSide * LANCE_SPREAD, LANCE_LENGTH);
            }
        } else if (move == MOVE_VOLLEY) {
            server.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 2.4, getZ(), 16, 0.8, 0.6, 0.8, 0.3);
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
                    cooldown = cooldownTicks();
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
        if (finished == MOVE_LANCES) {
            resolveLances(server);
        } else if (finished == MOVE_VOLLEY) {
            fireVolley(server);
        }
    }

    private void resolveLances(ServerLevel server) {
        Vec3 from = position().add(0.0, 0.3, 0.0);
        Set<LivingEntity> hit = new LinkedHashSet<>();
        for (int i = 0; i < LANCE_COUNT; i++) {
            double angle = lanceAngle + i * lanceSide * LANCE_SPREAD;
            BossCombat.drawLine(server, ParticleTypes.CRIT, from, angle, LANCE_LENGTH);
            hit.addAll(BossCombat.alongLine(this, from, angle, LANCE_LENGTH, LANCE_WIDTH));
        }
        for (LivingEntity victim : hit) {
            victim.hurt(damageSources().indirectMagic(this, this), scaledDamage(LANCE_DAMAGE));
        }
    }

    private void fireVolley(ServerLevel server) {
        LivingEntity target = getTarget();
        Vec3 origin = position().add(0.0, 2.2, 0.0);
        double base = target != null ? BossCombat.angleTo(position(), target.position()) : Math.toRadians(yBodyRot + 90.0);
        double dy = 0.0;
        if (target != null) {
            double horizontal = Math.max(1.0, Math.sqrt((target.getX() - getX()) * (target.getX() - getX()) + (target.getZ() - getZ()) * (target.getZ() - getZ())));
            dy = (target.getY(0.5) - origin.y) / horizontal;
        }
        for (int i = 0; i < VOLLEY_PAGES; i++) {
            double angle = base + (i - (VOLLEY_PAGES - 1) / 2.0) * VOLLEY_SPREAD;
            Vec3 vel = new Vec3(Math.cos(angle), dy, Math.sin(angle)).normalize().scale(PAGE_SPEED);
            IndexPage page = IndexPage.create(server, this, origin.add(vel.normalize().scale(1.2)), vel, scaledDamage(IndexPage.BASE_DAMAGE));
            server.addFreshEntity(page);
        }
    }

    private void chooseMove() {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || distanceToSqr(target) > 30.0 * 30.0) {
            return;
        }
        beginMove(distanceToSqr(target) <= 9.0 * 9.0 && random.nextFloat() < 0.5F ? MOVE_LANCES : (random.nextBoolean() ? MOVE_VOLLEY : MOVE_LANCES));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!isScaled() || !isAlive() || !(level() instanceof ServerLevel server)) {
            return;
        }
        updateStage();
        entityData.set(DATA_SEALED, !livingNodes().isEmpty());
        List<ServerPlayer> players = arenaPlayers();
        if (phase() >= 2) {
            tickCycle(players);
            tickRings();
        }
        if (phase() >= 3) {
            if (tickCount % 10 == 0) {
                drawAuraDome(server);
            }
            if (tickCount % 20 == 0) {
                applyAura();
            }
            if (isSealed() && tickCount % 5 == 0) {
                for (int i = 0; i < 6; i++) {
                    double a = tickCount * 0.15 + i * Math.PI / 3.0;
                    server.sendParticles(ParticleTypes.ENCHANT, getX() + Math.cos(a) * 1.4, getY() + 1.2 + (i % 3) * 0.7, getZ() + Math.sin(a) * 1.4,
                            1, 0.0, 0.0, 0.0, 0.0);
                }
            }
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

    @Override
    protected void onArenaReset() {
        discardNodes();
        rings.clear();
        finalSealRaised = false;
        cycleIndex = -1;
        cycleTicks = 0;
        waveTicks = 0;
        move = MOVE_NONE;
        moveTicks = 0;
        recover = 0;
        cooldown = BASE_COOLDOWN_TICKS;
        entityData.set(DATA_SEALED, false);
        entityData.set(DATA_PROGRESS, 0.0F);
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) {
            discardNodes();
            rings.clear();
        }
        super.die(source);
    }

    // ---- NBT ----

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ListTag nodeTags = new ListTag();
        for (UUID id : nodes) {
            nodeTags.add(NbtUtils.createUUID(id));
        }
        tag.put("IndexNodes", nodeTags);
        ListTag ringTags = new ListTag();
        for (Ring ring : rings) {
            CompoundTag r = new CompoundTag();
            r.putDouble("X", ring.center.x);
            r.putDouble("Y", ring.center.y);
            r.putDouble("Z", ring.center.z);
            r.putInt("T", ring.ticks);
            ringTags.add(r);
        }
        tag.put("Rings", ringTags);
        tag.putInt("Move", move);
        tag.putInt("MoveTicks", moveTicks);
        tag.putInt("MoveTotal", moveTotal);
        tag.putInt("Recover", recover);
        tag.putInt("Cooldown", cooldown);
        tag.putDouble("LanceAngle", lanceAngle);
        tag.putInt("LanceSide", lanceSide);
        tag.putInt("CycleIndex", cycleIndex);
        tag.putInt("CycleTicks", cycleTicks);
        tag.putInt("WaveTicks", waveTicks);
        tag.putBoolean("FinalSeal", finalSealRaised);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        nodes.clear();
        for (Tag t : tag.getList("IndexNodes", Tag.TAG_INT_ARRAY)) {
            nodes.add(NbtUtils.loadUUID(t));
        }
        rings.clear();
        for (Tag t : tag.getList("Rings", Tag.TAG_COMPOUND)) {
            CompoundTag r = (CompoundTag) t;
            rings.add(new Ring(new Vec3(r.getDouble("X"), r.getDouble("Y"), r.getDouble("Z")), r.getInt("T")));
        }
        move = tag.getInt("Move");
        moveTicks = tag.getInt("MoveTicks");
        moveTotal = Math.max(1, tag.getInt("MoveTotal"));
        recover = tag.getInt("Recover");
        cooldown = tag.getInt("Cooldown");
        lanceAngle = tag.getDouble("LanceAngle");
        lanceSide = tag.contains("LanceSide") && tag.getInt("LanceSide") < 0 ? -1 : 1;
        cycleIndex = tag.contains("CycleIndex") ? tag.getInt("CycleIndex") : -1;
        cycleTicks = tag.getInt("CycleTicks");
        waveTicks = tag.getInt("WaveTicks");
        finalSealRaised = tag.getBoolean("FinalSeal");
    }
}
