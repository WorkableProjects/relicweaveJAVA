package com.relicweave.block.infuser;

import com.relicweave.registry.ModRecipes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

// STUB (wave 0): replaced by task T05.
public class InfusionRecipe implements Recipe<Container> {
    private final ResourceLocation id;

    public InfusionRecipe(ResourceLocation id) {
        this.id = id;
    }

    @Override public boolean matches(Container container, Level level) { return false; }
    @Override public ItemStack assemble(Container container, RegistryAccess access) { return ItemStack.EMPTY; }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(RegistryAccess access) { return ItemStack.EMPTY; }
    @Override public boolean isSpecial() { return true; }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.INFUSION_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModRecipes.INFUSION_TYPE.get(); }
}
