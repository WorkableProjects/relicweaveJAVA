package com.relicweave.registry;

import com.relicweave.RelicWeave;
import com.relicweave.block.infuser.InfusionRecipe;
import com.relicweave.block.infuser.InfusionRecipeSerializer;
import com.relicweave.recipe.TransmutationRecipe;
import com.relicweave.recipe.TransmutationRecipeSerializer;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, RelicWeave.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, RelicWeave.MOD_ID);

    /** Stable type ID: {@code relicweave:transmutation}. */
    public static final RegistryObject<RecipeType<TransmutationRecipe>> TRANSMUTATION_TYPE =
            RECIPE_TYPES.register("transmutation", () -> RecipeType.simple(RelicWeave.id("transmutation")));

    public static final RegistryObject<RecipeSerializer<TransmutationRecipe>> TRANSMUTATION_SERIALIZER =
            RECIPE_SERIALIZERS.register("transmutation", TransmutationRecipeSerializer::new);

    /** Stable type ID: {@code relicweave:infusion}. */
    public static final RegistryObject<RecipeType<InfusionRecipe>> INFUSION_TYPE =
            RECIPE_TYPES.register("infusion", () -> RecipeType.simple(RelicWeave.id("infusion")));

    public static final RegistryObject<RecipeSerializer<InfusionRecipe>> INFUSION_SERIALIZER =
            RECIPE_SERIALIZERS.register("infusion", InfusionRecipeSerializer::new);

    private ModRecipes() {}
}
