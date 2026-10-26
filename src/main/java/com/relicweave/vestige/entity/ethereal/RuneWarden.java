package com.relicweave.vestige.entity.ethereal;

import com.relicweave.vestige.entity.Telegraph;
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
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Structure champion that spawns dormant (statue pose, no AI, invulnerable). A player right-clicking it, or standing
 * within {@link #WAKE_RADIUS} blocks for {@link #WAKE_TICKS}, starts a {@link #AWAKEN_TICKS}-tick rune sequence; then
 * it fights with Rune Burst (ring) and Rune Volley (three {@link RuneShard}s).
 */
public class RuneWarden extends MiniBossMob {
    private static final EntityDataAccessor<Boolean> DATA_DORMANT = SynchedEntityData.defineId(RuneWarden.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_AWAKEN = SynchedEntityData.defineId(RuneWarden.class, EntityDataSerializers.INT);
    static final int BURST = 0;
    static final int VOLLEY = 1;
    public static final double WAKE_RADIUS = 4.0;
    public static final int WAKE_TICKS = 60;
    public static final int AWAKEN_TICKS = 60;
    public static final double BURST_RADIUS = 3.5;
    public static final float BURST_DAMAGE = 10.0F;

    private int nearTicks;

    public RuneWarden(EntityType<? extends RuneWarden> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_DORMANT, true);
        this.entityData.define(DATA_AWAKEN, 0);
    }

    /** True from spawn until the rune sequence has finished (including while awakening). */
    public boolean isDormant() {
        return this.entityData.get(DATA_DORMANT);
    }

    public boolean isAwakening() {
        return isDormant() && this.entityData.get(DATA_AWAKEN) > 0;
    }

    /** Starts the 60-tick rune sequence (no effect unless dormant and not already awakening). */
    public void beginAwakening() {
        if (!isDormant() || isAwakening() || level().isClientSide) {
            return;
        }
        this.entityData.set(DATA_AWAKEN, AWAKEN_TICKS);
        ServerLevel server = serverLevel();
        server.playSound(null, getX(), getY(), getZ(), EtherealMobs.WARDEN_AWAKEN.get(), SoundSource.HOSTILE, 1.5F, 1.0F);
        for (ServerPlayer player : server.players()) {
            if (!player.isSpectator() && player.distanceToSqr(this) <= MiniBossSupport.RANGE * MiniBossSupport.RANGE) {
                player.displayClientMessage(Component.translatable("message.relicweave.rune_warden.awaken"), true);
            }
        }
    }

    @Override
    protected boolean isFrozen() {
        return isDormant();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return isDormant() || super.isInvulnerableTo(source);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !isDormant() && super.hurt(source, amount);
    }

    @Override
    public boolean isPushable() {
        return !isDormant() && super.isPushable();
    }

    @Override
    public void setTarget(@org.jetbrains.annotations.Nullable LivingEntity target) {
        super.setTarget(isDormant() ? null : target);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isDormant()) {
            if (!level().isClientSide && hand == InteractionHand.MAIN_HAND) {
                beginAwakening();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !isDormant()) {
            return;
        }
        int awaken = this.entityData.get(DATA_AWAKEN);
        if (awaken > 0) {
            awaken--;
            this.entityData.set(DATA_AWAKEN, awaken);
            if (awaken % 2 == 0) {
                drawRunes(awaken);
            }
            if (awaken == 0) {
                this.entityData.set(DATA_DORMANT, false);
                attackCooldown = 20;
            }
            return;
        }
        boolean near = !level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(WAKE_RADIUS, 1.0, WAKE_RADIUS),
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative() && p.distanceToSqr(this) <= WAKE_RADIUS * WAKE_RADIUS).isEmpty();
        nearTicks = near ? nearTicks + 1 : 0;
        if (nearTicks >= WAKE_TICKS) {
            nearTicks = 0;
            beginAwakening();
        }
    }

    private void drawRunes(int remaining) {
        ServerLevel server = serverLevel();
        double progress = 1.0 - remaining / (double) AWAKEN_TICKS;
        double y = getY() + progress * getBbHeight();
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3.0 + remaining * 0.3;
            server.sendParticles(i % 2 == 0 ? ParticleTypes.ENCHANT : ParticleTypes.END_ROD,
                    getX() + Math.cos(a) * 0.9, y, getZ() + Math.sin(a) * 0.9, 1, 0, 0.02, 0, 0.0);
        }
    }

    @Override
    protected void onBossReset() {
        super.onBossReset();
    }

    @Override
    protected void chooseAttack(LivingEntity target) {
        double flat = horizontalDistance(position(), target.position());
        if (flat <= BURST_RADIUS) {
            beginAttack(BURST, 20);
            Telegraph.windUp(this, ParticleTypes.ENCHANT, EtherealMobs.WARDEN_BURST.get(), 1.0F);
            Telegraph.ring(serverLevel(), ParticleTypes.ENCHANT, position(), BURST_RADIUS, 24);
        } else if (flat <= 18.0 && hasLineOfSight(target)) {
            aim = target.position().add(0.0, 1.0, 0.0);
            beginAttack(VOLLEY, 20);
            Telegraph.windUp(this, ParticleTypes.END_ROD, EtherealMobs.WARDEN_VOLLEY.get(), 1.2F);
        }
    }

    @Override
    protected void onWindupTick(int id, int remaining) {
        if (remaining % 5 == 0 && id == BURST) {
            Telegraph.ring(serverLevel(), ParticleTypes.ENCHANT, position(), BURST_RADIUS, 24);
        }
    }

    @Override
    protected void resolveAttack(int id) {
        attackCooldown = 50;
        if (id == BURST) {
            for (Player player : playersWithin(position(), BURST_RADIUS)) {
                player.hurt(damageSources().mobAttack(this), BURST_DAMAGE);
            }
            Telegraph.ring(serverLevel(), ParticleTypes.END_ROD, position(), BURST_RADIUS, 32);
        } else if (id == VOLLEY) {
            Vec3 origin = position().add(0.0, 1.8, 0.0);
            Vec3 dir = aim.subtract(origin);
            if (dir.lengthSqr() < 1.0E-6) {
                return;
            }
            dir = dir.normalize();
            for (int i = -1; i <= 1; i++) {
                double angle = Math.toRadians(10.0 * i);
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                Vec3 d = new Vec3(dir.x * cos - dir.z * sin, dir.y, dir.x * sin + dir.z * cos);
                RuneShard shard = new RuneShard(level(), this);
                shard.setPos(origin.x, origin.y, origin.z);
                shard.shoot(d.x, d.y, d.z, 1.2F, 0.0F);
                level().addFreshEntity(shard);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Dormant", isDormant());
        tag.putInt("AwakenTicks", this.entityData.get(DATA_AWAKEN));
        tag.putInt("NearTicks", nearTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_DORMANT, !tag.contains("Dormant") || tag.getBoolean("Dormant"));
        this.entityData.set(DATA_AWAKEN, tag.getInt("AwakenTicks"));
        nearTicks = tag.getInt("NearTicks");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.STONE_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.5F;
    }
}
