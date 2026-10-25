package com.relicweave.block.infuser;

import com.relicweave.infusion.InfusionType;
import com.relicweave.infusion.Infusions;
import com.relicweave.item.CelestialArmorItem;
import com.relicweave.registry.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Celestial armor (slot {@link #ARMOR_SLOT}) + {@code count} of a material (slot {@link #MATERIAL_SLOT}) -> the same
 * armor with {@code infusion} added. The result depends on the armor, so it is built in {@link #assemble}.
 */
public class InfusionRecipe implements Recipe<Container> {
    public static final int ARMOR_SLOT = 0;
    public static final int MATERIAL_SLOT = 1;

    private final ResourceLocation id;
    private final Ingredient ingredient;
    private final int count;
    private final InfusionType infusion;
    private final int processTime;

    public InfusionRecipe(ResourceLocation id, Ingredient ingredient, int count, InfusionType infusion, int processTime) {
        this.id = id;
        this.ingredient = ingredient;
        this.count = count;
        this.infusion = infusion;
        this.processTime = processTime;
    }

    @Override
    public boolean matches(Container container, Level level) {
        ItemStack armor = container.getItem(ARMOR_SLOT);
        ItemStack material = container.getItem(MATERIAL_SLOT);
        return armor.getItem() instanceof CelestialArmorItem && Infusions.canInfuse(armor, infusion)
                && ingredient.test(material) && material.getCount() >= count;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        ItemStack armor = container.getItem(ARMOR_SLOT);
        return Infusions.canInfuse(armor, infusion) ? Infusions.withInfusion(armor, infusion) : ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    /** Depends on the input armor, so there is no fixed result to show. */
    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, ingredient);
    }

    /** Not shown in the vanilla recipe book (avoids "Unknown recipe category" warnings). */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.INFUSION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.INFUSION_TYPE.get();
    }

    public Ingredient getIngredient() { return ingredient; }
    public int getCount() { return count; }
    public InfusionType getInfusion() { return infusion; }
    public int getProcessTime() { return processTime; }

    /** True if any loaded infusion recipe accepts {@code stack} as its material. */
    public static boolean isAnyMaterial(Level level, ItemStack stack) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.INFUSION_TYPE.get()).stream()
                .anyMatch(r -> r.ingredient.test(stack));
    }
}
