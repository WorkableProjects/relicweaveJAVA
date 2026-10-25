package com.relicweave.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.relicweave.RelicWeave;
import com.relicweave.block.infuser.CelestialInfuserBlockEntity;
import com.relicweave.block.infuser.CelestialInfuserMenu;
import com.relicweave.block.infuser.InfusionRecipe;
import com.relicweave.block.infuser.InfusionRecipeSerializer;
import com.relicweave.infusion.InfusionType;
import com.relicweave.infusion.Infusions;
import com.relicweave.registry.ModBlocks;
import com.relicweave.registry.ModItems;
import com.relicweave.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.ItemStackHandler;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Server-side tests for the Celestial Infuser. Run with {@code ./gradlew runGameTestServer}. */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InfuserGameTests {
    private static final String TEMPLATE = "platform";
    private static final BlockPos INFUSER_POS = new BlockPos(2, 1, 2);
    private static final int PROCESS_TICKS = 200;

    private InfuserGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 320)
    public static void infuserAddsInfusionAndConsumesInputs(GameTestHelper helper) {
        CelestialInfuserBlockEntity infuser = place(helper, helmet(), new ItemStack(Items.IRON_BLOCK, 3));
        helper.runAfterDelay(PROCESS_TICKS - 5, () -> {
            helper.assertTrue(infuser.getItems().getStackInSlot(2).isEmpty(), "Finished before process_time");
            helper.assertTrue(infuser.getProgress() > 0, "Should be processing");
        });
        helper.runAfterDelay(PROCESS_TICKS + 10, () -> {
            ItemStackHandler items = infuser.getItems();
            helper.assertTrue(items.getStackInSlot(0).isEmpty(), "Armor should be consumed");
            assertCount(helper, items.getStackInSlot(1), Items.IRON_BLOCK, 2);
            ItemStack output = items.getStackInSlot(2);
            helper.assertTrue(output.is(ModItems.CELESTIAL_HELMET.get()) && output.getCount() == 1, "Expected one celestial helmet, found " + output);
            helper.assertTrue(Infusions.get(output).equals(List.of(InfusionType.GOLEMED)), "Wrong infusions: " + Infusions.get(output));
            helper.assertTrue(output.getAttributeModifiers(EquipmentSlot.HEAD).containsKey(Attributes.KNOCKBACK_RESISTANCE),
                    "Infused helmet should expose the infusion's attribute modifier");
            helper.assertTrue(infuser.getProgress() == 0, "Progress should reset after completion");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 320)
    public static void fourthInfusionIsRefused(GameTestHelper helper) {
        ItemStack full = helmet(InfusionType.COBBLED, InfusionType.OXIDIZED, InfusionType.CONDUCTIVE);
        CelestialInfuserBlockEntity infuser = place(helper, full, new ItemStack(Items.IRON_BLOCK));
        helper.runAfterDelay(PROCESS_TICKS + 20, () -> {
            helper.assertTrue(infuser.getProgress() == 0, "Must not make progress with three infusions present");
            helper.assertTrue(infuser.getItems().getStackInSlot(0).is(ModItems.CELESTIAL_HELMET.get()), "Armor must stay in place");
            assertCount(helper, infuser.getItems().getStackInSlot(1), Items.IRON_BLOCK, 1);
            helper.assertTrue(infuser.getItems().getStackInSlot(2).isEmpty(), "Nothing should be produced");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 320)
    public static void duplicateInfusionIsRefused(GameTestHelper helper) {
        CelestialInfuserBlockEntity infuser = place(helper, helmet(InfusionType.GOLEMED), new ItemStack(Items.IRON_BLOCK));
        helper.runAfterDelay(PROCESS_TICKS + 20, () -> {
            helper.assertTrue(infuser.getProgress() == 0, "Must not make progress for a duplicate infusion");
            helper.assertTrue(infuser.getItems().getStackInSlot(0).is(ModItems.CELESTIAL_HELMET.get()), "Armor must stay in place");
            assertCount(helper, infuser.getItems().getStackInSlot(1), Items.IRON_BLOCK, 1);
            helper.assertTrue(infuser.getItems().getStackInSlot(2).isEmpty(), "Nothing should be produced");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 320)
    public static void nonCelestialArmorIsRefused(GameTestHelper helper) {
        // Bypasses slot validation on purpose: the recipe itself must also reject non-Celestial armor.
        CelestialInfuserBlockEntity infuser = place(helper, new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.IRON_BLOCK));
        helper.runAfterDelay(PROCESS_TICKS + 20, () -> {
            helper.assertTrue(infuser.getProgress() == 0, "Must not make progress for non-Celestial armor");
            helper.assertTrue(infuser.getItems().getStackInSlot(0).is(Items.DIAMOND_HELMET), "Armor must stay in place");
            assertCount(helper, infuser.getItems().getStackInSlot(1), Items.IRON_BLOCK, 1);
            helper.assertTrue(infuser.getItems().getStackInSlot(2).isEmpty(), "Nothing should be produced");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 320)
    public static void occupiedOutputBlocksProcessing(GameTestHelper helper) {
        CelestialInfuserBlockEntity infuser = place(helper, helmet(), new ItemStack(Items.IRON_BLOCK));
        infuser.getItems().setStackInSlot(2, new ItemStack(Items.STONE));
        helper.runAfterDelay(PROCESS_TICKS + 20, () -> {
            helper.assertTrue(infuser.getProgress() == 0, "Must not make progress while the output is occupied");
            helper.assertTrue(infuser.getItems().getStackInSlot(0).is(ModItems.CELESTIAL_HELMET.get()), "Armor must stay in place");
            assertCount(helper, infuser.getItems().getStackInSlot(1), Items.IRON_BLOCK, 1);
            assertCount(helper, infuser.getItems().getStackInSlot(2), Items.STONE, 1);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void insufficientMaterialCountDoesNothing(GameTestHelper helper) {
        // Cobbled needs 32 cobblestone.
        CelestialInfuserBlockEntity infuser = place(helper, helmet(), new ItemStack(Items.COBBLESTONE, 31));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(infuser.getProgress() == 0, "Must not make progress without enough material");
            assertCount(helper, infuser.getItems().getStackInSlot(1), Items.COBBLESTONE, 31);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void inventoryAndProgressSurviveSaveLoad(GameTestHelper helper) {
        CelestialInfuserBlockEntity infuser = place(helper, helmet(), new ItemStack(Items.IRON_BLOCK, 5));
        helper.runAfterDelay(40, () -> {
            CompoundTag saved = infuser.saveWithoutMetadata();
            CelestialInfuserBlockEntity reloaded = new CelestialInfuserBlockEntity(infuser.getBlockPos(), infuser.getBlockState());
            reloaded.load(saved);
            helper.assertTrue(reloaded.getProgress() == infuser.getProgress() && reloaded.getProgress() > 0, "Progress not persisted");
            helper.assertTrue(reloaded.getItems().getStackInSlot(0).is(ModItems.CELESTIAL_HELMET.get()), "Armor not persisted");
            assertCount(helper, reloaded.getItems().getStackInSlot(1), Items.IRON_BLOCK, 5);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void menuEnforcesSlotRules(GameTestHelper helper) {
        CelestialInfuserBlockEntity infuser = place(helper, ItemStack.EMPTY, ItemStack.EMPTY);
        Player player = helper.makeMockSurvivalPlayer();
        CelestialInfuserMenu menu = (CelestialInfuserMenu) infuser.createMenu(0, player.getInventory(), player);

        helper.assertFalse(menu.getSlot(0).mayPlace(new ItemStack(Items.DIAMOND_HELMET)), "Armor slot must reject non-Celestial armor");
        helper.assertTrue(menu.getSlot(0).mayPlace(helmet()), "Armor slot must accept Celestial armor");
        helper.assertTrue(menu.getSlot(1).mayPlace(new ItemStack(Items.IRON_BLOCK)), "Material slot should accept iron blocks");
        helper.assertFalse(menu.getSlot(1).mayPlace(new ItemStack(Items.DIRT)), "Material slot should reject dirt");
        helper.assertFalse(menu.getSlot(2).mayPlace(helmet()), "Output slot must reject insertion");

        // Shift-click from the player's inventory (menu slot 3 = inventory slot 9).
        player.getInventory().setItem(9, helmet());
        menu.quickMoveStack(player, 3);
        helper.assertTrue(infuser.getItems().getStackInSlot(0).is(ModItems.CELESTIAL_HELMET.get()), "Shift-click should fill the armor slot");

        player.getInventory().setItem(9, new ItemStack(Items.DIAMOND_HELMET));
        menu.quickMoveStack(player, 3);
        helper.assertTrue(infuser.getItems().getStackInSlot(2).isEmpty(), "Shift-click must not insert into output");
        helper.assertTrue(infuser.getItems().getStackInSlot(0).is(ModItems.CELESTIAL_HELMET.get()), "Armor slot must be unchanged");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void serializerValidatesJson(GameTestHelper helper) {
        InfusionRecipeSerializer serializer = new InfusionRecipeSerializer();
        String valid = """
                {"type":"relicweave:infusion","ingredient":{"item":"minecraft:iron_block"},"count":2,
                 "infusion":"relicweave:golemed","process_time":40}""";
        InfusionRecipe recipe = serializer.fromJson(RelicWeave.id("test"), json(valid));
        helper.assertTrue(recipe.getCount() == 2 && recipe.getInfusion() == InfusionType.GOLEMED && recipe.getProcessTime() == 40,
                "Valid recipe parsed incorrectly");

        String defaults = """
                {"type":"relicweave:infusion","ingredient":{"item":"minecraft:iron_block"},"infusion":"relicweave:golemed"}""";
        InfusionRecipe defaulted = serializer.fromJson(RelicWeave.id("test"), json(defaults));
        helper.assertTrue(defaulted.getCount() == 1 && defaulted.getProcessTime() == 200, "Defaults not applied");

        assertRejected(helper, serializer, valid.replace("relicweave:golemed", "relicweave:missing"));
        assertRejected(helper, serializer, valid.replace("\"count\":2", "\"count\":0"));
        assertRejected(helper, serializer, valid.replace("\"count\":2", "\"count\":65"));
        assertRejected(helper, serializer, valid.replace("\"process_time\":40", "\"process_time\":0"));
        assertRejected(helper, serializer, valid.replace("\"ingredient\":{\"item\":\"minecraft:iron_block\"},", ""));
        assertRejected(helper, serializer, valid.replace("minecraft:iron_block", "minecraft:not_an_item"));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void allInfusionRecipesLoad(GameTestHelper helper) {
        List<InfusionRecipe> recipes = helper.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.INFUSION_TYPE.get());
        helper.assertTrue(recipes.size() == 10, "Expected 10 infusion recipes, found " + recipes.size());
        Set<InfusionType> covered = EnumSet.noneOf(InfusionType.class);
        for (InfusionRecipe recipe : recipes) {
            covered.add(recipe.getInfusion());
        }
        helper.assertTrue(covered.equals(EnumSet.allOf(InfusionType.class)), "Not every infusion has a recipe: " + covered);
        helper.succeed();
    }

    private static ItemStack helmet(InfusionType... infusions) {
        ItemStack stack = new ItemStack(ModItems.CELESTIAL_HELMET.get());
        for (InfusionType type : infusions) {
            stack = Infusions.withInfusion(stack, type);
        }
        return stack;
    }

    private static CelestialInfuserBlockEntity place(GameTestHelper helper, ItemStack armor, ItemStack material) {
        helper.setBlock(INFUSER_POS, ModBlocks.CELESTIAL_INFUSER.get());
        CelestialInfuserBlockEntity infuser = (CelestialInfuserBlockEntity) helper.getBlockEntity(INFUSER_POS);
        infuser.getItems().setStackInSlot(0, armor);
        infuser.getItems().setStackInSlot(1, material);
        return infuser;
    }

    private static void assertCount(GameTestHelper helper, ItemStack stack, Item item, int count) {
        helper.assertTrue(stack.is(item) && stack.getCount() == count,
                "Expected " + count + "x " + item + " but found " + stack.getCount() + "x " + stack.getItem());
    }

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    private static void assertRejected(GameTestHelper helper, InfusionRecipeSerializer serializer, String text) {
        try {
            serializer.fromJson(RelicWeave.id("bad"), json(text));
        } catch (JsonParseException expected) {
            return;
        }
        helper.fail("Malformed recipe was accepted: " + text);
    }
}
