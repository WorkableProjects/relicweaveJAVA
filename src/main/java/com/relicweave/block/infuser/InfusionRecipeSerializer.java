package com.relicweave.block.infuser;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;

// STUB (wave 0): replaced by task T05.
public class InfusionRecipeSerializer implements RecipeSerializer<InfusionRecipe> {
    @Override public InfusionRecipe fromJson(ResourceLocation id, JsonObject json) { return new InfusionRecipe(id); }
    @Override public InfusionRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) { return new InfusionRecipe(id); }
    @Override public void toNetwork(FriendlyByteBuf buf, InfusionRecipe recipe) {}
}
