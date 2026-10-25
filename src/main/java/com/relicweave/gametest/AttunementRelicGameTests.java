package com.relicweave.gametest;

import com.mojang.authlib.GameProfile;
import com.relicweave.RelicWeave;
import com.relicweave.compat.curios.CuriosCompat;
import com.relicweave.registry.ModItems;
import com.relicweave.relic.LodestoneCharm;
import com.relicweave.relic.RelicActivation;
import com.relicweave.relic.RelicCooldowns;
import com.relicweave.relic.RelicEvents;
import com.relicweave.relic.RelicLocator;
import com.relicweave.relic.RelicType;
import com.relicweave.relic.WayfarerChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/** Server-side tests for the six 0.0.3 relics, their shared activation and cooldown rules, and the Curios fallback. */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AttunementRelicGameTests {
    private static final String TEMPLATE = "platform";

    private static final List<String> RECIPES = List.of(
            "lodestone_charm", "tidal_charm", "featherwoven_charm", "wayfarers_relic", "echo_charm", "guardian_relic");

    private AttunementRelicGameTests() {}

    // ---- Location / Curios fallback ----

    @GameTest(template = TEMPLATE)
    public static void relicsWorkFromInventoryWithoutCurios(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        helper.assertTrue(!CuriosCompat.isLoaded(), "Curios must not be loaded in the test environment");
        helper.assertTrue(CuriosCompat.findEquipped(player, s -> true).isEmpty(), "No Curios results expected");
        helper.assertTrue(!RelicLocator.has(player, RelicType.ECHO), "Nothing carried yet");
        player.getInventory().setItem(7, new ItemStack(ModItems.ECHO_CHARM.get()));
        helper.assertTrue(RelicLocator.has(player, RelicType.ECHO), "Echo Charm in the inventory should count");
        helper.assertTrue(RelicLocator.carried(player).size() == 1, "Exactly one relic type expected");
        player.getInventory().setItem(7, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.ECHO_CHARM.get()));
        helper.assertTrue(RelicLocator.has(player, RelicType.ECHO), "Echo Charm in the offhand should count");
        helper.succeed();
    }

    // ---- Cooldowns ----

    @GameTest(template = TEMPLATE)
    public static void cooldownIsPersistedAndSurvivesItemMoves(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        player.getInventory().setItem(0, new ItemStack(ModItems.ECHO_CHARM.get()));
        helper.assertTrue(RelicActivation.tryActivate(player, RelicType.ECHO), "Echo should activate");
        int remaining = RelicCooldowns.remainingTicks(player, RelicType.ECHO);
        helper.assertTrue(remaining == 45 * 20, "Echo cooldown should be 45 s, got " + remaining + " ticks");

        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        helper.assertTrue(persisted.getCompound("relicweave:relic_cooldowns").contains("echo_charm"),
                "Cooldown must live in the persisted player data (copied on respawn)");

        player.getInventory().setItem(0, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.ECHO_CHARM.get()));
        player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        player.getInventory().setItem(3, new ItemStack(ModItems.ECHO_CHARM.get()));
        helper.assertTrue(RelicCooldowns.remainingTicks(player, RelicType.ECHO) == remaining, "Moving the relic must not change the cooldown");
        helper.assertTrue(!RelicCooldowns.isReady(player, RelicType.ECHO), "Cooldown must still be running");
        helper.succeed();
    }

    // ---- Activation rules ----

    @GameTest(template = TEMPLATE)
    public static void duplicatesActivateOnceAndCooldownBlocksReuse(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        player.getInventory().setItem(0, new ItemStack(ModItems.ECHO_CHARM.get()));
        player.getInventory().setItem(1, new ItemStack(ModItems.ECHO_CHARM.get()));
        helper.assertTrue(RelicActivation.tryActivate(player, RelicType.ECHO), "First activation should work");
        int after = RelicCooldowns.remainingTicks(player, RelicType.ECHO);
        helper.assertTrue(!RelicActivation.tryActivate(player, RelicType.ECHO), "Second activation during the cooldown must do nothing");
        helper.assertTrue(RelicCooldowns.remainingTicks(player, RelicType.ECHO) == after, "Cooldown must not restart");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void activationRefusedWhenNotCarriedOrPassive(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        helper.assertTrue(!RelicActivation.tryActivate(player, RelicType.ECHO), "Not carried: must be refused");
        helper.assertTrue(RelicCooldowns.isReady(player, RelicType.ECHO), "A refused activation must not start a cooldown");
        player.getInventory().setItem(0, new ItemStack(ModItems.GUARDIAN_RELIC.get()));
        player.getInventory().setItem(1, new ItemStack(ModItems.TIDAL_CHARM.get()));
        player.getInventory().setItem(2, new ItemStack(ModItems.FEATHERWOVEN_CHARM.get()));
        helper.assertTrue(!RelicActivation.tryActivate(player, RelicType.GUARDIAN), "Passive relic: must be refused");
        helper.assertTrue(!RelicActivation.tryActivate(player, RelicType.TIDAL), "Passive relic: must be refused");
        helper.assertTrue(!RelicActivation.tryActivate(player, RelicType.FEATHERWOVEN), "Passive relic: must be refused");
        helper.assertTrue(RelicCooldowns.isReady(player, RelicType.GUARDIAN), "Passive refusal must not start a cooldown");
        helper.succeed();
    }

    // ---- Echo ----

    @GameTest(template = TEMPLATE)
    public static void echoMakesNearbyEnemiesGlowButNotDistantOnes(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        player.getInventory().setItem(0, new ItemStack(ModItems.ECHO_CHARM.get()));
        Zombie near = spawnZombie(helper, player.position().add(6, 0, 0));
        Zombie far = spawnZombie(helper, player.position().add(RelicActivation.ECHO_RADIUS + 12, 0, 0));
        helper.assertTrue(RelicActivation.tryActivate(player, RelicType.ECHO), "Echo should activate");
        boolean nearGlows = near.hasEffect(MobEffects.GLOWING);
        boolean farGlows = far.hasEffect(MobEffects.GLOWING);
        near.discard();
        far.discard();
        helper.assertTrue(nearGlows, "A zombie inside the radius should glow");
        helper.assertTrue(!farGlows, "A zombie outside the radius must not glow");
        helper.succeed();
    }

    // ---- Guardian ----

    @GameTest(template = TEMPLATE)
    public static void guardianShieldsOnceOnNonLethalCriticalHit(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        player.getInventory().setItem(0, new ItemStack(ModItems.GUARDIAN_RELIC.get()));
        helper.assertTrue(!RelicEvents.guardianCheck(player, 5.0F), "A hit leaving 15 health must not trigger");
        LivingDamageEvent hit = new LivingDamageEvent(player, player.damageSources().generic(), 15.0F);
        RelicEvents.onLivingDamage(hit);
        helper.assertTrue(!hit.isCanceled(), "The guardian must never cancel anything");
        helper.assertTrue(player.getAbsorptionAmount() == RelicEvents.GUARDIAN_SHIELD, "Expected 4 hearts of absorption, got " + player.getAbsorptionAmount());
        helper.assertTrue(!RelicCooldowns.isReady(player, RelicType.GUARDIAN), "Guardian should be on cooldown");
        float absorption = player.getAbsorptionAmount();
        helper.assertTrue(!RelicEvents.guardianCheck(player, 15.0F), "No second trigger during the cooldown");
        helper.assertTrue(player.getAbsorptionAmount() == absorption, "Absorption must not change during the cooldown");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void guardianIgnoresLethalHits(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        player.getInventory().setItem(0, new ItemStack(ModItems.GUARDIAN_RELIC.get()));
        LivingDamageEvent lethal = new LivingDamageEvent(player, player.damageSources().generic(), 25.0F);
        RelicEvents.onLivingDamage(lethal);
        helper.assertTrue(!lethal.isCanceled(), "A lethal hit must not be cancelled by the guardian");
        helper.assertTrue(player.getAbsorptionAmount() == 0.0F, "A lethal hit must grant nothing");
        helper.assertTrue(RelicCooldowns.isReady(player, RelicType.GUARDIAN), "A lethal hit must not start the cooldown");
        helper.succeed();
    }

    // ---- Featherwoven ----

    @GameTest(template = TEMPLATE)
    public static void featherwovenHalvesFallDamageAndSlowsDangerousFalls(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        LivingFallEvent without = new LivingFallEvent(player, 10.0F, 1.0F);
        RelicEvents.onLivingFall(without);
        helper.assertTrue(without.getDamageMultiplier() == 1.0F, "No charm: fall damage unchanged");

        player.getInventory().setItem(0, new ItemStack(ModItems.FEATHERWOVEN_CHARM.get()));
        LivingFallEvent with = new LivingFallEvent(player, 10.0F, 1.0F);
        RelicEvents.onLivingFall(with);
        helper.assertTrue(with.getDamageMultiplier() == 0.5F, "Charm: fall damage halved, got multiplier " + with.getDamageMultiplier());

        player.fallDistance = 5.0F;
        RelicEvents.featherwovenTick(player);
        helper.assertTrue(!player.hasEffect(MobEffects.SLOW_FALLING), "A short fall must not trigger Slow Falling");
        player.fallDistance = RelicEvents.DANGEROUS_FALL + 1.0F;
        RelicEvents.featherwovenTick(player);
        helper.assertTrue(player.hasEffect(MobEffects.SLOW_FALLING), "A dangerous fall should apply Slow Falling");
        helper.assertTrue(!RelicCooldowns.isReady(player, RelicType.FEATHERWOVEN), "Featherwoven should be on cooldown");
        helper.succeed();
    }

    // ---- Tidal ----

    @GameTest(template = TEMPLATE)
    public static void tidalDoesNothingOutOfWater(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        player.getInventory().setItem(0, new ItemStack(ModItems.TIDAL_CHARM.get()));
        RelicEvents.tidalTick(player);
        helper.assertTrue(!player.hasEffect(MobEffects.WATER_BREATHING), "No water breathing outside water");
        helper.succeed();
    }

    // ---- Wayfarer ----

    @GameTest(template = TEMPLATE)
    public static void wayfarerBindingWritesNbt(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        ItemStack relic = new ItemStack(ModItems.WAYFARERS_RELIC.get());
        WayfarerChannel.bind(player, relic);
        helper.assertTrue(WayfarerChannel.hasBinding(relic), "Binding tag expected");
        CompoundTag tag = WayfarerChannel.binding(relic);
        BlockPos pos = player.blockPosition();
        helper.assertTrue(tag.getInt("x") == pos.getX() && tag.getInt("y") == pos.getY() && tag.getInt("z") == pos.getZ(), "Binding must store the block position");
        helper.assertTrue(tag.getString("dimension").equals("minecraft:overworld"), "Binding must store the dimension, got " + tag.getString("dimension"));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void wayfarerRefusesOtherDimensionAndUnbound(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        ItemStack relic = new ItemStack(ModItems.WAYFARERS_RELIC.get());
        player.getInventory().setItem(0, relic);
        helper.assertTrue(!RelicActivation.tryActivate(player, RelicType.WAYFARER), "Unbound relic must be refused");
        setBinding(relic, player.blockPosition().offset(3, 0, 0), "minecraft:the_nether");
        helper.assertTrue(!RelicActivation.tryActivate(player, RelicType.WAYFARER), "Another dimension must be refused");
        helper.assertTrue(!WayfarerChannel.isChanneling(player), "No channel may start");
        helper.assertTrue(RelicCooldowns.isReady(player, RelicType.WAYFARER), "No cooldown for refused activation");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void wayfarerChannelTeleportsAndStartsCooldown(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        ItemStack relic = new ItemStack(ModItems.WAYFARERS_RELIC.get());
        player.getInventory().setItem(0, relic);
        BlockPos target = helper.absolutePos(new BlockPos(5, 1, 5));
        setBinding(relic, target, "minecraft:overworld");
        helper.assertTrue(RelicActivation.tryActivate(player, RelicType.WAYFARER), "Activation should start the channel");
        helper.assertTrue(WayfarerChannel.isChanneling(player), "Channel expected");
        for (int i = 0; i < WayfarerChannel.CHANNEL_TICKS - 1; i++) {
            WayfarerChannel.tickChannel(player);
        }
        helper.assertTrue(RelicCooldowns.isReady(player, RelicType.WAYFARER), "Cooldown must not start before completion");
        WayfarerChannel.tickChannel(player);
        helper.assertTrue(!WayfarerChannel.isChanneling(player), "Channel should have finished");
        // A FakePlayer has a no-op network handler, so teleportTo cannot move it here; completion is asserted through the
        // cooldown (an obstructed destination, see the next test, does not start one).
        helper.assertTrue(!RelicCooldowns.isReady(player, RelicType.WAYFARER), "Cooldown should start on success");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void wayfarerObstructedDestinationCancelsWithoutCooldown(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        ItemStack relic = new ItemStack(ModItems.WAYFARERS_RELIC.get());
        player.getInventory().setItem(0, relic);
        BlockPos target = new BlockPos(5, 1, 5);
        helper.setBlock(target.above(), Blocks.STONE);
        setBinding(relic, helper.absolutePos(target), "minecraft:overworld");
        helper.assertTrue(RelicActivation.tryActivate(player, RelicType.WAYFARER), "Activation should start the channel");
        for (int i = 0; i < WayfarerChannel.CHANNEL_TICKS; i++) {
            WayfarerChannel.tickChannel(player);
        }
        helper.assertTrue(!WayfarerChannel.isChanneling(player), "Channel should have ended");
        helper.assertTrue(RelicCooldowns.isReady(player, RelicType.WAYFARER), "An obstructed destination must not start the cooldown");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void wayfarerChannelCancelsWhenMovingWithoutCooldown(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        ItemStack relic = new ItemStack(ModItems.WAYFARERS_RELIC.get());
        player.getInventory().setItem(0, relic);
        setBinding(relic, helper.absolutePos(new BlockPos(5, 1, 5)), "minecraft:overworld");
        helper.assertTrue(RelicActivation.tryActivate(player, RelicType.WAYFARER), "Activation should start the channel");
        WayfarerChannel.tickChannel(player);
        player.moveTo(player.getX() + 2.0, player.getY(), player.getZ(), 0.0F, 0.0F);
        WayfarerChannel.tickChannel(player);
        helper.assertTrue(!WayfarerChannel.isChanneling(player), "Moving must cancel the channel");
        helper.assertTrue(RelicCooldowns.isReady(player, RelicType.WAYFARER), "A cancelled channel must not start the cooldown");
        helper.succeed();
    }

    // ---- Lodestone ----

    @GameTest(template = TEMPLATE)
    public static void lodestoneToggleFlipsAndPullsItems(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        player.getInventory().setItem(0, new ItemStack(ModItems.LODESTONE_CHARM.get()));
        helper.assertTrue(!RelicActivation.isLodestoneEnabled(player), "Default is off");
        ItemEntity item = spawnItem(helper, player.position().add(4, 0, 0));
        LodestoneCharm.pull(player);
        helper.assertTrue(item.getDeltaMovement().x == 0.0, "Disabled: items must not move");

        helper.assertTrue(RelicActivation.tryActivate(player, RelicType.LODESTONE), "Toggle should succeed");
        helper.assertTrue(RelicActivation.isLodestoneEnabled(player), "Toggle should switch it on");
        for (int i = 0; i < 4; i++) {
            LodestoneCharm.pull(player);
        }
        double speed = item.getDeltaMovement().x;
        helper.assertTrue(speed < 0.0 && Math.abs(speed) <= LodestoneCharm.MAX_SPEED, "Item should move toward the player, vx=" + speed);

        ItemEntity delayed = new ItemEntity(helper.getLevel(), player.getX() - 4, player.getY(), player.getZ(), new ItemStack(Items.DIRT));
        delayed.setPickUpDelay(40);
        delayed.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(delayed);
        LodestoneCharm.pull(player);
        helper.assertTrue(delayed.getDeltaMovement().x == 0.0, "Items in pickup delay must not be pulled");
        item.discard();
        delayed.discard();

        helper.assertTrue(RelicActivation.tryActivate(player, RelicType.LODESTONE), "Second toggle should succeed");
        helper.assertTrue(!RelicActivation.isLodestoneEnabled(player), "Second toggle switches it off");
        helper.succeed();
    }

    // ---- Recipes ----

    @GameTest(template = TEMPLATE)
    public static void allSixRelicRecipesLoad(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getServer().getRecipeManager();
        for (String name : RECIPES) {
            helper.assertTrue(recipes.byKey(RelicWeave.id("relic/" + name)).isPresent(), "Missing recipe relic/" + name);
        }
        helper.succeed();
    }

    // ---- helpers ----

    private static void setBinding(ItemStack relic, BlockPos pos, String dimension) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        tag.putString("dimension", dimension);
        relic.getOrCreateTag().put(WayfarerChannel.BINDING_TAG, tag);
    }

    private static Zombie spawnZombie(GameTestHelper helper, Vec3 pos) {
        Zombie zombie = EntityType.ZOMBIE.create(helper.getLevel());
        zombie.setNoAi(true);
        zombie.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(zombie);
        return zombie;
    }

    private static ItemEntity spawnItem(GameTestHelper helper, Vec3 pos) {
        ItemEntity item = new ItemEntity(helper.getLevel(), pos.x, pos.y, pos.z, new ItemStack(Items.DIRT));
        item.setNoPickUpDelay();
        item.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(item);
        return item;
    }

    private static ServerPlayer makePlayer(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "relicweave-attune-test"));
        player.setGameMode(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        return player;
    }
}
