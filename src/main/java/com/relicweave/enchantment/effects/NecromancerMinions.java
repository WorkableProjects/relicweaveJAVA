package com.relicweave.enchantment.effects;

import com.relicweave.RelicWeave;
import com.relicweave.registry.ModEnchantments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


/**
 * The Necromancer: kills made with the weapon may raise a short-lived Risen Minion (a Husk) that fights hostile mobs.
 * <p>
 * Minions are tagged in persistent data. They never persist across saves (their custom goals would reset on reload and
 * turn them hostile), never target players or each other, and drop nothing.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class NecromancerMinions {
    public static final int MINION_LIFETIME_TICKS = 600;
    public static final float CHANCE_PER_LEVEL = 0.15F;
    private static final String TAG_MINION = "relicweave:minion";
    private static final String TAG_OWNER = "relicweave:minion_owner";
    private static final String TAG_EXPIRES = "relicweave:minion_expires";

    private NecromancerMinions() {}

    public static boolean isMinion(Entity entity) {
        return entity.getPersistentData().getBoolean(TAG_MINION);
    }

    /** Spawns a Risen Minion at {@code pos} owned by {@code owner}, or returns null if it could not be added. */
    public static Husk spawnMinion(ServerLevel level, Vec3 pos, Player owner) {
        Husk husk = EntityType.HUSK.create(level);
        if (husk == null) {
            return null;
        }
        husk.moveTo(pos.x, pos.y, pos.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
        husk.setBaby(false);
        husk.setCustomName(Component.translatable("entity.relicweave.risen_minion"));
        husk.setCustomNameVisible(true);
        husk.getPersistentData().putBoolean(TAG_MINION, true);
        husk.getPersistentData().putUUID(TAG_OWNER, owner.getUUID());
        husk.getPersistentData().putLong(TAG_EXPIRES, level.getGameTime() + MINION_LIFETIME_TICKS);

        husk.targetSelector.removeAllGoals(goal -> true);
        husk.targetSelector.addGoal(1, new HurtByTargetGoal(husk));
        husk.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(husk, Mob.class, 10, true, false,
                target -> target instanceof Enemy && !isMinion(target)));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            husk.setDropChance(slot, 0.0F);
        }
        return level.addFreshEntity(husk) ? husk : null;
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof Player killer) || source.getDirectEntity() != killer
                || !(killer.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack weapon = killer.getMainHandItem();
        Enchantment necromancer = ModEnchantments.THE_NECROMANCER.get();
        int enchantLevel = EnchantmentHelper.getItemEnchantmentLevel(necromancer, weapon);
        var victim = event.getEntity();
        if (enchantLevel <= 0 || !necromancer.canEnchant(weapon) || !(victim instanceof Enemy)
                || victim.getType().is(Tags.EntityTypes.BOSSES) || isMinion(victim)) {
            return;
        }
        if (level.getRandom().nextFloat() < CHANCE_PER_LEVEL * enchantLevel) {
            spawnMinion(level, victim.position(), killer);
        }
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (isMinion(event.getEntity())
                && (event.getNewTarget() instanceof Player || (event.getNewTarget() != null && isMinion(event.getNewTarget())))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        var entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || !isMinion(entity)) {
            return;
        }
        if (level.getGameTime() >= entity.getPersistentData().getLong(TAG_EXPIRES)) {
            level.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + 1.0, entity.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
            entity.discard();
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() && isMinion(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (isMinion(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExperience(LivingExperienceDropEvent event) {
        if (isMinion(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
