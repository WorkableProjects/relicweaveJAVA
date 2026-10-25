package com.relicweave.gametest;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.relicweave.RelicWeave;
import com.relicweave.block.TransmutationTableBlockEntity;
import com.relicweave.enchantment.KeenEdgeEvents;
import com.relicweave.enchantment.PurlingHandler;
import com.relicweave.menu.TransmutationTableMenu;
import com.relicweave.recipe.TransmutationRecipe;
import com.relicweave.recipe.TransmutationRecipeSerializer;
import com.relicweave.registry.ModBlocks;
import com.relicweave.registry.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.ItemStackHandler;

import java.util.Map;
import java.util.UUID;

/**
 * Server-side regression tests. Run with {@code ./gradlew runGameTestServer}; only registered in development runs
 * where {@code forge.enabledGameTestNamespaces} includes {@code relicweave}.
 */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RelicWeaveGameTests {
    private static final String TEMPLATE = "platform";
    private static final BlockPos TABLE_POS = new BlockPos(2, 1, 2);

    private RelicWeaveGameTests() {}

    // ---- Transmutation Table ----

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void keenEdgeRecipeProducesExactlyOneBook(GameTestHelper helper) {
        TransmutationTableBlockEntity table = placeTable(helper, new ItemStack(Items.DIAMOND, 2), new ItemStack(Items.AMETHYST_SHARD, 3));
        helper.runAfterDelay(99, () -> helper.assertTrue(table.getItems().getStackInSlot(2).isEmpty(), "Finished before process_time"));
        // Long after completion: the full output slot must block a second operation without consuming anything.
        helper.runAfterDelay(160, () -> {
            ItemStackHandler items = table.getItems();
            assertCount(helper, items.getStackInSlot(0), Items.DIAMOND, 1);
            assertCount(helper, items.getStackInSlot(1), Items.AMETHYST_SHARD, 2);
            assertBook(helper, items.getStackInSlot(2), ModEnchantments.KEEN_EDGE.get(), 1);
            helper.assertTrue(table.getProgress() == 0, "Progress should stay reset while output is blocked");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void purlingRecipeProducesBook(GameTestHelper helper) {
        TransmutationTableBlockEntity table = placeTable(helper, new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.AMETHYST_SHARD));
        helper.runAfterDelay(120, () -> {
            ItemStackHandler items = table.getItems();
            helper.assertTrue(items.getStackInSlot(0).isEmpty() && items.getStackInSlot(1).isEmpty(), "Ingredients should be consumed");
            assertBook(helper, items.getStackInSlot(2), ModEnchantments.PURLING.get(), 1);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void mismatchedIngredientsDoNothing(GameTestHelper helper) {
        // Diamond in the catalyst slot matches no recipe (bypasses slot validation on purpose).
        TransmutationTableBlockEntity table = placeTable(helper, new ItemStack(Items.DIAMOND), new ItemStack(Items.DIAMOND));
        helper.runAfterDelay(120, () -> {
            assertCount(helper, table.getItems().getStackInSlot(0), Items.DIAMOND, 1);
            assertCount(helper, table.getItems().getStackInSlot(1), Items.DIAMOND, 1);
            helper.assertTrue(table.getItems().getStackInSlot(2).isEmpty(), "Nothing should be produced");
            helper.assertTrue(table.getProgress() == 0, "No progress without a recipe");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void removingCatalystResetsProgressWithoutLoss(GameTestHelper helper) {
        TransmutationTableBlockEntity table = placeTable(helper, new ItemStack(Items.DIAMOND), new ItemStack(Items.AMETHYST_SHARD));
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(table.getProgress() > 0, "Should be processing");
            table.getItems().setStackInSlot(1, ItemStack.EMPTY);
        });
        helper.runAfterDelay(52, () -> {
            helper.assertTrue(table.getProgress() == 0, "Progress should reset when the recipe becomes invalid");
            assertCount(helper, table.getItems().getStackInSlot(0), Items.DIAMOND, 1);
            helper.assertTrue(table.getItems().getStackInSlot(2).isEmpty(), "Nothing should be produced");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void inventoryAndProgressSurviveSaveLoad(GameTestHelper helper) {
        TransmutationTableBlockEntity table = placeTable(helper, new ItemStack(Items.DIAMOND, 5), new ItemStack(Items.AMETHYST_SHARD, 7));
        helper.runAfterDelay(40, () -> {
            CompoundTag saved = table.saveWithoutMetadata();
            TransmutationTableBlockEntity reloaded = new TransmutationTableBlockEntity(table.getBlockPos(), table.getBlockState());
            reloaded.load(saved);
            helper.assertTrue(reloaded.getProgress() == table.getProgress() && reloaded.getProgress() > 0, "Progress not persisted");
            assertCount(helper, reloaded.getItems().getStackInSlot(0), Items.DIAMOND, 5);
            assertCount(helper, reloaded.getItems().getStackInSlot(1), Items.AMETHYST_SHARD, 7);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void menuEnforcesSlotRules(GameTestHelper helper) {
        TransmutationTableBlockEntity table = placeTable(helper, ItemStack.EMPTY, ItemStack.EMPTY);
        Player player = helper.makeMockSurvivalPlayer();
        TransmutationTableMenu menu = (TransmutationTableMenu) table.createMenu(0, player.getInventory(), player);

        ItemStack book = EnchantedBookItem.createForEnchantment(
                new net.minecraft.world.item.enchantment.EnchantmentInstance(ModEnchantments.KEEN_EDGE.get(), 1));
        helper.assertFalse(menu.getSlot(2).mayPlace(book), "Output slot must reject insertion");
        helper.assertTrue(menu.getSlot(0).mayPlace(new ItemStack(Items.DIAMOND)), "Input should accept diamonds");
        helper.assertFalse(menu.getSlot(0).mayPlace(new ItemStack(Items.DIRT)), "Input should reject dirt");
        helper.assertTrue(menu.getSlot(1).mayPlace(new ItemStack(Items.AMETHYST_SHARD)), "Catalyst should accept amethyst");
        helper.assertFalse(menu.getSlot(1).mayPlace(new ItemStack(Items.DIAMOND)), "Catalyst should reject diamonds");

        // Shift-click from the player's inventory (menu slot 3 = inventory slot 9).
        player.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 4));
        menu.quickMoveStack(player, 3);
        assertCount(helper, table.getItems().getStackInSlot(0), Items.DIAMOND, 4);

        player.getInventory().setItem(9, new ItemStack(Items.AMETHYST_SHARD, 2));
        menu.quickMoveStack(player, 3);
        assertCount(helper, table.getItems().getStackInSlot(1), Items.AMETHYST_SHARD, 2);

        player.getInventory().setItem(9, book.copy());
        menu.quickMoveStack(player, 3);
        helper.assertTrue(table.getItems().getStackInSlot(2).isEmpty(), "Shift-click must not insert into output");

        // Shift-click out of the table returns items to the player.
        menu.quickMoveStack(player, 0);
        helper.assertTrue(table.getItems().getStackInSlot(0).isEmpty(), "Input should be emptied by shift-click");
        helper.assertTrue(player.getInventory().countItem(Items.DIAMOND) == 4, "Diamonds should return to the player");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void serializerValidatesJson(GameTestHelper helper) {
        TransmutationRecipeSerializer serializer = new TransmutationRecipeSerializer();
        String valid = """
                {"type":"relicweave:transmutation","input":{"item":"minecraft:diamond"},"input_count":2,
                 "catalyst":{"item":"minecraft:amethyst_shard"},"catalyst_count":3,
                 "enchantment":"relicweave:keen_edge","level":2,"process_time":40}""";
        TransmutationRecipe recipe = serializer.fromJson(RelicWeave.id("test"), json(valid));
        helper.assertTrue(recipe.getInputCount() == 2 && recipe.getCatalystCount() == 3
                && recipe.getLevel() == 2 && recipe.getProcessTime() == 40, "Valid recipe parsed incorrectly");

        assertRejected(helper, serializer, valid.replace("\"input\":{\"item\":\"minecraft:diamond\"},", ""));
        assertRejected(helper, serializer, valid.replace("relicweave:keen_edge", "relicweave:missing"));
        assertRejected(helper, serializer, valid.replace("\"level\":2", "\"level\":9"));
        assertRejected(helper, serializer, valid.replace("\"input_count\":2", "\"input_count\":0"));
        assertRejected(helper, serializer, valid.replace("\"process_time\":40", "\"process_time\":-5"));
        assertRejected(helper, serializer, valid.replace("minecraft:diamond", "minecraft:not_an_item"));
        helper.succeed();
    }

    // ---- Keen Edge ----

    @GameTest(template = TEMPLATE)
    public static void keenEdgeAddsDamageOnlyToMelee(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        float base = 2.0F;

        player.setItemSlot(EquipmentSlot.MAINHAND, enchanted(Items.IRON_SWORD, ModEnchantments.KEEN_EDGE.get(), 1));
        assertDamage(helper, player, true, base, base + KeenEdgeEvents.DAMAGE_PER_LEVEL, "Keen Edge I sword melee");
        assertDamage(helper, player, false, base, base, "Non-melee damage with Keen Edge sword");

        player.setItemSlot(EquipmentSlot.MAINHAND, enchanted(Items.IRON_AXE, ModEnchantments.KEEN_EDGE.get(), 3));
        assertDamage(helper, player, true, base, base + 3 * KeenEdgeEvents.DAMAGE_PER_LEVEL, "Keen Edge III axe melee");

        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        assertDamage(helper, player, true, base, base, "Unenchanted sword melee");

        player.setItemSlot(EquipmentSlot.MAINHAND, enchanted(Items.STICK, ModEnchantments.KEEN_EDGE.get(), 1));
        assertDamage(helper, player, true, base, base, "Keen Edge forced onto a stick");

        helper.assertTrue(ModEnchantments.KEEN_EDGE.get().canEnchant(new ItemStack(Items.DIAMOND_SWORD)), "Swords eligible");
        helper.assertTrue(ModEnchantments.KEEN_EDGE.get().canEnchant(new ItemStack(Items.GOLDEN_AXE)), "Axes eligible");
        helper.assertFalse(ModEnchantments.KEEN_EDGE.get().canEnchant(new ItemStack(Items.BOW)), "Bows not eligible");
        helper.succeed();
    }

    // ---- Purling ----

    @GameTest(template = TEMPLATE, timeoutTicks = 700)
    public static void purlingWorksWithoutPearlsAndEnforcesCooldown(GameTestHelper helper) {
        ServerPlayer player = makeServerPlayer(helper);
        player.getInventory().setItem(5, new ItemStack(Items.ENDER_PEARL, 3));

        // No Purling boots: nothing happens.
        PurlingHandler.tryActivate(player);
        helper.assertTrue(countPurlingPearls(helper, player) == 0, "Activated without Purling boots");

        player.setItemSlot(EquipmentSlot.FEET, enchanted(Items.IRON_BOOTS, ModEnchantments.PURLING.get(), 1));
        PurlingHandler.tryActivate(player);
        helper.assertTrue(countPurlingPearls(helper, player) == 1, "Expected one Purling pearl");
        helper.assertTrue(player.getInventory().countItem(Items.ENDER_PEARL) == 3, "Purling must not consume carried pearls");

        // Packet spam during cooldown does nothing.
        for (int i = 0; i < 20; i++) {
            PurlingHandler.tryActivate(player);
        }
        helper.assertTrue(countPurlingPearls(helper, player) == 1, "Cooldown not enforced");

        helper.runAfterDelay(PurlingHandler.COOLDOWN_TICKS - 20, () -> {
            discardPurlingPearls(helper);
            PurlingHandler.tryActivate(player);
            helper.assertTrue(countPurlingPearls(helper, player) == 0, "Cooldown ended early");
        });
        helper.runAfterDelay(PurlingHandler.COOLDOWN_TICKS + 2, () -> {
            PurlingHandler.tryActivate(player);
            helper.assertTrue(countPurlingPearls(helper, player) == 1, "Should activate after cooldown");
            discardPurlingPearls(helper);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void purlingNeedsNoPearls(GameTestHelper helper) {
        ServerPlayer player = makeServerPlayer(helper);
        player.setItemSlot(EquipmentSlot.FEET, enchanted(Items.IRON_BOOTS, ModEnchantments.PURLING.get(), 1));
        helper.assertTrue(player.getInventory().countItem(Items.ENDER_PEARL) == 0, "Test player should start with no pearls");
        PurlingHandler.tryActivate(player);
        helper.assertTrue(countPurlingPearls(helper, player) == 1, "Purling should throw a pearl even with none carried");
        discardPurlingPearls(helper);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void purlingProtectionIsScopedToPurlingPearls(GameTestHelper helper) {
        ServerPlayer player = makeServerPlayer(helper);
        player.setItemSlot(EquipmentSlot.FEET, enchanted(Items.IRON_BOOTS, ModEnchantments.PURLING.get(), 1));
        PurlingHandler.tryActivate(player);
        ThrownEnderpearl purlingPearl = helper.getLevel().getEntitiesOfClass(ThrownEnderpearl.class,
                new AABB(player.blockPosition()).inflate(4), PurlingHandler::isPurlingPearl).get(0);
        ThrownEnderpearl vanillaPearl = new ThrownEnderpearl(helper.getLevel(), player);

        EntityTeleportEvent.EnderPearl purlingLanding = ForgeEventFactory.onEnderPearlLand(player, 0, 0, 0, purlingPearl, 5.0F, null);
        EntityTeleportEvent.EnderPearl vanillaLanding = ForgeEventFactory.onEnderPearlLand(player, 0, 0, 0, vanillaPearl, 5.0F, null);
        helper.assertTrue(purlingLanding.getAttackDamage() == 0.0F, "Purling pearl should deal no landing damage");
        helper.assertTrue(vanillaLanding.getAttackDamage() == 5.0F, "Ordinary pearls must keep vanilla landing damage");
        purlingPearl.discard();
        vanillaPearl.discard();
        helper.succeed();
    }

    // ---- helpers ----

    /**
     * A server player with a no-op connection. ({@code makeMockServerPlayerInLevel} fails on Forge 1.20.1 because
     * its fake connection has no Netty channel.)
     */
    private static ServerPlayer makeServerPlayer(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "relicweave-test"));
        player.setGameMode(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        return player;
    }

    private static int countPurlingPearls(GameTestHelper helper, ServerPlayer player) {
        return helper.getLevel().getEntitiesOfClass(ThrownEnderpearl.class,
                new AABB(player.blockPosition()).inflate(4), PurlingHandler::isPurlingPearl).size();
    }

    private static void discardPurlingPearls(GameTestHelper helper) {
        helper.getLevel().getEntitiesOfClass(ThrownEnderpearl.class,
                new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(8), PurlingHandler::isPurlingPearl)
                .forEach(net.minecraft.world.entity.Entity::discard);
    }

    private static TransmutationTableBlockEntity placeTable(GameTestHelper helper, ItemStack input, ItemStack catalyst) {
        helper.setBlock(TABLE_POS, ModBlocks.TRANSMUTATION_TABLE.get());
        TransmutationTableBlockEntity table = (TransmutationTableBlockEntity) helper.getBlockEntity(TABLE_POS);
        table.getItems().setStackInSlot(0, input);
        table.getItems().setStackInSlot(1, catalyst);
        return table;
    }

    private static void assertCount(GameTestHelper helper, ItemStack stack, Item item, int count) {
        helper.assertTrue(stack.is(item) && stack.getCount() == count,
                "Expected " + count + "x " + item + " but found " + stack.getCount() + "x " + stack.getItem());
    }

    private static void assertBook(GameTestHelper helper, ItemStack stack, Enchantment enchantment, int level) {
        helper.assertTrue(stack.is(Items.ENCHANTED_BOOK) && stack.getCount() == 1, "Expected one enchanted book, found " + stack);
        Map<Enchantment, Integer> stored = EnchantmentHelper.deserializeEnchantments(EnchantedBookItem.getEnchantments(stack));
        helper.assertTrue(stored.size() == 1 && Integer.valueOf(level).equals(stored.get(enchantment)),
                "Book has wrong enchantments: " + stored);
    }

    private static ItemStack enchanted(Item item, Enchantment enchantment, int level) {
        ItemStack stack = new ItemStack(item);
        stack.enchant(enchantment, level);
        return stack;
    }

    private static void assertDamage(GameTestHelper helper, Player player, boolean melee, float amount, float expected, String label) {
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 1, 1));
        float before = pig.getHealth();
        pig.hurt(melee ? player.damageSources().playerAttack(player) : player.damageSources().mobAttack(player), amount);
        float dealt = before - pig.getHealth();
        pig.discard();
        helper.assertTrue(Math.abs(dealt - expected) < 1.0E-4F, label + ": expected " + expected + " damage, got " + dealt);
    }

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    private static void assertRejected(GameTestHelper helper, TransmutationRecipeSerializer serializer, String text) {
        try {
            serializer.fromJson(RelicWeave.id("bad"), json(text));
        } catch (JsonParseException expected) {
            return;
        }
        helper.fail("Malformed recipe was accepted: " + text);
    }
}
