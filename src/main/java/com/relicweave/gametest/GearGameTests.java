package com.relicweave.gametest;

import com.mojang.authlib.GameProfile;
import com.relicweave.RelicWeave;
import com.relicweave.vestige.boss.BossRegistry;
import com.relicweave.vestige.gear.AetherforgedMaterials;
import com.relicweave.vestige.gear.AetherforgedPerk;
import com.relicweave.vestige.gear.GearRegistry;
import com.relicweave.vestige.hazard.HazardProtection;
import com.relicweave.vestige.registry.VestigeMaterials;
import com.relicweave.vestige.world.VestigeWorldBlocks;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/** Aetherforged gear: Netherite comparison, smithing upgrade, repair, alloy recipe, Vestige-attuned perk. */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GearGameTests {
    private static final String TEMPLATE = "platform";
    private static final ArmorItem.Type[] TYPES = ArmorItem.Type.values();

    private GearGameTests() {}

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1.0e-4;
    }

    @GameTest(template = TEMPLATE)
    public static void aetherforgedComparesToNetherite(GameTestHelper helper) {
        int netheriteUses = Tiers.NETHERITE.getUses();
        int uses = AetherforgedMaterials.TIER.getUses();
        helper.assertTrue(uses >= netheriteUses && uses <= netheriteUses * 1.25, "Tool durability within 1.0-1.25x Netherite: " + uses);
        helper.assertTrue(near(AetherforgedMaterials.TIER.getAttackDamageBonus(), Tiers.NETHERITE.getAttackDamageBonus()), "Attack damage bonus equal");
        helper.assertTrue(near(AetherforgedMaterials.TIER.getSpeed(), 9.5) && near(Tiers.NETHERITE.getSpeed(), 9.0), "Mining speed 9.5 vs 9");
        helper.assertTrue(AetherforgedMaterials.TIER.getEnchantmentValue() == 16 && Tiers.NETHERITE.getEnchantmentValue() == 15, "Enchantability 16 vs 15");
        helper.assertTrue(AetherforgedMaterials.TIER.getLevel() == Tiers.NETHERITE.getLevel(), "Same mining level");

        helper.assertTrue(near(AetherforgedMaterials.ARMOR.getToughness(), 3.5) && near(ArmorMaterials.NETHERITE.getToughness(), 3.0), "Toughness 3.5 vs 3");
        helper.assertTrue(AetherforgedMaterials.ARMOR.getEnchantmentValue() == 16 && ArmorMaterials.NETHERITE.getEnchantmentValue() == 15, "Armor enchantability");
        helper.assertTrue(AetherforgedMaterials.ARMOR.getKnockbackResistance() > ArmorMaterials.NETHERITE.getKnockbackResistance()
                && near(AetherforgedMaterials.ARMOR.getKnockbackResistance(), 0.12), "Knockback resistance 0.12");
        for (ArmorItem.Type type : TYPES) {
            helper.assertTrue(AetherforgedMaterials.ARMOR.getDefenseForType(type) == ArmorMaterials.NETHERITE.getDefenseForType(type), "Defense equal for " + type);
            int armorUses = AetherforgedMaterials.ARMOR.getDurabilityForType(type);
            int base = ArmorMaterials.NETHERITE.getDurabilityForType(type);
            helper.assertTrue(armorUses >= base && armorUses <= base * 1.25, "Armor durability for " + type + ": " + armorUses + " vs " + base);
        }
        SwordItem sword = (SwordItem) GearRegistry.AETHERFORGED_SWORD.get();
        helper.assertTrue(near(sword.getDamage(), ((SwordItem) Items.NETHERITE_SWORD).getDamage()), "Sword damage equals Netherite");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void repairIngredientIsConcordantAlloy(GameTestHelper helper) {
        for (var piece : List.of(GearRegistry.AETHERFORGED_SWORD, GearRegistry.AETHERFORGED_PICKAXE, GearRegistry.AETHERFORGED_AXE,
                GearRegistry.AETHERFORGED_SHOVEL, GearRegistry.AETHERFORGED_HOE, GearRegistry.AETHERFORGED_HELMET,
                GearRegistry.AETHERFORGED_CHESTPLATE, GearRegistry.AETHERFORGED_LEGGINGS, GearRegistry.AETHERFORGED_BOOTS)) {
            ItemStack stack = new ItemStack(piece.get());
            helper.assertTrue(piece.get().isValidRepairItem(stack, new ItemStack(VestigeMaterials.CONCORDANT_ALLOY.get())), "Alloy repairs " + piece.getId());
            helper.assertFalse(piece.get().isValidRepairItem(stack, new ItemStack(Items.NETHERITE_INGOT)), "Netherite does not repair " + piece.getId());
            helper.assertTrue(stack.getItem().isFireResistant(), "Fire resistant " + piece.getId());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void smithingKeepsEnchantmentsAndName(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getRecipeManager();
        ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
        sword.enchant(Enchantments.SHARPNESS, 3);
        sword.setHoverName(Component.literal("Dawnbreaker"));
        sword.setDamageValue(100);

        SimpleContainer container = new SimpleContainer(
                new ItemStack(BossRegistry.VESTIGE_UPGRADE_TEMPLATE.get()), sword, new ItemStack(VestigeMaterials.CONCORDANT_ALLOY.get()));
        SmithingTransformRecipe recipe = smithing(recipes, "sword");
        helper.assertTrue(recipe.matches(container, helper.getLevel()), "Netherite sword + template + alloy matches");
        ItemStack result = recipe.assemble(container, helper.getLevel().registryAccess());
        helper.assertTrue(result.is(GearRegistry.AETHERFORGED_SWORD.get()), "Result is the Aetherforged sword");
        helper.assertTrue(EnchantmentHelper.getEnchantments(result).getOrDefault(Enchantments.SHARPNESS, 0) == 3, "Sharpness III kept");
        helper.assertTrue("Dawnbreaker".equals(result.getHoverName().getString()), "Custom name kept");

        SimpleContainer wrongBase = new SimpleContainer(
                new ItemStack(BossRegistry.VESTIGE_UPGRADE_TEMPLATE.get()), new ItemStack(Items.DIAMOND_SWORD), new ItemStack(VestigeMaterials.CONCORDANT_ALLOY.get()));
        helper.assertFalse(recipe.matches(wrongBase, helper.getLevel()), "Diamond sword is not upgradable");

        for (String piece : List.of("pickaxe", "axe", "shovel", "hoe", "helmet", "chestplate", "leggings", "boots")) {
            SmithingTransformRecipe other = smithing(recipes, piece);
            Item netherite = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(new ResourceLocation("minecraft", "netherite_" + piece));
            SimpleContainer c = new SimpleContainer(new ItemStack(BossRegistry.VESTIGE_UPGRADE_TEMPLATE.get()), new ItemStack(netherite),
                    new ItemStack(VestigeMaterials.CONCORDANT_ALLOY.get()));
            helper.assertTrue(other.matches(c, helper.getLevel()), "Smithing recipe matches for " + piece);
            helper.assertTrue(other.assemble(c, helper.getLevel().registryAccess()).getItem()
                    == net.minecraft.core.registries.BuiltInRegistries.ITEM.get(RelicWeave.id("aetherforged_" + piece)), "Result for " + piece);
        }
        // Vanilla's own netherite upgrade must be untouched.
        helper.assertTrue(recipes.byKey(new ResourceLocation("minecraft", "netherite_sword_smithing")).isPresent(), "Vanilla netherite upgrade still exists");
        helper.succeed();
    }

    private static SmithingTransformRecipe smithing(RecipeManager recipes, String piece) {
        Recipe<?> recipe = recipes.byKey(RelicWeave.id("vestige/gear/aetherforged_" + piece + "_smithing")).orElseThrow();
        return (SmithingTransformRecipe) recipe;
    }

    @GameTest(template = TEMPLATE)
    public static void alloyRecipeAcceptsEachCatalyst(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getRecipeManager();
        CraftingRecipe recipe = (CraftingRecipe) recipes.byKey(RelicWeave.id("vestige/gear/concordant_alloy")).orElseThrow();
        for (Item catalyst : List.of(VestigeMaterials.SOVEREIGN_THORN.get(), VestigeMaterials.LIMINAL_CHORD.get(),
                VestigeMaterials.GRAVITY_HEART.get(), VestigeMaterials.STARFORGED_CORE.get())) {
            TransientCraftingContainer grid = grid();
            for (int i = 0; i < 4; i++) {
                grid.setItem(i, new ItemStack(VestigeMaterials.AETHERITE_INGOT.get()));
            }
            grid.setItem(4, new ItemStack(catalyst));
            helper.assertTrue(recipe.matches(grid, helper.getLevel()), "Alloy accepts " + catalyst);
            ItemStack out = recipe.assemble(grid, helper.getLevel().registryAccess());
            helper.assertTrue(out.is(VestigeMaterials.CONCORDANT_ALLOY.get()) && out.getCount() == 2, "Yields 2 alloy");
        }
        TransientCraftingContainer bad = grid();
        for (int i = 0; i < 4; i++) {
            bad.setItem(i, new ItemStack(VestigeMaterials.AETHERITE_INGOT.get()));
        }
        bad.setItem(4, new ItemStack(Items.NETHER_STAR));
        helper.assertFalse(recipe.matches(bad, helper.getLevel()), "Nether Star is not a catalyst");

        CraftingRecipe duplicate = (CraftingRecipe) recipes.byKey(RelicWeave.id("vestige/gear/vestige_upgrade_template_duplication")).orElseThrow();
        TransientCraftingContainer dup = grid();
        for (int i = 0; i < 9; i++) {
            dup.setItem(i, new ItemStack(Items.DIAMOND));
        }
        dup.setItem(1, new ItemStack(BossRegistry.VESTIGE_UPGRADE_TEMPLATE.get()));
        dup.setItem(4, new ItemStack(VestigeMaterials.CONCORDANT_ALLOY.get()));
        helper.assertTrue(duplicate.matches(dup, helper.getLevel()), "Template duplication matches");
        helper.assertTrue(duplicate.assemble(dup, helper.getLevel().registryAccess()).getCount() == 2, "Duplication yields 2");

        CraftingRecipe loom = (CraftingRecipe) recipes.byKey(RelicWeave.id("vestige/gear/reliquary_loom")).orElseThrow();
        TransientCraftingContainer loomGrid = grid();
        Item ingot = VestigeMaterials.AETHERITE_INGOT.get();
        Item bricks = VestigeWorldBlocks.VESTIGE_BRICKS.get().asItem();
        Item[] layout = {ingot, VestigeMaterials.MNEMONIC_FILAMENT.get(), ingot, bricks, Items.WHITE_WOOL, bricks, bricks, bricks, bricks};
        for (int i = 0; i < 9; i++) {
            loomGrid.setItem(i, new ItemStack(layout[i]));
        }
        helper.assertTrue(loom.matches(loomGrid, helper.getLevel()), "Loom recipe matches");
        helper.assertTrue(loom.assemble(loomGrid, helper.getLevel().registryAccess()).is(GearRegistry.RELIQUARY_LOOM_ITEM.get()), "Loom recipe result");
        helper.succeed();
    }

    private static TransientCraftingContainer grid() {
        return new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) { return true; }
        }, 3, 3);
    }

    @GameTest(template = TEMPLATE)
    public static void riftProtectionFromWornPieces(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        HazardProtection.Hazard rift = HazardProtection.Hazard.RIFT_SATURATION;
        helper.assertTrue(near(HazardProtection.reduction(player, rift), 0.0), "No pieces, no protection");
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(GearRegistry.AETHERFORGED_HELMET.get()));
        helper.assertTrue(near(HazardProtection.reduction(player, rift), 0.06), "One piece 0.06, got " + HazardProtection.reduction(player, rift));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(GearRegistry.AETHERFORGED_CHESTPLATE.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(GearRegistry.AETHERFORGED_LEGGINGS.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(GearRegistry.AETHERFORGED_BOOTS.get()));
        double full = HazardProtection.reduction(player, rift);
        helper.assertTrue(near(full, 0.24), "Four pieces 0.24, got " + full);
        helper.assertTrue(near(HazardProtection.reduction(player, HazardProtection.Hazard.GRAVITY_SHEAR), 0.0), "Only Rift Saturation is affected");
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        helper.assertTrue(near(HazardProtection.reduction(player, rift), 0.18), "Netherite piece adds nothing");
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            player.setItemSlot(slot, ItemStack.EMPTY);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void toolsMineVestigeBlocksFaster(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        var vestige = VestigeWorldBlocks.VESTIGE_STONE.get().defaultBlockState();
        var stone = Blocks.STONE.defaultBlockState();
        helper.assertTrue(AetherforgedPerk.boostsMining(new ItemStack(GearRegistry.AETHERFORGED_PICKAXE.get()), vestige), "Perk applies to vestige stone");
        helper.assertFalse(AetherforgedPerk.boostsMining(new ItemStack(GearRegistry.AETHERFORGED_PICKAXE.get()), stone), "Not to vanilla stone");
        helper.assertFalse(AetherforgedPerk.boostsMining(new ItemStack(Items.NETHERITE_PICKAXE), vestige), "Not to Netherite");
        helper.assertFalse(AetherforgedPerk.boostsMining(new ItemStack(GearRegistry.AETHERFORGED_AXE.get()), vestige), "Axe is not mineable-for vestige stone");

        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(GearRegistry.AETHERFORGED_PICKAXE.get()));
        float onVestige = player.getDestroySpeed(vestige);
        float onStone = player.getDestroySpeed(stone);
        helper.assertTrue(Math.abs(onVestige / onStone - AetherforgedPerk.MINING_SPEED_MULTIPLIER) < 1.0e-3,
                "15% faster through the break-speed event: " + onVestige + " vs " + onStone);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_PICKAXE));
        helper.assertTrue(Math.abs(player.getDestroySpeed(vestige) - player.getDestroySpeed(stone)) < 1.0e-3, "Netherite gets no bonus");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.succeed();
    }

    private static ServerPlayer makePlayer(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "relicweave-gear-test"));
        player.setGameMode(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        return player;
    }
}
