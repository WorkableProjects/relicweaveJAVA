package com.relicweave.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * JSON schema:
 * <pre>
 * {
 *   "type": "relicweave:transmutation",
 *   "input": { "item": "minecraft:diamond" },     "input_count": 1,      (count optional, default 1)
 *   "catalyst": { "item": "minecraft:amethyst_shard" }, "catalyst_count": 1, (count optional, default 1)
 *   "enchantment": "relicweave:keen_edge",
 *   "level": 1,                                    (optional, default 1)
 *   "process_time": 100                            (ticks, optional, default 100)
 * }
 * </pre>
 * Malformed recipes throw a {@link JsonParseException}, which the recipe manager logs with the recipe ID.
 */
public class TransmutationRecipeSerializer implements RecipeSerializer<TransmutationRecipe> {
    public static final int DEFAULT_PROCESS_TIME = 100;
    /** Progress is synced to the client as a short. */
    public static final int MAX_PROCESS_TIME = Short.MAX_VALUE;

    @Override
    public TransmutationRecipe fromJson(ResourceLocation id, JsonObject json) {
        Ingredient input = parseIngredient(json, "input");
        Ingredient catalyst = parseIngredient(json, "catalyst");
        int inputCount = parseInt(json, "input_count", 1, 1, 64);
        int catalystCount = parseInt(json, "catalyst_count", 1, 1, 64);

        ResourceLocation enchantmentId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "enchantment"));
        if (enchantmentId == null) {
            throw new JsonParseException("Invalid enchantment ID: " + GsonHelper.getAsString(json, "enchantment"));
        }
        Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
        if (enchantment == null || !ForgeRegistries.ENCHANTMENTS.containsKey(enchantmentId)) {
            throw new JsonParseException("Unknown enchantment: " + enchantmentId);
        }
        int level = parseInt(json, "level", 1, 1, enchantment.getMaxLevel());
        int processTime = parseInt(json, "process_time", DEFAULT_PROCESS_TIME, 1, MAX_PROCESS_TIME);

        return new TransmutationRecipe(id, input, inputCount, catalyst, catalystCount, enchantmentId, level, processTime);
    }

    private static Ingredient parseIngredient(JsonObject json, String key) {
        if (!json.has(key)) {
            throw new JsonParseException("Missing required field '" + key + "'");
        }
        Ingredient ingredient = Ingredient.fromJson(json.get(key), false);
        if (ingredient.isEmpty()) {
            throw new JsonParseException("Field '" + key + "' matches no items");
        }
        return ingredient;
    }

    private static int parseInt(JsonObject json, String key, int fallback, int min, int max) {
        int value = GsonHelper.getAsInt(json, key, fallback);
        if (value < min || value > max) {
            throw new JsonParseException("Field '" + key + "' must be between " + min + " and " + max + ", got " + value);
        }
        return value;
    }

    @Override
    public TransmutationRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        Ingredient input = Ingredient.fromNetwork(buf);
        int inputCount = buf.readVarInt();
        Ingredient catalyst = Ingredient.fromNetwork(buf);
        int catalystCount = buf.readVarInt();
        ResourceLocation enchantmentId = buf.readResourceLocation();
        int level = buf.readVarInt();
        int processTime = buf.readVarInt();
        return new TransmutationRecipe(id, input, inputCount, catalyst, catalystCount, enchantmentId, level, processTime);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, TransmutationRecipe recipe) {
        recipe.getInput().toNetwork(buf);
        buf.writeVarInt(recipe.getInputCount());
        recipe.getCatalyst().toNetwork(buf);
        buf.writeVarInt(recipe.getCatalystCount());
        buf.writeResourceLocation(recipe.getEnchantmentId());
        buf.writeVarInt(recipe.getLevel());
        buf.writeVarInt(recipe.getProcessTime());
    }
}
