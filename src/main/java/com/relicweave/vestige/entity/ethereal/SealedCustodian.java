package com.relicweave.vestige.entity.ethereal;

import com.relicweave.vestige.entity.Telegraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Mini-boss that guards chests. It records every container within {@link #SEAL_RADIUS} blocks of its home; while it
 * lives {@link CustodianSeals} stops players opening or breaking them. Attacks: Seal Slam (ring, radius 3) and a
 * 3-second Rune Shield every 15 seconds (immune and unable to attack, never chained back-to-back).
 */
public class SealedCustodian extends MiniBossMob {
    static final int SLAM = 0;
    public static final int SEAL_RADIUS = 8;
    public static final double SLAM_RADIUS = 3.0;
    public static final float SLAM_DAMAGE = 9.0F;
    public static final int SHIELD_TICKS = 60;
    public static final int SHIELD_INTERVAL = 300;

    private final List<BlockPos> sealed = new ArrayList<>();
    private boolean recorded;
    private int shieldTicks;
    private int shieldCooldown = SHIELD_INTERVAL;

    public SealedCustodian(EntityType<? extends SealedCustodian> type, Level level) {
        super(type, level, BossEvent.BossBarColor.BLUE);
    }

    /** Scans the cube of {@link #SEAL_RADIUS} around home for container block entities and remembers them. */
    public void recordContainers() {
        if (level().isClientSide) {
            return;
        }
        BlockPos h = support.ensureHome();
        sealed.clear();
        for (BlockPos p : BlockPos.betweenClosed(h.offset(-SEAL_RADIUS, -SEAL_RADIUS, -SEAL_RADIUS),
                h.offset(SEAL_RADIUS, SEAL_RADIUS, SEAL_RADIUS))) {
            if (level().isLoaded(p) && level().getBlockEntity(p) instanceof BaseContainerBlockEntity) {
                sealed.add(p.immutable());
            }
        }
        recorded = true;
    }

    public boolean isSealing(BlockPos pos) {
        return isAlive() && sealed.contains(pos);
    }

    public int sealedCount() {
        return sealed.size();
    }

    public boolean isShielded() {
        return shieldTicks > 0;
    }

    public int shieldCooldown() {
        return shieldCooldown;
    }

    @Override
    protected boolean isFrozen() {
        return shieldTicks > 0;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (shieldTicks > 0 && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void customServerAiStep() {
        if (!recorded) {
            recordContainers();
        }
        if (shieldTicks > 0) {
            shieldTicks--;
            if (shieldTicks % 4 == 0) {
                drawDome();
            }
            if (shieldTicks == 0) {
                shieldCooldown = SHIELD_INTERVAL;
            }
        } else if (getTarget() != null && windup() == 0) {
            if (shieldCooldown > 0) {
                shieldCooldown--;
            } else {
                startShield();
            }
        }
        super.customServerAiStep();
    }

    private void startShield() {
        shieldTicks = SHIELD_TICKS;
        getNavigation().stop();
        Telegraph.windUp(this, ParticleTypes.ENCHANT, EtherealMobs.CUSTODIAN_SHIELD.get(), 1.0F);
    }

    private void drawDome() {
        ServerLevel server = serverLevel();
        for (int i = 0; i < 14; i++) {
            double theta = random.nextDouble() * Math.PI * 2.0;
            double phi = random.nextDouble() * Math.PI * 0.5;
            double r = 1.4;
            server.sendParticles(ParticleTypes.ENCHANT, getX() + Math.cos(theta) * Math.cos(phi) * r,
                    getY() + 0.2 + Math.sin(phi) * r * 1.4, getZ() + Math.sin(theta) * Math.cos(phi) * r, 1, 0, 0, 0, 0.0);
        }
    }

    @Override
    protected void onBossReset() {
        super.onBossReset();
        shieldTicks = 0;
        shieldCooldown = SHIELD_INTERVAL;
    }

    @Override
    protected void chooseAttack(LivingEntity target) {
        if (horizontalDistance(position(), target.position()) <= SLAM_RADIUS) {
            beginAttack(SLAM, 30);
            Telegraph.windUp(this, ParticleTypes.CRIT, EtherealMobs.CUSTODIAN_SLAM.get(), 0.9F);
            Telegraph.ring(serverLevel(), ParticleTypes.ENCHANT, position(), SLAM_RADIUS, 20);
        }
    }

    @Override
    protected void onWindupTick(int id, int remaining) {
        if (remaining % 5 == 0) {
            Telegraph.ring(serverLevel(), ParticleTypes.ENCHANT, position(), SLAM_RADIUS, 20);
        }
    }

    @Override
    protected void resolveAttack(int id) {
        attackCooldown = 60;
        for (Player player : playersWithin(position(), SLAM_RADIUS)) {
            player.hurt(damageSources().mobAttack(this), SLAM_DAMAGE);
        }
        Telegraph.ring(serverLevel(), ParticleTypes.CLOUD, position(), SLAM_RADIUS, 24);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Recorded", recorded);
        tag.putLongArray("Sealed", sealed.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putInt("ShieldTicks", shieldTicks);
        tag.putInt("ShieldCd", shieldCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        recorded = tag.getBoolean("Recorded");
        sealed.clear();
        for (long l : tag.getLongArray("Sealed")) {
            sealed.add(BlockPos.of(l));
        }
        shieldTicks = tag.getInt("ShieldTicks");
        shieldCooldown = tag.contains("ShieldCd") ? tag.getInt("ShieldCd") : SHIELD_INTERVAL;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.8F;
    }
}
