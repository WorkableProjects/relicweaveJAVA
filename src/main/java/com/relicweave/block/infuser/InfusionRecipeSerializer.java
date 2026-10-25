package com.relicweave.block.infuser;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.relicweave.infusion.InfusionType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * JSON schema:
 * <pre>
 * {
 *   "type": "relicweave:infusion",
 *   "ingredient": { "item": "minecraft:iron_block" },
 *   "count": 1,                              (optional, default 1, 1-64)
 *   "infusion": "relicweave:golemed",
 *   "process_time": 200                      (ticks, optional, default 200)
 * }
 * </pre>
 * Malformed recipes throw a {@link JsonParseException}, which the recipe manager logs with the recipe ID.
 */
public class InfusionRecipeSerializer implements RecipeSerializer<InfusionRecipe> {
    public static final int DEFAULT_PROCESS_TIME = 200;
    /** Progress is synced to the client as a short. */
    public static final int MAX_PROCESS_TIME = Short.MAX_VALUE;

    @Override
    public InfusionRecipe fromJson(ResourceLocation id, JsonObject json) {
        if (!json.has("ingredient")) {
            throw new JsonParseException("Missing required field 'ingredient'");
        }
        Ingredient ingredient = Ingredient.fromJson(json.get("ingredient"), false);
        if (ingredient.isEmpty()) {
            throw new JsonParseException("Field 'ingredient' matches no items");
        }
        int count = parseInt(json, "count", 1, 1, 64);

        String infusionText = GsonHelper.getAsString(json, "infusion");
        ResourceLocation infusionId = ResourceLocation.tryParse(infusionText);
        InfusionType infusion = infusionId == null ? null : InfusionType.byId(infusionId);
        if (infusion == null) {
            throw new JsonParseException("Unknown infusion: " + infusionText);
        }
        int processTime = parseInt(json, "process_time", DEFAULT_PROCESS_TIME, 1, MAX_PROCESS_TIME);

        return new InfusionRecipe(id, ingredient, count, infusion, processTime);
    }

    private static int parseInt(JsonObject json, String key, int fallback, int min, int max) {
        int value = GsonHelper.getAsInt(json, key, fallback);
        if (value < min || value > max) {
            throw new JsonParseException("Field '" + key + "' must be between " + min + " and " + max + ", got " + value);
        }
        return value;
    }

    @Override
    public InfusionRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        Ingredient ingredient = Ingredient.fromNetwork(buf);
        int count = buf.readVarInt();
        ResourceLocation infusionId = buf.readResourceLocation();
        InfusionType infusion = InfusionType.byId(infusionId);
        if (infusion == null) {
            throw new IllegalStateException("Unknown infusion in recipe " + id + ": " + infusionId);
        }
        int processTime = buf.readVarInt();
        return new InfusionRecipe(id, ingredient, count, infusion, processTime);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, InfusionRecipe recipe) {
        recipe.getIngredient().toNetwork(buf);
        buf.writeVarInt(recipe.getCount());
        buf.writeResourceLocation(recipe.getInfusion().id());
        buf.writeVarInt(recipe.getProcessTime());
    }
}
