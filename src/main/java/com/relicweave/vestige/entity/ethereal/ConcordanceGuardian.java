package com.relicweave.vestige.entity.ethereal;

import com.relicweave.vestige.entity.Telegraph;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Structure mini-boss that teaches boss mechanics. Phase 1 (above 60% health): melee and a ground Shockwave. Phase 2
 * (60% to 30%): adds a Rune Beam along a line fixed at wind-up start. Phase 3 (below 30%): moves 30% faster and winds
 * up 25% quicker (never below 12 ticks). Phase changes are announced in the action bar.
 */
public class ConcordanceGuardian extends MiniBossMob {
    static final int SHOCKWAVE = 0;
    static final int BEAM = 1;
    public static final double SHOCK_RADIUS = 4.0;
    public static final float BEAM_DAMAGE = 12.0F;
    public static final double BEAM_WIDTH = 1.0;
    public static final double BEAM_LENGTH = 24.0;
    private static final UUID RAGE_SPEED = UUID.fromString("5d1c8c0e-6a1e-4c71-9a55-3c2a6f7a0b01");

    private int announcedPhase = 1;

    public ConcordanceGuardian(EntityType<? extends ConcordanceGuardian> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
    }

    /** 1 above 60% health, 2 from 60% down to 30%, 3 below 30%. */
    public static int phaseFor(float healthRatio) {
        if (healthRatio > 0.6F) {
            return 1;
        }
        return healthRatio >= 0.3F ? 2 : 3;
    }

    /** Wind-up length after phase scaling: x0.75 in phase 3, never below 12 ticks. */
    public static int windupFor(int base, int phase) {
        return phase >= 3 ? Math.max(12, Math.round(base * 0.75F)) : base;
    }

    public int phase() {
        return phaseFor(getHealth() / getMaxHealth());
    }

    int announcedPhase() {
        return announcedPhase;
    }

    @Override
    protected void customServerAiStep() {
        int phase = phase();
        if (phase > announcedPhase) {
            announcedPhase = phase;
            announce(phase);
        }
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            boolean has = speed.getModifier(RAGE_SPEED) != null;
            if (phase >= 3 && !has) {
                speed.addTransientModifier(new AttributeModifier(RAGE_SPEED, "guardian_rage", 0.3, AttributeModifier.Operation.MULTIPLY_BASE));
            } else if (phase < 3 && has) {
                speed.removeModifier(RAGE_SPEED);
            }
        }
        super.customServerAiStep();
    }

    private void announce(int phase) {
        if (phase < 2 || !(level() instanceof ServerLevel server)) {
            return;
        }
        Component message = Component.translatable("message.relicweave.guardian.phase" + phase);
        for (ServerPlayer player : server.players()) {
            if (!player.isSpectator() && player.distanceToSqr(this) <= MiniBossSupport.RANGE * MiniBossSupport.RANGE) {
                player.displayClientMessage(message, true);
            }
        }
    }

    @Override
    protected void onBossReset() {
        super.onBossReset();
        announcedPhase = 1;
    }

    @Override
    protected void chooseAttack(LivingEntity target) {
        int phase = phase();
        double flat = horizontalDistance(position(), target.position());
        if (flat <= SHOCK_RADIUS) {
            beginAttack(SHOCKWAVE, windupFor(20, phase));
            Telegraph.windUp(this, ParticleTypes.CRIT, EtherealMobs.GUARDIAN_SHOCKWAVE.get(), 0.8F);
            Telegraph.ring(serverLevel(), ParticleTypes.CRIT, position(), SHOCK_RADIUS, 24);
        } else if (phase >= 2 && flat <= BEAM_LENGTH && hasLineOfSight(target)) {
            aim = target.position().add(0.0, 1.0, 0.0);
            beginAttack(BEAM, windupFor(25, phase));
            Telegraph.windUp(this, ParticleTypes.END_ROD, EtherealMobs.GUARDIAN_RUNE_BEAM.get(), 1.0F);
            drawBeam(ParticleTypes.END_ROD);
        }
    }

    private Vec3 beamOrigin() {
        return position().add(0.0, 2.0, 0.0);
    }

    private Vec3 beamEnd() {
        Vec3 dir = aim.subtract(beamOrigin());
        return dir.lengthSqr() < 1.0E-6 ? beamOrigin() : beamOrigin().add(dir.normalize().scale(BEAM_LENGTH));
    }

    private void drawBeam(net.minecraft.core.particles.SimpleParticleType particle) {
        Telegraph.line(serverLevel(), particle, beamOrigin(), beamEnd(), 24);
    }

    @Override
    protected void onWindupTick(int id, int remaining) {
        if (remaining % 5 != 0) {
            return;
        }
        if (id == SHOCKWAVE) {
            Telegraph.ring(serverLevel(), ParticleTypes.CRIT, position(), SHOCK_RADIUS, 24);
        } else if (id == BEAM) {
            drawBeam(ParticleTypes.END_ROD);
        }
    }

    @Override
    protected void resolveAttack(int id) {
        ServerLevel server = serverLevel();
        attackCooldown = phase() >= 3 ? 35 : 50;
        if (id == SHOCKWAVE) {
            float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
            for (Player player : playersWithin(position(), SHOCK_RADIUS)) {
                if (player.onGround()) {
                    player.hurt(damageSources().mobAttack(this), damage);
                }
            }
            Telegraph.ring(server, ParticleTypes.CLOUD, position(), SHOCK_RADIUS, 32);
            Telegraph.ring(server, ParticleTypes.CLOUD, position(), SHOCK_RADIUS * 0.5, 16);
        } else if (id == BEAM) {
            for (Player player : playersOnLine(beamOrigin(), beamEnd(), BEAM_WIDTH)) {
                player.hurt(damageSources().mobAttack(this), BEAM_DAMAGE);
            }
            Telegraph.line(server, ParticleTypes.CRIT, beamOrigin(), beamEnd(), 40);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Phase", announcedPhase);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        announcedPhase = Math.max(1, tag.getInt("Phase"));
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 1.0F, 0.7F);
    }

    @Override
    public float getVoicePitch() {
        return 0.6F;
    }
}
