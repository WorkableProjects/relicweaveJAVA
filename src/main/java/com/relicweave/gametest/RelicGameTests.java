package com.relicweave.gametest;

import com.mojang.authlib.GameProfile;
import com.relicweave.RelicWeave;
import com.relicweave.item.CharmType;
import com.relicweave.item.ModFoods;
import com.relicweave.registry.ModItems;
import com.relicweave.relic.CharmEvents;
import com.relicweave.relic.TotemEvents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.UUID;

/** Server-side tests for charms, totems, and their recipes. Run with {@code ./gradlew runGameTestServer}. */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RelicGameTests {
    private static final String TEMPLATE = "platform";

    private static final List<String> RECIPES = List.of(
            "copper_charm", "iron_charm", "gold_charm", "diamond_charm", "netherite_charm_smithing",
            "diamond_totem", "netherite_totem_smithing", "ultra_totem", "diamond_steak", "netherite_steak");

    private RelicGameTests() {}

    // ---- Charms ----

    @GameTest(template = TEMPLATE)
    public static void eachCharmAddsAndRemovesItsModifier(GameTestHelper helper) {
        ServerPlayer player = makeServerPlayer(helper);
        Object[][] cases = {
                {ModItems.COPPER_CHARM, CharmType.COPPER, Attributes.MOVEMENT_SPEED},
                {ModItems.IRON_CHARM, CharmType.IRON, Attributes.ARMOR},
                {ModItems.GOLD_CHARM, CharmType.GOLD, Attributes.LUCK},
                {ModItems.DIAMOND_CHARM, CharmType.DIAMOND, Attributes.MAX_HEALTH},
                {ModItems.NETHERITE_CHARM, CharmType.NETHERITE, Attributes.KNOCKBACK_RESISTANCE},
        };
        for (Object[] c : cases) {
            @SuppressWarnings("unchecked")
            Item charm = ((RegistryObject<Item>) c[0]).get();
            CharmType type = (CharmType) c[1];
            AttributeInstance attribute = player.getAttribute((Attribute) c[2]);
            UUID id = CharmEvents.modifierId(type);

            player.getInventory().setItem(5, new ItemStack(charm));
            CharmEvents.updateCharms(player);
            helper.assertTrue(attribute.getModifier(id) != null, type + " charm should add its modifier");
            player.getInventory().setItem(5, ItemStack.EMPTY);
            CharmEvents.updateCharms(player);
            helper.assertTrue(attribute.getModifier(id) == null, type + " charm modifier should be removed with the charm");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void offhandCharmCountsButArmorSlotDoesNot(GameTestHelper helper) {
        ServerPlayer player = makeServerPlayer(helper);
        UUID id = CharmEvents.modifierId(CharmType.IRON);
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);

        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.IRON_CHARM.get()));
        CharmEvents.updateCharms(player);
        helper.assertTrue(armor.getModifier(id) == null, "A charm in an armor slot must not count");
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);

        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.IRON_CHARM.get()));
        CharmEvents.updateCharms(player);
        helper.assertTrue(armor.getModifier(id) != null, "An offhand charm should count");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void twoCopperCharmsGiveOneModifier(GameTestHelper helper) {
        ServerPlayer player = makeServerPlayer(helper);
        player.getInventory().setItem(0, new ItemStack(ModItems.COPPER_CHARM.get()));
        player.getInventory().setItem(1, new ItemStack(ModItems.COPPER_CHARM.get()));
        CharmEvents.updateCharms(player);
        CharmEvents.updateCharms(player);
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        helper.assertTrue(speed.getModifier(CharmEvents.modifierId(CharmType.COPPER)) != null, "Copper modifier expected");
        helper.assertTrue(speed.getModifiers().size() == 1, "Expected exactly one modifier, got " + speed.getModifiers().size());
        helper.assertTrue(Math.abs(speed.getValue() - speed.getBaseValue() * 1.05) < 1.0E-6, "Speed should be +5%, got " + speed.getValue());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void diamondCharmRemovalClampsHealth(GameTestHelper helper) {
        ServerPlayer player = makeServerPlayer(helper);
        player.getInventory().setItem(0, new ItemStack(ModItems.DIAMOND_CHARM.get()));
        CharmEvents.updateCharms(player);
        helper.assertTrue(player.getMaxHealth() == 22.0F, "Max health should be 22, got " + player.getMaxHealth());
        player.setHealth(22.0F);
        player.getInventory().setItem(0, ItemStack.EMPTY);
        CharmEvents.updateCharms(player);
        helper.assertTrue(player.getMaxHealth() == 20.0F, "Max health should return to 20, got " + player.getMaxHealth());
        helper.assertTrue(player.getHealth() == 20.0F, "Health should be clamped to 20, got " + player.getHealth());
        helper.succeed();
    }

    // ---- Totems ----

    @GameTest(template = TEMPLATE)
    public static void totemPassiveOnlyFromOffhandUnlessUltra(GameTestHelper helper) {
        ServerPlayer player = makeServerPlayer(helper);

        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.DIAMOND_TOTEM.get()));
        TotemEvents.updatePassive(player);
        helper.assertTrue(player.getMaxHealth() == 20.0F, "Diamond totem in main hand must give nothing, got " + player.getMaxHealth());
        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.DIAMOND_TOTEM.get()));
        TotemEvents.updatePassive(player);
        helper.assertTrue(player.getMaxHealth() == 24.0F, "Diamond totem in offhand should give +4, got " + player.getMaxHealth());
        player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        TotemEvents.updatePassive(player);
        helper.assertTrue(player.getMaxHealth() == 20.0F, "Passive should be removed with the totem, got " + player.getMaxHealth());

        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.ULTRA_TOTEM.get()));
        TotemEvents.updatePassive(player);
        helper.assertTrue(player.getMaxHealth() == 28.0F, "Ultra totem in main hand should give +8, got " + player.getMaxHealth());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void highestTierTotemWins(GameTestHelper helper) {
        ServerPlayer player = makeServerPlayer(helper);
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.DIAMOND_TOTEM.get()));
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.ULTRA_TOTEM.get()));
        TotemEvents.updatePassive(player);
        helper.assertTrue(player.getMaxHealth() == 28.0F, "Ultra should win over Diamond, got " + player.getMaxHealth());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void deathSaveRestoresHealthSetsCooldownKeepsItem(GameTestHelper helper) {
        ServerPlayer player = makeServerPlayer(helper);
        DamageSource lethal = helper.getLevel().damageSources().generic();

        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.DIAMOND_TOTEM.get()));
        player.setHealth(1.0F);
        helper.assertTrue(TotemEvents.tryDeathSave(player, lethal), "Diamond totem should save");
        helper.assertTrue(player.getHealth() == 4.0F, "Diamond save should leave 4 HP, got " + player.getHealth());
        helper.assertTrue(player.getCooldowns().isOnCooldown(ModItems.DIAMOND_TOTEM.get()), "Totem should be on cooldown");
        helper.assertTrue(player.getOffhandItem().is(ModItems.DIAMOND_TOTEM.get()), "Totem must not be consumed");
        helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE), "Resistance expected");
        helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION), "Regeneration expected");
        helper.assertTrue(!TotemEvents.tryDeathSave(player, lethal), "Second save during cooldown must fail");

        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.NETHERITE_TOTEM.get()));
        player.setHealth(1.0F);
        helper.assertTrue(TotemEvents.tryDeathSave(player, lethal), "Netherite totem should save");
        helper.assertTrue(player.getHealth() == 10.0F, "Netherite save should leave half health, got " + player.getHealth());

        player.removeAllEffects();
        player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.ULTRA_TOTEM.get()));
        player.setHealth(1.0F);
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 200));
        helper.assertTrue(TotemEvents.tryDeathSave(player, lethal), "Ultra totem should save from the main hand");
        helper.assertTrue(player.getHealth() == player.getMaxHealth(), "Ultra save should fully heal, got " + player.getHealth());
        helper.assertTrue(!player.hasEffect(net.minecraft.world.effect.MobEffects.POISON), "Ultra save should clear harmful effects");
        helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.ABSORPTION), "Absorption expected");
        helper.assertTrue(player.getEffect(net.minecraft.world.effect.MobEffects.REGENERATION).getAmplifier() == 2, "Regeneration III expected");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void deathSaveIgnoresVoidDamage(GameTestHelper helper) {
        ServerPlayer player = makeServerPlayer(helper);
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.DIAMOND_TOTEM.get()));
        helper.assertTrue(!TotemEvents.tryDeathSave(player, helper.getLevel().damageSources().fellOutOfWorld()),
                "Void-type damage must not be saved");
        helper.assertTrue(!player.getCooldowns().isOnCooldown(ModItems.DIAMOND_TOTEM.get()), "No cooldown when not saved");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void unheldTotemDoesNotSave(GameTestHelper helper) {
        ServerPlayer player = makeServerPlayer(helper);
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.DIAMOND_TOTEM.get()));
        helper.assertTrue(!TotemEvents.tryDeathSave(player, helper.getLevel().damageSources().generic()),
                "Diamond totem in the main hand must not save");
        helper.succeed();
    }

    // ---- Recipes and food ----

    @GameTest(template = TEMPLATE)
    public static void allRelicRecipesLoad(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getServer().getRecipeManager();
        for (String name : RECIPES) {
            helper.assertTrue(recipes.byKey(RelicWeave.id("relic/" + name)).isPresent(), "Missing recipe relic/" + name);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void steaksCarryTheirEffects(GameTestHelper helper) {
        helper.assertTrue(ModFoods.DIAMOND_STEAK.getEffects().size() == 2, "Diamond steak should have 2 effects");
        helper.assertTrue(ModFoods.NETHERITE_STEAK.getEffects().size() == 4, "Netherite steak should have 4 effects");
        helper.assertTrue(ModItems.DIAMOND_STEAK.get().isEdible() && ModItems.NETHERITE_STEAK.get().isEdible(), "Steaks must be edible");
        helper.succeed();
    }

    private static ServerPlayer makeServerPlayer(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "relicweave-relic-test"));
        player.setGameMode(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        return player;
    }
}
