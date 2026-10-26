package com.relicweave.vestige.entity.humanoid;

import com.relicweave.vestige.registry.VestigeMaterials;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Neutral wanderer. It never attacks unless hurt (then it is angry at the attacker for 30 seconds). Handing it an
 * Inscribed Page makes it share one of eight lore lines, at most once per five minutes per drifter.
 */
public class VestigeDrifter extends PathfinderMob implements NeutralMob {
    public static final int LORE_LINES = 8;
    public static final int LORE_COOLDOWN_TICKS = 5 * 60 * 20;
    private static final UniformInt ANGER_TIME = TimeUtil.rangeOfSeconds(30, 30);

    private int angerTime;
    @Nullable
    private UUID angerTarget;
    /** Game time at which the next lore line may be shared. */
    private long loreReadyTime;

    public VestigeDrifter(EntityType<? extends VestigeDrifter> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    // ---- NeutralMob ----

    @Override
    public int getRemainingPersistentAngerTime() {
        return angerTime;
    }

    @Override
    public void setRemainingPersistentAngerTime(int time) {
        angerTime = time;
    }

    @Nullable
    @Override
    public UUID getPersistentAngerTarget() {
        return angerTarget;
    }

    @Override
    public void setPersistentAngerTarget(@Nullable UUID target) {
        angerTarget = target;
    }

    @Override
    public void startPersistentAngerTimer() {
        setRemainingPersistentAngerTime(ANGER_TIME.sample(random));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && source.getEntity() instanceof LivingEntity attacker && attacker.isAlive()
                && !(attacker instanceof Player player && (player.isCreative() || player.isSpectator()))) {
            setPersistentAngerTarget(attacker.getUUID());
            startPersistentAngerTimer();
            setTarget(attacker);
        }
        return hurt;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel server) {
            updatePersistentAnger(server, true);
        }
    }

    // ---- Lore ----

    public long loreReadyTime() {
        return loreReadyTime;
    }

    public void setLoreReadyTime(long gameTime) {
        loreReadyTime = gameTime;
    }

    public boolean canShareLore() {
        return level().getGameTime() >= loreReadyTime;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(VestigeMaterials.INSCRIBED_PAGE.get())) {
            return super.mobInteract(player, hand);
        }
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!canShareLore()) {
            player.sendSystemMessage(Component.translatable("message.relicweave.drifter.silent"));
            return InteractionResult.CONSUME;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        loreReadyTime = level().getGameTime() + LORE_COOLDOWN_TICKS;
        player.sendSystemMessage(Component.translatable("message.relicweave.drifter.lore." + random.nextInt(LORE_LINES)));
        if (level() instanceof ServerLevel server) {
            MobCombat.burst(server, ParticleTypes.ENCHANT, this, 10);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        addPersistentAngerSaveData(tag);
        tag.putLong("LoreReadyTime", loreReadyTime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (level() instanceof ServerLevel server) {
            readPersistentAngerSaveData(server, tag);
        }
        loreReadyTime = tag.getLong("LoreReadyTime");
    }
}
