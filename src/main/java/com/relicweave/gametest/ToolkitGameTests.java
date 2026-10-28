package com.relicweave.gametest;

import com.mojang.authlib.GameProfile;
import com.relicweave.RelicWeave;
import com.relicweave.block.furnace.SuperFurnaceBlockEntity;
import com.relicweave.block.furnace.SuperFurnaceTier;
import com.relicweave.item.ScubaArmorItem;
import com.relicweave.item.ScubaEvents;
import com.relicweave.registry.ModBlocks;
import com.relicweave.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingBreatheEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** 0.0.4.5 toolkit: Multitool, Super Furnace and Scuba Gear. Run with {@code ./gradlew runGameTestServer}. */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ToolkitGameTests {
    private static final String TEMPLATE = "platform";
    private static final BlockPos FURNACE_POS = new BlockPos(2, 1, 2);

    private ToolkitGameTests() {}

    // --- Multitool -----------------------------------------------------------------------------------------------

    @GameTest(template = TEMPLATE)
    public static void multitoolMinesLikePickaxeAndAxeAndCutsLikeSword(GameTestHelper helper) {
        ItemStack tool = new ItemStack(ModItems.DIAMOND_MULTITOOL.get());
        float diamond = Tiers.DIAMOND.getSpeed();
        helper.assertTrue(tool.getDestroySpeed(Blocks.STONE.defaultBlockState()) == diamond, "Stone at pickaxe speed");
        helper.assertTrue(tool.getDestroySpeed(Blocks.OAK_LOG.defaultBlockState()) == diamond, "Logs at axe speed");
        helper.assertTrue(tool.getDestroySpeed(Blocks.COBWEB.defaultBlockState()) == 15.0F, "Cobwebs at sword speed");
        helper.assertTrue(tool.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "Diamond tier mines obsidian");
        helper.assertTrue(tool.isCorrectToolForDrops(Blocks.COBWEB.defaultBlockState()), "Cobweb drops string");
        helper.assertFalse(new ItemStack(ModItems.IRON_MULTITOOL.get()).isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()),
                "Iron tier must not mine obsidian");
        for (var action : new net.minecraftforge.common.ToolAction[]{ToolActions.PICKAXE_DIG, ToolActions.AXE_DIG,
                ToolActions.SWORD_DIG, ToolActions.SWORD_SWEEP, ToolActions.AXE_STRIP}) {
            helper.assertTrue(tool.canPerformAction(action), "Missing tool action " + action);
        }
        helper.assertFalse(tool.canPerformAction(ToolActions.SHOVEL_DIG), "Not a shovel");
        helper.assertTrue(tool.getMaxDamage() == Tiers.DIAMOND.getUses() * 2, "Twice the tier durability");
        double damage = tool.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).stream()
                .mapToDouble(m -> m.getAmount()).sum();
        helper.assertTrue(damage == 3.0 + Tiers.DIAMOND.getAttackDamageBonus(), "Sword damage expected, got " + damage);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void multitoolTakesSwordAxeAndPickaxeEnchantments(GameTestHelper helper) {
        ItemStack tool = new ItemStack(ModItems.NETHERITE_MULTITOOL.get());
        for (Enchantment e : new Enchantment[]{Enchantments.SHARPNESS, Enchantments.SWEEPING_EDGE, Enchantments.MOB_LOOTING,
                Enchantments.BLOCK_EFFICIENCY, Enchantments.BLOCK_FORTUNE, Enchantments.SILK_TOUCH, Enchantments.UNBREAKING}) {
            helper.assertTrue(e.canEnchant(tool), "Should accept " + e.getDescriptionId());
            helper.assertTrue(e.canApplyAtEnchantingTable(tool), "Enchanting table should offer " + e.getDescriptionId());
        }
        for (Enchantment e : new Enchantment[]{Enchantments.ALL_DAMAGE_PROTECTION, Enchantments.POWER_ARROWS, Enchantments.RIPTIDE}) {
            helper.assertFalse(e.canEnchant(tool), "Should not accept " + e.getDescriptionId());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void multitoolStripsLogs(GameTestHelper helper) {
        BlockPos log = new BlockPos(2, 1, 2);
        helper.setBlock(log, Blocks.OAK_LOG);
        ServerPlayer player = makePlayer(helper);
        ItemStack tool = new ItemStack(ModItems.IRON_MULTITOOL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        BlockPos abs = helper.absolutePos(log);
        tool.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
        helper.assertBlockPresent(Blocks.STRIPPED_OAK_LOG, log);
        helper.assertTrue(tool.getDamageValue() == 1, "Stripping costs 1 durability");
        helper.succeed();
    }

    // --- Super Furnace -------------------------------------------------------------------------------------------

    @GameTest(template = TEMPLATE)
    public static void superFurnaceTiersGetBetter(GameTestHelper helper) {
        SuperFurnaceTier previous = null;
        for (SuperFurnaceTier tier : SuperFurnaceTier.values()) {
            if (previous != null) {
                helper.assertTrue(tier.speed() > previous.speed(), tier + " should be faster than " + previous);
                helper.assertTrue(tier.rawOutput() >= previous.rawOutput(), tier + " output must not drop");
                helper.assertTrue(tier.experience() >= previous.experience(), tier + " experience must not drop");
                helper.assertTrue(!tier.needsFuel() || tier.fuelEfficiency() > previous.fuelEfficiency(),
                        tier + " should use fuel better");
            }
            previous = tier;
        }
        helper.assertTrue(SuperFurnaceTier.IRON.cookTime(200) == 100, "Iron: 2x speed");
        helper.assertTrue(SuperFurnaceTier.IRON.burnTime(1600) == 800, "Iron: coal still smelts 8 items");
        helper.assertTrue(SuperFurnaceTier.DIAMOND.burnTime(1600) == 600, "Diamond: coal smelts 12 items");
        helper.assertTrue(SuperFurnaceTier.DIAMOND.rawOutput() == 2 && SuperFurnaceTier.CELESTIAL.rawOutput() == 3,
                "2x from Diamond, 3x at Celestial");
        helper.assertTrue(new ItemStack(Items.RAW_IRON).is(SuperFurnaceBlockEntity.MULTIPLIED), "Raw iron is multiplied");
        helper.assertTrue(new ItemStack(Items.IRON_ORE).is(SuperFurnaceBlockEntity.MULTIPLIED), "Iron ore is multiplied");
        helper.assertFalse(new ItemStack(Items.SAND).is(SuperFurnaceBlockEntity.MULTIPLIED), "Sand is not multiplied");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void ironSuperFurnaceSmeltsTwiceAsFast(GameTestHelper helper) {
        SuperFurnaceBlockEntity furnace = place(helper, ModBlocks.IRON_SUPER_FURNACE.get(),
                new ItemStack(Items.RAW_IRON, 4), new ItemStack(Items.COAL));
        helper.runAfterDelay(95, () -> helper.assertTrue(furnace.getItem(2).isEmpty(), "Finished before 100 ticks"));
        helper.runAfterDelay(110, () -> {
            assertStack(helper, furnace.getItem(2), Items.IRON_INGOT, 1);
            helper.assertTrue(helper.getBlockState(FURNACE_POS).getValue(AbstractFurnaceBlock.LIT), "Furnace should be lit");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 800)
    public static void diamondSuperFurnaceDoublesRawOreAndStretchesFuel(GameTestHelper helper) {
        SuperFurnaceBlockEntity furnace = place(helper, ModBlocks.DIAMOND_SUPER_FURNACE.get(),
                new ItemStack(Items.RAW_IRON, 16), new ItemStack(Items.COAL));
        helper.runAfterDelay(700, () -> {
            assertStack(helper, furnace.getItem(2), Items.IRON_INGOT, 24);
            assertStack(helper, furnace.getItem(0), Items.RAW_IRON, 4);
            helper.assertTrue(furnace.getItem(1).isEmpty(), "One coal smelts 12 items at Diamond tier");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void diamondSuperFurnaceDoesNotDoubleOtherItems(GameTestHelper helper) {
        SuperFurnaceBlockEntity furnace = place(helper, ModBlocks.DIAMOND_SUPER_FURNACE.get(),
                new ItemStack(Items.SAND, 2), new ItemStack(Items.COAL));
        helper.runAfterDelay(60, () -> {
            assertStack(helper, furnace.getItem(2), Items.GLASS, 1);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void celestialSuperFurnaceNeedsNoFuelAndTriples(GameTestHelper helper) {
        SuperFurnaceBlockEntity furnace = place(helper, ModBlocks.CELESTIAL_SUPER_FURNACE.get(),
                new ItemStack(Items.RAW_GOLD, 2), ItemStack.EMPTY);
        helper.runAfterDelay(50, () -> {
            assertStack(helper, furnace.getItem(2), Items.GOLD_INGOT, 6);
            helper.assertTrue(furnace.getItem(0).isEmpty(), "Both raw gold smelted");
        });
        helper.runAfterDelay(55, () -> {
            helper.assertFalse(helper.getBlockState(FURNACE_POS).getValue(AbstractFurnaceBlock.LIT), "Goes out when empty");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void goldSuperFurnaceDoublesExperience(GameTestHelper helper) {
        SuperFurnaceBlockEntity furnace = place(helper, ModBlocks.GOLD_SUPER_FURNACE.get(),
                new ItemStack(Items.RAW_GOLD), new ItemStack(Items.COAL));
        helper.runAfterDelay(80, () -> {
            assertStack(helper, furnace.getItem(2), Items.GOLD_INGOT, 1);
            Vec3 at = Vec3.atCenterOf(helper.absolutePos(FURNACE_POS));
            // Only orbs spawned by this call: tests run side by side, so others may drop orbs nearby.
            AABB box = new AABB(at, at).inflate(0.5);
            var before = new java.util.HashSet<>(helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, box.inflate(4.0)));
            furnace.getRecipesToAwardAndPopExperience(helper.getLevel(), at);
            int xp = helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, box).stream()
                    .filter(orb -> !before.contains(orb)).mapToInt(ExperienceOrb::getValue).sum();
            helper.assertTrue(xp == 2, "Raw gold gives 1 XP, doubled at Gold tier; got " + xp);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void toolkitRecipesAreLoaded(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        String[] ids = {"iron_multitool", "diamond_multitool", "netherite_multitool_smithing", "celestial_multitool_smithing",
                "iron_super_furnace", "gold_super_furnace", "diamond_super_furnace", "netherite_super_furnace_smithing",
                "celestial_super_furnace_smithing", "scuba_mask", "scuba_tank", "scuba_wetsuit", "scuba_flippers"};
        for (String id : ids) {
            Recipe<?> recipe = recipes.byKey(RelicWeave.id("toolkit/" + id)).orElse(null);
            helper.assertTrue(recipe != null, "Missing recipe toolkit/" + id);
            if (recipe instanceof CraftingRecipe) {
                Item result = recipe.getResultItem(helper.getLevel().registryAccess()).getItem();
                helper.assertTrue(result.builtInRegistryHolder().key().location().getPath().equals(id), "Wrong result for " + id);
            }
        }
        helper.succeed();
    }

    // --- Scuba Gear ----------------------------------------------------------------------------------------------

    @GameTest(template = TEMPLATE)
    public static void scubaGearGivesNoProtection(GameTestHelper helper) {
        for (Item item : new Item[]{ModItems.SCUBA_MASK.get(), ModItems.SCUBA_TANK.get(), ModItems.SCUBA_WETSUIT.get(),
                ModItems.SCUBA_FLIPPERS.get()}) {
            ArmorItem armor = (ArmorItem) item;
            helper.assertTrue(armor.getDefense() == 0 && armor.getToughness() == 0.0F, item + " must give no protection");
            ItemStack stack = new ItemStack(item);
            helper.assertFalse(stack.getAttributeModifiers(armor.getEquipmentSlot()).containsKey(Attributes.ARMOR),
                    item + " must not add armor");
        }
        helper.assertTrue(new ItemStack(ModItems.SCUBA_FLIPPERS.get()).getAttributeModifiers(EquipmentSlot.FEET)
                .containsKey(ForgeMod.SWIM_SPEED.get()), "Flippers add swim speed");
        helper.assertTrue(new ItemStack(ModItems.SCUBA_WETSUIT.get()).getAttributeModifiers(EquipmentSlot.LEGS)
                .containsKey(ForgeMod.SWIM_SPEED.get()), "Wetsuit adds swim speed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void scubaTankLetsYouBreatheUnderwater(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        LivingBreatheEvent without = new LivingBreatheEvent(player, false, 1, 4);
        MinecraftForge.EVENT_BUS.post(without);
        helper.assertFalse(without.canBreathe(), "No tank: no air");
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.SCUBA_TANK.get()));
        LivingBreatheEvent with = new LivingBreatheEvent(player, false, 1, 4);
        MinecraftForge.EVENT_BUS.post(with);
        helper.assertTrue(with.canBreathe(), "Air Tank should let the wearer breathe");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void scubaMaskRestoresFullMiningSpeedUnderwater(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        for (int y = 1; y <= 3; y++) {
            helper.setBlock(new BlockPos(2, y, 2), Blocks.WATER);
        }
        player.setOnGround(false);
        player.baseTick();
        helper.assertTrue(player.isInWater(), "Player should be in water");
        float base = 4.0F;
        helper.assertTrue(ScubaEvents.underwaterSpeed(player, base) == base, "No mask: speed unchanged");
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.SCUBA_MASK.get()));
        helper.assertTrue(ScubaArmorItem.wearing(player, ModItems.SCUBA_MASK.get()), "Mask worn");
        float masked = ScubaEvents.underwaterSpeed(player, base);
        helper.assertTrue(masked == base * 25.0F, "Mask should undo both /5 penalties, got " + masked);
        helper.succeed();
    }

    // --- helpers -------------------------------------------------------------------------------------------------

    private static SuperFurnaceBlockEntity place(GameTestHelper helper, Block block, ItemStack input, ItemStack fuel) {
        BlockState state = block.defaultBlockState();
        helper.setBlock(FURNACE_POS, state);
        SuperFurnaceBlockEntity furnace = (SuperFurnaceBlockEntity) helper.getBlockEntity(FURNACE_POS);
        furnace.setItem(0, input);
        furnace.setItem(1, fuel);
        return furnace;
    }

    private static void assertStack(GameTestHelper helper, ItemStack stack, Item item, int count) {
        helper.assertTrue(stack.is(item) && stack.getCount() == count, "Expected " + count + " " + item + ", found " + stack);
    }

    private static ServerPlayer makePlayer(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "relicweave-toolkit-test"));
        player.setGameMode(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        return player;
    }
}
