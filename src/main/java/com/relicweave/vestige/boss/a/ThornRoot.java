package com.relicweave.vestige.boss.a;

import com.relicweave.vestige.boss.VestigeBoss;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One segment of the Thorned Regent's Root Wall: solid to players (and everything that collides with entities), cannot
 * be hurt, deals {@value #CONTACT_DAMAGE} damage to a player touching it at most once per second, withers away after
 * {@value #LIFETIME_TICKS} ticks. The wall id lets the owner count how many walls are standing.
 */
public class ThornRoot extends Entity {
    public static final int LIFETIME_TICKS = 200;
    public static final float CONTACT_DAMAGE = 3.0F;
    public static final int CONTACT_COOLDOWN = 20;

    private int age;
    @Nullable
    private UUID owner;
    private int wallId;
    private final Map<UUID, Integer> nextHit = new HashMap<>();

    public ThornRoot(EntityType<? extends ThornRoot> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        setNoGravity(true);
    }

    public void setOwner(@Nullable UUID owner, int wallId) {
        this.owner = owner;
        this.wallId = wallId;
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public int wallId() {
        return wallId;
    }

    public int age() {
        return age;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        age++;
        if (age >= LIFETIME_TICKS) {
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.COMPOSTER, getX(), getY() + 1.0, getZ(), 8, 0.4, 0.8, 0.4, 0.02);
            }
            discard();
            return;
        }
        for (Player player : BossACombat.victims(level(), getBoundingBox().inflate(0.25, 0.1, 0.25))) {
            if (age >= nextHit.getOrDefault(player.getUUID(), 0)) {
                nextHit.put(player.getUUID(), age + CONTACT_COOLDOWN);
                player.hurt(damageSources().cactus(), (float) (CONTACT_DAMAGE * VestigeBoss.damageMultiplier()));
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        age = tag.getInt("Age");
        wallId = tag.getInt("Wall");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", age);
        tag.putInt("Wall", wallId);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }
}
