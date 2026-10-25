package com.relicweave.recipe;

import com.relicweave.registry.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Input + catalyst (each with a required count) -> enchanted book carrying {@code enchantment} at {@code level}.
 * Matches against a container whose slot {@link #INPUT_SLOT} is the input and {@link #CATALYST_SLOT} the catalyst.
 */
public class TransmutationRecipe implements Recipe<Container> {
    public static final int INPUT_SLOT = 0;
    public static final int CATALYST_SLOT = 1;

    private final ResourceLocation id;
    private final Ingredient input;
    private final int inputCount;
    private final Ingredient catalyst;
    private final int catalystCount;
    private final ResourceLocation enchantmentId;
    private final int level;
    private final int processTime;

    public TransmutationRecipe(ResourceLocation id, Ingredient input, int inputCount, Ingredient catalyst, int catalystCount,
                               ResourceLocation enchantmentId, int level, int processTime) {
        this.id = id;
        this.input = input;
        this.inputCount = inputCount;
        this.catalyst = catalyst;
        this.catalystCount = catalystCount;
        this.enchantmentId = enchantmentId;
        this.level = level;
        this.processTime = processTime;
    }

    @Override
    public boolean matches(Container container, Level level) {
        ItemStack in = container.getItem(INPUT_SLOT);
        ItemStack cat = container.getItem(CATALYST_SLOT);
        return input.test(in) && in.getCount() >= inputCount
                && catalyst.test(cat) && cat.getCount() >= catalystCount;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return createResult();
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return createResult();
    }

    /** Builds a fresh enchanted book; empty if the enchantment is no longer registered. */
    public ItemStack createResult() {
        Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
        if (enchantment == null) {
            return ItemStack.EMPTY;
        }
        return EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, level));
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, input, catalyst);
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
        return ModRecipes.TRANSMUTATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.TRANSMUTATION_TYPE.get();
    }

    public Ingredient getInput() { return input; }
    public int getInputCount() { return inputCount; }
    public Ingredient getCatalyst() { return catalyst; }
    public int getCatalystCount() { return catalystCount; }
    public ResourceLocation getEnchantmentId() { return enchantmentId; }
    public int getLevel() { return level; }
    public int getProcessTime() { return processTime; }

    /** True if any loaded transmutation recipe accepts {@code stack} as its input. */
    public static boolean isAnyInput(Level level, ItemStack stack) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.TRANSMUTATION_TYPE.get()).stream()
                .anyMatch(r -> r.input.test(stack));
    }

    /** True if any loaded transmutation recipe accepts {@code stack} as its catalyst. */
    public static boolean isAnyCatalyst(Level level, ItemStack stack) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.TRANSMUTATION_TYPE.get()).stream()
                .anyMatch(r -> r.catalyst.test(stack));
    }
}
