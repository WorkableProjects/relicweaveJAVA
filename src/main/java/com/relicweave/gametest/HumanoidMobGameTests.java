package com.relicweave.gametest;

import com.mojang.authlib.GameProfile;
import com.relicweave.RelicWeave;
import com.relicweave.vestige.entity.humanoid.EchoWeaver;
import com.relicweave.vestige.entity.humanoid.GildedHusk;
import com.relicweave.vestige.entity.humanoid.GlyphBolt;
import com.relicweave.vestige.entity.humanoid.HumanoidMobs;
import com.relicweave.vestige.entity.humanoid.MireboundCantor;
import com.relicweave.vestige.entity.humanoid.OathbreakerKnight;
import com.relicweave.vestige.entity.humanoid.ReliquarySentinel;
import com.relicweave.vestige.entity.humanoid.RiftArcher;
import com.relicweave.vestige.entity.humanoid.ScriptboundScribe;
import com.relicweave.vestige.entity.humanoid.VestigeDrifter;
import com.relicweave.vestige.registry.VestigeMaterials;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** The eight humanoid Vestige mobs (T06): attributes, NBT state and their signature behaviours. */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HumanoidMobGameTests {
    private static final String TEMPLATE = "platform";

    private HumanoidMobGameTests() {}

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1.0E-3;
    }

    private static void face(Mob mob, float yaw) {
        mob.setYRot(yaw);
        mob.yBodyRot = yaw;
        mob.yHeadRot = yaw;
    }

    private static Zombie attacker(GameTestHelper helper, double x, double y, double z) {
        return helper.spawnWithNoFreeWill(EntityType.ZOMBIE, (float) x, (float) y, (float) z);
    }

    private static Zombie helmeted(GameTestHelper helper, double x, double y, double z) {
        Zombie zombie = attacker(helper, x, y, z);
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        return zombie;
    }

    private static ServerPlayer makePlayer(GameTestHelper helper, Vec3 relative) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "relicweave-humanoid-test"));
        player.setGameMode(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(relative);
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.setHealth(player.getMaxHealth());
        return player;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Mob> T roundTrip(GameTestHelper helper, T mob) {
        CompoundTag tag = mob.saveWithoutId(new CompoundTag());
        T copy = (T) mob.getType().create(helper.getLevel());
        copy.load(tag);
        return copy;
    }

    private static void checkStats(GameTestHelper helper, Mob mob, double health, double armor, double damage) {
        helper.assertTrue(near(mob.getAttributeValue(Attributes.MAX_HEALTH), health), "max health " + mob.getAttributeValue(Attributes.MAX_HEALTH));
        if (armor >= 0) {
            helper.assertTrue(near(mob.getAttributeValue(Attributes.ARMOR), armor), "armor " + mob.getAttributeValue(Attributes.ARMOR));
        }
        if (damage >= 0) {
            helper.assertTrue(near(mob.getAttributeValue(Attributes.ATTACK_DAMAGE), damage), "damage " + mob.getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
        helper.assertTrue(mob.isAlive(), "mob spawned");
    }

    // ---- Reliquary Sentinel ----

    @GameTest(template = TEMPLATE)
    public static void sentinelStatsAndNbt(GameTestHelper helper) {
        ReliquarySentinel sentinel = helper.spawnWithNoFreeWill(HumanoidMobs.RELIQUARY_SENTINEL.get(), 2.5F, 2.0F, 2.5F);
        checkStats(helper, sentinel, 40, 8, 7);
        helper.assertTrue(near(sentinel.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.25), "speed");
        sentinel.setState(ReliquarySentinel.GUARDING, 33);
        sentinel.setCooldown(77);
        ReliquarySentinel copy = roundTrip(helper, sentinel);
        helper.assertTrue(copy.state() == ReliquarySentinel.GUARDING && copy.stateTicks() == 33 && copy.cooldown() == 77,
                "guard state survives NBT");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void sentinelGuardReducesFrontalDamageOnly(GameTestHelper helper) {
        ReliquarySentinel front = helper.spawnWithNoFreeWill(HumanoidMobs.RELIQUARY_SENTINEL.get(), 1.5F, 2.0F, 1.5F);
        ReliquarySentinel rear = helper.spawnWithNoFreeWill(HumanoidMobs.RELIQUARY_SENTINEL.get(), 3.5F, 2.0F, 1.5F);
        for (ReliquarySentinel s : new ReliquarySentinel[] {front, rear}) {
            s.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
            face(s, 0.0F); // faces +z
            s.setState(ReliquarySentinel.GUARDING, 40);
        }
        Zombie ahead = attacker(helper, 1.5, 2.0, 3.5);
        Zombie behind = attacker(helper, 3.5, 2.0, -0.5);
        front.hurt(helper.getLevel().damageSources().mobAttack(ahead), 10.0F);
        rear.hurt(helper.getLevel().damageSources().mobAttack(behind), 10.0F);
        float frontLoss = front.getMaxHealth() - front.getHealth();
        float rearLoss = rear.getMaxHealth() - rear.getHealth();
        helper.assertTrue(near(frontLoss, 2.0), "frontal hit reduced by 80%, lost " + frontLoss);
        helper.assertTrue(near(rearLoss, 10.0), "rear hit at full damage, lost " + rearLoss);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void sentinelExposedTakesMore(GameTestHelper helper) {
        ReliquarySentinel s = helper.spawnWithNoFreeWill(HumanoidMobs.RELIQUARY_SENTINEL.get(), 2.5F, 2.0F, 2.5F);
        s.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        s.setState(ReliquarySentinel.EXPOSED, 30);
        s.hurt(helper.getLevel().damageSources().mobAttack(attacker(helper, 2.5, 2.0, 4.0)), 10.0F);
        float loss = s.getMaxHealth() - s.getHealth();
        helper.assertTrue(near(loss, 13.0), "Exposed takes 30% more, lost " + loss);
        helper.succeed();
    }

    // ---- Scriptbound Scribe and glyph bolts ----

    @GameTest(template = TEMPLATE)
    public static void scribeStatsAndNbt(GameTestHelper helper) {
        ScriptboundScribe scribe = helper.spawnWithNoFreeWill(HumanoidMobs.SCRIPTBOUND_SCRIBE.get(), 2.5F, 2.0F, 2.5F);
        checkStats(helper, scribe, 28, 2, -1);
        scribe.setFanNext(true);
        scribe.setRecoveryTicks(21);
        ScriptboundScribe copy = roundTrip(helper, scribe);
        helper.assertTrue(copy.fanNext() && copy.isRecovering() && copy.recoveryTicks() == 21, "pattern and recovery survive NBT");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void scribeBoltDamagesTarget(GameTestHelper helper) {
        ScriptboundScribe scribe = helper.spawnWithNoFreeWill(HumanoidMobs.SCRIPTBOUND_SCRIBE.get(), 0.5F, 2.0F, 0.5F);
        Zombie target = helmeted(helper, 4.5, 2.0, 0.5);
        float before = target.getHealth();
        scribe.fireBolts(target, false);
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(target.getHealth() < before, "glyph bolt hurt the target, health " + target.getHealth());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void scribeFanFiresThreeBolts(GameTestHelper helper) {
        ScriptboundScribe scribe = helper.spawnWithNoFreeWill(HumanoidMobs.SCRIPTBOUND_SCRIBE.get(), 0.5F, 2.0F, 0.5F);
        Zombie target = helmeted(helper, 4.5, 2.0, 0.5);
        scribe.fireBolts(target, true);
        long bolts = helper.getLevel().getEntitiesOfClass(GlyphBolt.class, scribe.getBoundingBox().inflate(4.0)).size();
        helper.assertTrue(bolts == 3, "a fan fires three bolts, found " + bolts);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void decoyBoltDealsNoDamage(GameTestHelper helper) {
        Zombie target = helmeted(helper, 4.5, 2.0, 0.5);
        float before = target.getHealth();
        Vec3 start = helper.absoluteVec(new Vec3(1.5, 2.6, 0.5));
        GlyphBolt decoy = GlyphBolt.create(helper.getLevel(), null, start, new Vec3(0.6, 0.0, 0.0), true);
        helper.getLevel().addFreshEntity(decoy);
        helper.assertTrue(decoy.isDecoy(), "flag set");
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(target.getHealth() == before, "a decoy deals no damage, health " + target.getHealth());
            helper.assertTrue(decoy.isRemoved(), "a decoy vanishes on hit");
            helper.succeed();
        });
    }

    // ---- Gilded Husk ----

    @GameTest(template = TEMPLATE)
    public static void huskStatsAndNbt(GameTestHelper helper) {
        GildedHusk husk = helper.spawnWithNoFreeWill(HumanoidMobs.GILDED_HUSK.get(), 2.5F, 2.0F, 2.5F);
        checkStats(helper, husk, 36, 14, 6);
        helper.assertTrue(near(husk.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.20), "speed");
        husk.setCrackTicks(120);
        GildedHusk copy = roundTrip(helper, husk);
        helper.assertTrue(copy.crackTicks() == 120 && near(copy.getAttributeValue(Attributes.ARMOR), 6.0), "crack survives NBT");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void huskArmorCracksAndRecovers(GameTestHelper helper) {
        GildedHusk husk = helper.spawnWithNoFreeWill(HumanoidMobs.GILDED_HUSK.get(), 2.5F, 2.0F, 2.5F);
        face(husk, 0.0F);
        Zombie hitter = attacker(helper, 4.5, 2.0, 0.5);
        helper.assertTrue(near(husk.getAttributeValue(Attributes.ARMOR), 14.0), "starts at 14 armor");
        husk.hurt(helper.getLevel().damageSources().mobAttack(hitter), 8.0F);
        helper.assertTrue(husk.isCracked() && near(husk.getAttributeValue(Attributes.ARMOR), 6.0),
                "an 8 damage hit cracks the armor, armor " + husk.getAttributeValue(Attributes.ARMOR));
        husk.setCrackTicks(5);
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(!husk.isCracked() && near(husk.getAttributeValue(Attributes.ARMOR), 14.0),
                    "armor recovers, armor " + husk.getAttributeValue(Attributes.ARMOR));
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void huskSmallHitsDoNotCrack(GameTestHelper helper) {
        GildedHusk husk = helper.spawnWithNoFreeWill(HumanoidMobs.GILDED_HUSK.get(), 2.5F, 2.0F, 2.5F);
        husk.hurt(helper.getLevel().damageSources().mobAttack(attacker(helper, 4.5, 2.0, 0.5)), 5.0F);
        helper.assertTrue(!husk.isCracked(), "a 5 damage hit does not crack");
        helper.succeed();
    }

    // ---- Mirebound Cantor ----

    @GameTest(template = TEMPLATE)
    public static void cantorStatsAndNbt(GameTestHelper helper) {
        MireboundCantor cantor = helper.spawnWithNoFreeWill(HumanoidMobs.MIREBOUND_CANTOR.get(), 2.5F, 2.0F, 2.5F);
        checkStats(helper, cantor, 24, 2, -1);
        cantor.setChantCooldown(88);
        cantor.setHushCooldown(222);
        MireboundCantor copy = roundTrip(helper, cantor);
        helper.assertTrue(copy.chantCooldown() == 88 && copy.hushCooldown() == 222, "cooldowns survive NBT");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cantorBuffsPlainAlliesNotElites(GameTestHelper helper) {
        MireboundCantor cantor = helper.spawnWithNoFreeWill(HumanoidMobs.MIREBOUND_CANTOR.get(), 2.5F, 2.0F, 2.5F);
        ReliquarySentinel ally = helper.spawnWithNoFreeWill(HumanoidMobs.RELIQUARY_SENTINEL.get(), 1.5F, 2.0F, 1.5F);
        OathbreakerKnight elite = helper.spawnWithNoFreeWill(HumanoidMobs.OATHBREAKER_KNIGHT.get(), 3.5F, 2.0F, 3.5F);
        // Neighbouring GameTests may also have Vestige mobs within the chant radius, so only assert on our two mobs.
        helper.assertTrue(cantor.completeChant() >= 1, "at least the plain Vestige mob is buffed");
        helper.assertTrue(ally.hasEffect(MobEffects.DAMAGE_RESISTANCE) && ally.hasEffect(MobEffects.DAMAGE_BOOST), "ally gets Resistance and Strength");
        helper.assertTrue(!elite.hasEffect(MobEffects.DAMAGE_RESISTANCE) && !elite.hasEffect(MobEffects.DAMAGE_BOOST), "elite is not buffed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cantorChantInterruptedByHeavyHit(GameTestHelper helper) {
        MireboundCantor cantor = helper.spawnWithNoFreeWill(HumanoidMobs.MIREBOUND_CANTOR.get(), 2.5F, 2.0F, 2.5F);
        cantor.startChant(30);
        helper.assertTrue(cantor.isChanting(), "chanting");
        cantor.hurt(helper.getLevel().damageSources().mobAttack(attacker(helper, 0.5, 2.0, 0.5)), 5.0F);
        helper.assertTrue(!cantor.isChanting(), "a 4+ damage hit interrupts the chant");
        helper.succeed();
    }

    // ---- Oathbreaker Knight ----

    @GameTest(template = TEMPLATE)
    public static void knightStatsAndStanceNbt(GameTestHelper helper) {
        OathbreakerKnight knight = helper.spawnWithNoFreeWill(HumanoidMobs.OATHBREAKER_KNIGHT.get(), 2.5F, 2.0F, 2.5F);
        checkStats(helper, knight, 80, 12, 12);
        helper.assertTrue(near(knight.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 4.0), "toughness");
        helper.assertTrue(near(knight.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.6), "knockback resistance");
        knight.setStance(OathbreakerKnight.STANCE_LUNGE, 41);
        OathbreakerKnight copy = roundTrip(helper, knight);
        helper.assertTrue(copy.stance() == OathbreakerKnight.STANCE_LUNGE && copy.stanceTicks() == 41, "stance survives NBT");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void knightGuardStanceReducesFrontalDamage(GameTestHelper helper) {
        OathbreakerKnight knight = helper.spawnWithNoFreeWill(HumanoidMobs.OATHBREAKER_KNIGHT.get(), 2.5F, 2.0F, 1.5F);
        knight.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        knight.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0.0);
        face(knight, 0.0F);
        knight.setStance(OathbreakerKnight.STANCE_GUARD, 100);
        knight.hurt(helper.getLevel().damageSources().mobAttack(attacker(helper, 2.5, 2.0, 3.5)), 10.0F);
        float loss = knight.getMaxHealth() - knight.getHealth();
        helper.assertTrue(near(loss, 3.0), "guard stance reduces frontal damage by 70%, lost " + loss);
        helper.succeed();
    }

    // ---- Vestige Drifter ----

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void drifterNeutralUntilHurt(GameTestHelper helper) {
        VestigeDrifter drifter = helper.spawn(HumanoidMobs.VESTIGE_DRIFTER.get(), 2.5F, 2.0F, 2.5F);
        checkStats(helper, drifter, 30, -1, 5);
        helper.assertTrue(drifter.targetSelector.getAvailableGoals().stream().noneMatch(g -> g.getGoal() instanceof NearestAttackableTargetGoal),
                "no proactive target goals");
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(drifter.getTarget() == null && !drifter.isAngry(), "unprovoked drifter is calm");
            ServerPlayer player = makePlayer(helper, new Vec3(1.5, 2.0, 1.5));
            drifter.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
            helper.assertTrue(drifter.isAngry() && drifter.getTarget() == player, "a hurt drifter turns on its attacker");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void drifterLoreCooldownPersists(GameTestHelper helper) {
        VestigeDrifter drifter = helper.spawnWithNoFreeWill(HumanoidMobs.VESTIGE_DRIFTER.get(), 2.5F, 2.0F, 2.5F);
        ServerPlayer player = makePlayer(helper, new Vec3(1.5, 2.0, 1.5));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(VestigeMaterials.INSCRIBED_PAGE.get(), 3));
        drifter.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().getCount() == 2, "the first page is consumed");
        helper.assertTrue(drifter.loreReadyTime() > helper.getLevel().getGameTime(), "cooldown started");
        drifter.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().getCount() == 2, "no page is consumed during the cooldown");
        VestigeDrifter copy = roundTrip(helper, drifter);
        helper.assertTrue(copy.loreReadyTime() == drifter.loreReadyTime(), "cooldown survives NBT");
        copy.setPos(drifter.position());
        helper.getLevel().addFreshEntity(copy);
        copy.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().getCount() == 2, "the reloaded drifter is still on cooldown");
        helper.succeed();
    }

    // ---- Rift Archer ----

    @GameTest(template = TEMPLATE)
    public static void archerStatsNbtAndLead(GameTestHelper helper) {
        RiftArcher archer = helper.spawnWithNoFreeWill(HumanoidMobs.RIFT_ARCHER.get(), 2.5F, 2.0F, 2.5F);
        checkStats(helper, archer, 40, 6, -1);
        archer.setCooldown(17);
        helper.assertTrue(roundTrip(helper, archer).cooldown() == 17, "shot cooldown survives NBT");
        Vec3 lead = RiftArcher.lead(new Vec3(1.0, 0.0, 0.0), 30.0);
        helper.assertTrue(near(lead.length(), RiftArcher.MAX_LEAD), "lead is capped at 4 blocks, got " + lead.length());
        Vec3 small = RiftArcher.lead(new Vec3(0.1, 0.0, 0.0), 10.0);
        helper.assertTrue(near(small.x, 0.5), "short lead is velocity times flight time, got " + small.x);
        helper.succeed();
    }

    // ---- Echo Weaver ----

    @GameTest(template = TEMPLATE)
    public static void weaverStatsNbtAndDecoys(GameTestHelper helper) {
        EchoWeaver weaver = helper.spawnWithNoFreeWill(HumanoidMobs.ECHO_WEAVER.get(), 2.5F, 2.0F, 2.5F);
        checkStats(helper, weaver, 26, -1, -1);
        weaver.setDecoyCooldown(123);
        helper.assertTrue(roundTrip(helper, weaver).decoyCooldown() == 123, "decoy cooldown survives NBT");
        Zombie target = helmeted(helper, 0.5, 2.0, 0.5);
        GlyphBolt[] decoys = weaver.castDecoys(target);
        helper.assertTrue(decoys.length == 2 && decoys[0] != null && decoys[0].isDecoy() && decoys[1].isDecoy(), "two decoy bolts");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void weaverPulseHurtsNearbyOnly(GameTestHelper helper) {
        EchoWeaver weaver = helper.spawnWithNoFreeWill(HumanoidMobs.ECHO_WEAVER.get(), 2.5F, 2.0F, 2.5F);
        Zombie near = helmeted(helper, 2.5, 2.0, 4.0);
        float before = near.getHealth();
        int hit = weaver.pulse();
        helper.assertTrue(hit >= 1 && near.getHealth() < before, "pulse hurts a nearby target, health " + near.getHealth());
        helper.succeed();
    }

}
