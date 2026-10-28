package com.relicweave.gametest;

import com.mojang.authlib.GameProfile;
import com.relicweave.RelicWeave;
import com.relicweave.infusion.InfusionType;
import com.relicweave.infusion.Infusions;
import com.relicweave.infusion.effects.vestige.CycleweaveEffect;
import com.relicweave.infusion.effects.vestige.RiftwardEffect;
import com.relicweave.infusion.effects.vestige.SealguardEffect;
import com.relicweave.registry.ModItems;
import com.relicweave.vestige.hazard.HazardProtection;
import com.relicweave.vestige.hazard.field.StarfallImpacts;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Server-side tests for the four 0.0.4 Vestige effect infusions. Run with {@code ./gradlew runGameTestServer}. */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VestigeInfusionGameTests {
    private static final String TEMPLATE = "platform";
    private static final String SEALGUARD_TAG = RelicWeave.MOD_ID + ":sealguard";

    private VestigeInfusionGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void vestigeInfusionsOnlyFitTheirPiece(GameTestHelper helper) {
        check(helper, InfusionType.RIFTWARD, EquipmentSlot.HEAD);
        check(helper, InfusionType.SEALGUARD, EquipmentSlot.CHEST);
        check(helper, InfusionType.CYCLEWEAVE, EquipmentSlot.LEGS);
        check(helper, InfusionType.GRAVITIC_TREAD, EquipmentSlot.FEET);
        helper.succeed();
    }

    private static void check(GameTestHelper helper, InfusionType type, EquipmentSlot slot) {
        helper.assertFalse(type.hasAttribute(), type + " is an effect infusion");
        for (Item piece : new Item[]{ModItems.CELESTIAL_HELMET.get(), ModItems.CELESTIAL_CHESTPLATE.get(),
                ModItems.CELESTIAL_LEGGINGS.get(), ModItems.CELESTIAL_BOOTS.get()}) {
            boolean fits = ((net.minecraft.world.item.ArmorItem) piece).getEquipmentSlot() == slot;
            helper.assertTrue(Infusions.canInfuse(new ItemStack(piece), type) == fits, type + " on " + piece + " should " + (fits ? "" : "not ") + "fit");
        }
    }

    @GameTest(template = TEMPLATE)
    public static void threeInfusionLimitUnchanged(GameTestHelper helper) {
        ItemStack boots = new ItemStack(ModItems.CELESTIAL_BOOTS.get());
        boots = Infusions.withInfusion(boots, InfusionType.COBBLED);
        boots = Infusions.withInfusion(boots, InfusionType.OXIDIZED);
        helper.assertTrue(Infusions.canInfuse(boots, InfusionType.GRAVITIC_TREAD), "Third infusion accepted");
        boots = Infusions.withInfusion(boots, InfusionType.GRAVITIC_TREAD);
        helper.assertTrue(Infusions.check(boots, InfusionType.CONDUCTIVE) == Infusions.Result.LIMIT_REACHED, "A fourth infusion is refused");
        helper.assertTrue(Infusions.MAX_INFUSIONS == 3, "Limit is 3");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void riftwardReducesRiftSaturationByThirtyPercent(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        helper.assertTrue(HazardProtection.reduction(player, HazardProtection.Hazard.RIFT_SATURATION) == 0.0, "No protection bare");
        wear(player, EquipmentSlot.HEAD, ModItems.CELESTIAL_HELMET.get(), InfusionType.RIFTWARD);
        double r = HazardProtection.reduction(player, HazardProtection.Hazard.RIFT_SATURATION);
        helper.assertTrue(Math.abs(r - 0.30) < 1.0E-9 && RiftwardEffect.RIFT_PROTECTION == 0.30, "Riftward gives 0.30, got " + r);
        helper.assertTrue(HazardProtection.reduction(player, HazardProtection.Hazard.GRAVITY_SHEAR) == 0.0, "Other hazards unaffected");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void sealguardBlocksExactlyOneStarfallThenRecharges(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        wear(player, EquipmentSlot.CHEST, ModItems.CELESTIAL_CHESTPLATE.get(), InfusionType.SEALGUARD);
        helper.assertTrue(attack(player, starfall(player)), "First starfall hit is cancelled");
        helper.assertFalse(attack(player, starfall(player)), "Second starfall hit is not cancelled");
        helper.assertFalse(attack(player, player.damageSources().generic()), "Other damage is never cancelled");
        helper.assertFalse(SealguardEffect.isReady(player), "Recharging");

        // Recharge: the deadline lives in persistent data; move it into the past.
        CompoundTag tag = player.getPersistentData().getCompound(SEALGUARD_TAG);
        helper.assertTrue(tag.getLong("ready_at") == player.level().getGameTime() + SealguardEffect.RECHARGE_TICKS, "Recharge is 900 ticks");
        tag.putLong("ready_at", player.level().getGameTime() - 1);
        player.getPersistentData().put(SEALGUARD_TAG, tag);
        helper.assertTrue(SealguardEffect.isReady(player), "Ready again after the deadline");
        helper.assertTrue(attack(player, starfall(player)), "Blocks again after recharging");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void sealguardNeedsTheInfusion(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        helper.assertFalse(attack(player, starfall(player)), "Nothing to cancel without Sealguard");
        helper.assertTrue(SealguardEffect.isReady(player), "No cooldown was started");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cycleweaveGrantsBurstWithPersistedCooldown(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        helper.assertFalse(CycleweaveEffect.trigger(player), "Nothing without the infusion");
        wear(player, EquipmentSlot.LEGS, ModItems.CELESTIAL_LEGGINGS.get(), InfusionType.CYCLEWEAVE);
        helper.assertTrue(CycleweaveEffect.trigger(player), "First phase change triggers");
        helper.assertTrue(player.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier() == 1, "Speed II");
        helper.assertTrue(player.getEffect(MobEffects.JUMP).getAmplifier() == 0, "Jump Boost I");
        helper.assertTrue(player.getEffect(MobEffects.MOVEMENT_SPEED).getDuration() <= CycleweaveEffect.DURATION_TICKS, "200 ticks at most");
        helper.assertFalse(CycleweaveEffect.trigger(player), "Cooldown blocks the next phase change");

        // The cooldown survives a save/load round trip of the player data.
        CompoundTag saved = player.getPersistentData().copy();
        ServerPlayer other = makePlayer(helper);
        wear(other, EquipmentSlot.LEGS, ModItems.CELESTIAL_LEGGINGS.get(), InfusionType.CYCLEWEAVE);
        for (String key : saved.getAllKeys()) {
            other.getPersistentData().put(key, saved.get(key));
        }
        helper.assertFalse(CycleweaveEffect.trigger(other), "Restored cooldown still applies");
        helper.succeed();
    }

    private static boolean attack(ServerPlayer player, DamageSource source) {
        LivingAttackEvent event = new LivingAttackEvent(player, source, 6.0F);
        MinecraftForge.EVENT_BUS.post(event);
        return event.isCanceled();
    }

    private static DamageSource starfall(ServerPlayer player) {
        return new DamageSource(player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(StarfallImpacts.DAMAGE_TYPE));
    }

    private static void wear(ServerPlayer player, EquipmentSlot slot, Item armor, InfusionType infusion) {
        player.setItemSlot(slot, Infusions.withInfusion(new ItemStack(armor), infusion));
    }

    private static ServerPlayer makePlayer(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "relicweave-vestige-infusion-test"));
        player.setGameMode(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        return player;
    }
}
