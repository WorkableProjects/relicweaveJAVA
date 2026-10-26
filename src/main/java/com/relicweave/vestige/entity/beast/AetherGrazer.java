package com.relicweave.vestige.entity.beast;

import com.relicweave.vestige.registry.VestigeMaterials;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Neutral wildlife. Flees when hurt or when a sprinting player gets close. Its fleece can be sheared for Aether Down
 * and regrows over {@link #REGROW_TICKS}. It cannot breed and has no food, so natural spawns are the only supply.
 */
public class AetherGrazer extends PathfinderMob {
    public static final int REGROW_TICKS = 6000;

    private static final EntityDataAccessor<Integer> DATA_REGROW = SynchedEntityData.defineId(AetherGrazer.class, EntityDataSerializers.INT);

    private int regrowTicks;
    private int panicTicks;
    private boolean spooked;

    public AetherGrazer(EntityType<? extends AetherGrazer> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_REGROW, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        this.goalSelector.addGoal(2, new SprinterAvoidGoal());
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public boolean isShearable() {
        return isAlive() && regrowTicks <= 0;
    }

    public int getRegrowTicks() {
        return regrowTicks;
    }

    public void setRegrowTicks(int ticks) {
        this.regrowTicks = Math.max(0, ticks);
        this.entityData.set(DATA_REGROW, this.regrowTicks);
    }

    /** Client: 1.0 for a full fleece, down to 0.4 right after shearing. */
    public float fleeceScale() {
        int regrow = this.entityData.get(DATA_REGROW);
        return regrow <= 0 ? 1.0F : 0.4F + 0.6F * (1.0F - Math.min(1.0F, regrow / (float) REGROW_TICKS));
    }

    /** Shear the fleece: returns the drops (1-2 Aether Down) and starts the regrowth, or nothing if bare. */
    public List<ItemStack> shearFleece() {
        List<ItemStack> drops = new ArrayList<>();
        if (!isShearable()) {
            return drops;
        }
        drops.add(new ItemStack(VestigeMaterials.AETHER_DOWN.get(), 1 + random.nextInt(2)));
        setRegrowTicks(REGROW_TICKS);
        return drops;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.SHEARS)) {
            if (!level().isClientSide && isShearable()) {
                level().playSound(null, this, SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.2F);
                for (ItemStack drop : shearFleece()) {
                    spawnAtLocation(drop, 1.0F);
                }
                stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
                return InteractionResult.SUCCESS;
            }
            return level().isClientSide && isShearable() ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && !level().isClientSide) {
            panicTicks = 60;
        }
        return result;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (regrowTicks > 0) {
            regrowTicks--;
            if (regrowTicks % 40 == 0 || regrowTicks == 0) {
                this.entityData.set(DATA_REGROW, regrowTicks);
            }
        }
        if (panicTicks > 0) {
            panicTicks--;
        }
        if ((panicTicks > 0 || spooked) && tickCount % 4 == 0 && level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 2, 0.4, 0.1, 0.4, 0.01);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("RegrowTicks", regrowTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setRegrowTicks(tag.getInt("RegrowTicks"));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SHEEP_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SHEEP_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SHEEP_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.7F;
    }

    private final class SprinterAvoidGoal extends AvoidEntityGoal<Player> {
        SprinterAvoidGoal() {
            super(AetherGrazer.this, Player.class, e -> e.isSprinting(), 6.0F, 1.2, 1.6, e -> EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(e));
        }

        @Override
        public void start() {
            super.start();
            spooked = true;
        }

        @Override
        public void stop() {
            super.stop();
            spooked = false;
        }
    }
}
