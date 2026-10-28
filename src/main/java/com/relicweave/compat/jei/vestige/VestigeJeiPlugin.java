package com.relicweave.compat.jei.vestige;

import com.relicweave.RelicWeave;
import com.relicweave.relic.RelicAugment;
import com.relicweave.vestige.boss.BossRegistry;
import com.relicweave.vestige.gear.GearRegistry;
import com.relicweave.vestige.registry.VestigeMaterials;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.List;

/** JEI integration for 0.0.4 gear: the Reliquary Loom category and Aetherforged info pages. */
@JeiPlugin
public class VestigeJeiPlugin implements IModPlugin {
    public static final RecipeType<RelicAugment> LOOM = RecipeType.create(RelicWeave.MOD_ID, "reliquary_loom", RelicAugment.class);

    /** Items that are not crafted (or only partly): JEI shows where they come from. */
    private static final List<String> SOURCE_INFO = List.of(
            "vestige_stone",
            "aetherite_ore",
            "riftglass_cluster",
            "starfall_slag",
            "glassroot_log",
            "canopy_log",
            "echo_clay",
            "aether_down",
            "sovereign_thorn",
            "liminal_chord",
            "gravity_heart",
            "starforged_core",
            "concordant_seal",
            "prime_mnemonic_core",
            "riftglass",
            "ward_salve",
            "threshold_sigil",
            "boss_altar",
            "seal_gate",
            "ancient_terminal",
            "rift_fissure",
            "null_ward",
            "return_waystone");

    @Override
    public ResourceLocation getPluginUid() {
        return RelicWeave.id("jei_vestige");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new ReliquaryLoomCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // Only augments whose relic item exists (the six Vestige relics are registered by a separate task).
        registration.addRecipes(LOOM, Arrays.stream(RelicAugment.values())
                .filter(a -> BuiltInRegistries.ITEM.get(a.relic().itemId()) != Items.AIR).toList());

        registration.addItemStackInfo(new ItemStack(VestigeMaterials.CONCORDANT_ALLOY.get()),
                Component.translatable("jei.relicweave.info.concordant_alloy"));
        registration.addItemStackInfo(new ItemStack(BossRegistry.VESTIGE_UPGRADE_TEMPLATE.get()),
                Component.translatable("jei.relicweave.info.vestige_upgrade_template"));
        List<ItemStack> aetherforged = List.of(GearRegistry.AETHERFORGED_SWORD, GearRegistry.AETHERFORGED_PICKAXE,
                        GearRegistry.AETHERFORGED_AXE, GearRegistry.AETHERFORGED_SHOVEL, GearRegistry.AETHERFORGED_HOE,
                        GearRegistry.AETHERFORGED_HELMET, GearRegistry.AETHERFORGED_CHESTPLATE,
                        GearRegistry.AETHERFORGED_LEGGINGS, GearRegistry.AETHERFORGED_BOOTS)
                .stream().map(o -> new ItemStack((Item) o.get())).toList();
        registration.addItemStackInfo(aetherforged, Component.translatable("jei.relicweave.info.aetherforged"));
        registration.addItemStackInfo(new ItemStack(GearRegistry.RELIQUARY_LOOM_ITEM.get()),
                Component.translatable("jei.relicweave.info.reliquary_loom"));
        for (String id : SOURCE_INFO) {
            Item item = BuiltInRegistries.ITEM.get(RelicWeave.id(id));
            if (item != Items.AIR) {
                registration.addItemStackInfo(new ItemStack(item), Component.translatable("jei.relicweave.info.source." + id));
            }
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(GearRegistry.RELIQUARY_LOOM_ITEM.get()), LOOM);
    }
}
