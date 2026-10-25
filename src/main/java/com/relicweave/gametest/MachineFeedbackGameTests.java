package com.relicweave.gametest;

import com.relicweave.RelicWeave;
import com.relicweave.block.MachineStatus;
import com.relicweave.block.TransmutationTableBlockEntity;
import com.relicweave.block.infuser.CelestialInfuserBlockEntity;
import com.relicweave.infusion.InfusionType;
import com.relicweave.infusion.Infusions;
import com.relicweave.registry.ModBlocks;
import com.relicweave.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Status messages and completion counters of both machines. Run with {@code ./gradlew runGameTestServer}. */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MachineFeedbackGameTests {
    private static final String TEMPLATE = "platform";
    private static final BlockPos POS = new BlockPos(2, 1, 2);

    private MachineFeedbackGameTests() {}

    // ---- Transmutation Table ----

    @GameTest(template = TEMPLATE, timeoutTicks = 260)
    public static void tableCountsEachCompletionOnce(GameTestHelper helper) {
        TransmutationTableBlockEntity table = placeTable(helper, new ItemStack(Items.DIAMOND, 2), new ItemStack(Items.AMETHYST_SHARD, 3));
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(table.getStatus() == MachineStatus.WORKING, "Expected WORKING but was " + table.getStatus());
            helper.assertTrue(table.getCompletions() == 0, "No completion yet");
        });
        // The book does not stack, so the second operation is blocked: the counter must stay at exactly 1.
        helper.runAfterDelay(110, () -> helper.assertTrue(table.getCompletions() == 1, "Expected 1 completion, got " + table.getCompletions()));
        helper.runAfterDelay(200, () -> {
            helper.assertTrue(table.getCompletions() == 1, "Expected still 1 completion, got " + table.getCompletions());
            helper.assertTrue(table.getStatus() == MachineStatus.OUTPUT_FULL, "Expected OUTPUT_FULL but was " + table.getStatus());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void tableReportsFullOutput(GameTestHelper helper) {
        TransmutationTableBlockEntity table = placeTable(helper, new ItemStack(Items.DIAMOND, 2), new ItemStack(Items.AMETHYST_SHARD, 3));
        table.getItems().setStackInSlot(TransmutationTableBlockEntity.SLOT_OUTPUT, new ItemStack(Items.STICK));
        helper.runAfterDelay(130, () -> {
            helper.assertTrue(table.getStatus() == MachineStatus.OUTPUT_FULL, "Expected OUTPUT_FULL but was " + table.getStatus());
            helper.assertTrue(table.getCompletions() == 0, "No completion expected");
            assertCount(helper, table.getItems().getStackInSlot(0), Items.DIAMOND, 2);
            assertCount(helper, table.getItems().getStackInSlot(1), Items.AMETHYST_SHARD, 3);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void tableReportsMissingIngredient(GameTestHelper helper) {
        TransmutationTableBlockEntity table = placeTable(helper, new ItemStack(Items.DIAMOND), new ItemStack(Items.DIAMOND));
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(table.getStatus() == MachineStatus.MISSING_INGREDIENT, "Expected MISSING_INGREDIENT but was " + table.getStatus());
            helper.assertTrue(table.getCompletions() == 0, "No completion expected");
            helper.succeed();
        });
    }

    // ---- Celestial Infuser ----

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void infuserReportsMaxInfusions(GameTestHelper helper) {
        expectStatus(helper, helmet(InfusionType.COBBLED, InfusionType.OXIDIZED, InfusionType.CONDUCTIVE),
                new ItemStack(Items.IRON_BLOCK), MachineStatus.MAX_INFUSIONS);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void infuserReportsAlreadyInfused(GameTestHelper helper) {
        expectStatus(helper, helmet(InfusionType.GOLEMED), new ItemStack(Items.IRON_BLOCK), MachineStatus.ALREADY_INFUSED);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void infuserReportsWrongArmorPiece(GameTestHelper helper) {
        expectStatus(helper, new ItemStack(ModItems.CELESTIAL_BOOTS.get()), new ItemStack(Items.PRISMARINE_SHARD, 16),
                MachineStatus.WRONG_ARMOR_PIECE);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void infuserReportsTooFewMaterials(GameTestHelper helper) {
        expectStatus(helper, helmet(), new ItemStack(Items.COBBLESTONE), MachineStatus.MISSING_INGREDIENT);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 320)
    public static void infuserCountsCompletion(GameTestHelper helper) {
        CelestialInfuserBlockEntity infuser = placeInfuser(helper, helmet(), new ItemStack(Items.IRON_BLOCK, 3));
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(infuser.getStatus() == MachineStatus.WORKING, "Expected WORKING but was " + infuser.getStatus());
            helper.assertTrue(infuser.getCompletions() == 0, "No completion yet");
        });
        helper.runAfterDelay(250, () -> {
            helper.assertTrue(infuser.getCompletions() == 1, "Expected 1 completion, got " + infuser.getCompletions());
            helper.assertTrue(infuser.getStatus() == MachineStatus.IDLE, "Expected IDLE but was " + infuser.getStatus());
            helper.assertTrue(!infuser.getItems().getStackInSlot(2).isEmpty(), "Output expected");
            helper.succeed();
        });
    }

    // ---- helpers ----

    private static void expectStatus(GameTestHelper helper, ItemStack armor, ItemStack material, MachineStatus expected) {
        CelestialInfuserBlockEntity infuser = placeInfuser(helper, armor, material);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(infuser.getStatus() == expected, "Expected " + expected + " but was " + infuser.getStatus());
            helper.assertTrue(infuser.getCompletions() == 0, "No completion expected");
            helper.assertTrue(infuser.getItems().getStackInSlot(2).isEmpty(), "Nothing should be produced");
            helper.succeed();
        });
    }

    private static ItemStack helmet(InfusionType... infusions) {
        ItemStack stack = new ItemStack(ModItems.CELESTIAL_HELMET.get());
        for (InfusionType type : infusions) {
            stack = Infusions.withInfusion(stack, type);
        }
        return stack;
    }

    private static TransmutationTableBlockEntity placeTable(GameTestHelper helper, ItemStack input, ItemStack catalyst) {
        helper.setBlock(POS, ModBlocks.TRANSMUTATION_TABLE.get());
        TransmutationTableBlockEntity table = (TransmutationTableBlockEntity) helper.getBlockEntity(POS);
        table.getItems().setStackInSlot(0, input);
        table.getItems().setStackInSlot(1, catalyst);
        return table;
    }

    private static CelestialInfuserBlockEntity placeInfuser(GameTestHelper helper, ItemStack armor, ItemStack material) {
        helper.setBlock(POS, ModBlocks.CELESTIAL_INFUSER.get());
        CelestialInfuserBlockEntity infuser = (CelestialInfuserBlockEntity) helper.getBlockEntity(POS);
        infuser.getItems().setStackInSlot(0, armor);
        infuser.getItems().setStackInSlot(1, material);
        return infuser;
    }

    private static void assertCount(GameTestHelper helper, ItemStack stack, Item item, int count) {
        helper.assertTrue(stack.is(item) && stack.getCount() == count,
                "Expected " + count + "x " + item + " but found " + stack.getCount() + "x " + stack.getItem());
    }
}
