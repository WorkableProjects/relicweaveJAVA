package com.relicweave.gametest;

import com.relicweave.RelicWeave;
import com.relicweave.registry.ModBlocks;
import com.relicweave.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Data-driven checks for the Celestial tier: recipes, ore loot, worldgen registration and tool/armor stats. */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CelestialGameTests {
    private static final String TEMPLATE = "platform";
    private static final List<String> RECIPES = List.of(
            "celestial_ingot", "celestial_block", "celestial_ingot_from_block",
            "celestial_shard_from_smelting", "celestial_shard_from_blasting", "celestial_infuser",
            "celestial_sword_smithing", "celestial_pickaxe_smithing", "celestial_axe_smithing",
            "celestial_shovel_smithing", "celestial_hoe_smithing", "celestial_helmet_smithing",
            "celestial_chestplate_smithing", "celestial_leggings_smithing", "celestial_boots_smithing");

    private CelestialGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void celestialRecipesLoad(GameTestHelper helper) {
        for (String name : RECIPES) {
            ResourceLocation id = RelicWeave.id("celestial/" + name);
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(id).isPresent(), "Missing recipe " + id);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void oreDropsShardsAndSilkTouchDropsOre(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack silk = new ItemStack(Items.DIAMOND_PICKAXE);
        silk.enchant(Enchantments.SILK_TOUCH, 1);
        for (BlockState ore : List.of(ModBlocks.CELESTIAL_ORE.get().defaultBlockState(),
                ModBlocks.NETHER_CELESTIAL_ORE.get().defaultBlockState())) {
            List<ItemStack> drops = Block.getDrops(ore, level, pos, null, null, pickaxe);
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(ModItems.CELESTIAL_SHARD.get()),
                    "Ore should drop a celestial shard, got " + drops);
            List<ItemStack> silkDrops = Block.getDrops(ore, level, pos, null, null, silk);
            helper.assertTrue(silkDrops.size() == 1 && silkDrops.get(0).is(ore.getBlock().asItem()),
                    "Silk touch should drop the ore itself, got " + silkDrops);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void orePlacedFeaturesRegistered(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        helper.assertTrue(registry.containsKey(RelicWeave.id("ore_celestial_end")), "End placed feature missing");
        helper.assertTrue(registry.containsKey(RelicWeave.id("ore_celestial_nether")), "Nether placed feature missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void celestialPickaxeMinesObsidianTier(GameTestHelper helper) {
        ItemStack pickaxe = new ItemStack(ModItems.CELESTIAL_PICKAXE.get());
        helper.assertTrue(pickaxe.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "Cannot mine obsidian");
        helper.assertTrue(pickaxe.isCorrectToolForDrops(Blocks.ANCIENT_DEBRIS.defaultBlockState()), "Cannot mine ancient debris");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void celestialChestplateStats(GameTestHelper helper) {
        ItemStack chest = new ItemStack(ModItems.CELESTIAL_CHESTPLATE.get());
        double armor = sum(chest, EquipmentSlot.CHEST, Attributes.ARMOR);
        double toughness = sum(chest, EquipmentSlot.CHEST, Attributes.ARMOR_TOUGHNESS);
        helper.assertTrue(armor == 9.0, "Chestplate defense should be 9, was " + armor);
        helper.assertTrue(toughness == 4.0, "Chestplate toughness should be 4, was " + toughness);
        helper.succeed();
    }

    private static double sum(ItemStack stack, EquipmentSlot slot, Attribute attribute) {
        double total = 0;
        for (AttributeModifier modifier : stack.getAttributeModifiers(slot).get(attribute)) {
            total += modifier.getAmount();
        }
        return total;
    }
}
