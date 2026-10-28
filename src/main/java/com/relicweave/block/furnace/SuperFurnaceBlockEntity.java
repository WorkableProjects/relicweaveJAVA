package com.relicweave.block.furnace;

import com.relicweave.RelicWeave;
import com.relicweave.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Super Furnace: a Furnace (same slots, menu, recipe book, hoppers and experience) whose tier makes it faster, more
 * fuel-efficient and, from Diamond up, multiplies the output of ores and raw materials. Uses its own tick instead of
 * {@link AbstractFurnaceBlockEntity#serverTick}; the vanilla fields are reached through {@link #dataAccess}.
 */
public class SuperFurnaceBlockEntity extends AbstractFurnaceBlockEntity {
    /** Inputs whose smelting output is multiplied by {@link SuperFurnaceTier#rawOutput()}. */
    public static final TagKey<Item> MULTIPLIED = TagKey.create(Registries.ITEM, RelicWeave.id("super_furnace_multiplied"));

    private static final int LIT_TIME = 0;
    private static final int LIT_DURATION = 1;
    private static final int COOKING_PROGRESS = 2;
    private static final int COOKING_TOTAL_TIME = 3;
    /** Flame shown while a fuel-free (Celestial) furnace works. */
    private static final int FUEL_FREE_FLAME = 100;

    private final SuperFurnaceTier tier;
    private final RecipeManager.CachedCheck<net.minecraft.world.Container, SmeltingRecipe> quickCheck =
            RecipeManager.createCheck(RecipeType.SMELTING);

    public SuperFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUPER_FURNACE.get(), pos, state, RecipeType.SMELTING);
        this.tier = state.getBlock() instanceof SuperFurnaceBlock block ? block.tier() : SuperFurnaceTier.IRON;
    }

    public SuperFurnaceTier tier() {
        return tier;
    }

    /** Exposes the synced furnace values (lit time, lit duration, progress, total) for tests. */
    public ContainerData data() {
        return dataAccess;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.relicweave." + tier.blockName());
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new FurnaceMenu(id, inventory, this, dataAccess);
    }

    @Override
    protected int getBurnDuration(ItemStack fuel) {
        return tier.needsFuel() ? tier.burnTime(super.getBurnDuration(fuel)) : 0;
    }

    /** How many items one smelt of {@code input} produces at this tier, as a multiple of the recipe result. */
    public int outputMultiplier(ItemStack input) {
        return input.is(MULTIPLIED) ? tier.rawOutput() : 1;
    }

    private boolean isLit() {
        return dataAccess.get(LIT_TIME) > 0;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SuperFurnaceBlockEntity be) {
        ContainerData data = be.dataAccess;
        boolean wasLit = be.isLit();
        boolean changed = false;
        ItemStack input = be.items.get(SLOT_INPUT);
        ItemStack fuel = be.items.get(SLOT_FUEL);
        boolean hasInput = !input.isEmpty();
        SmeltingRecipe recipe = hasInput ? be.quickCheck.getRecipeFor(be, level).orElse(null) : null;
        boolean canBurn = be.canBurn(level.registryAccess(), recipe);
        if (recipe != null) {
            // Keep the total in step with this tier (setItem() resets it to the Furnace time).
            int total = be.tier.cookTime(recipe.getCookingTime());
            if (data.get(COOKING_TOTAL_TIME) != total) {
                data.set(COOKING_TOTAL_TIME, total);
                data.set(COOKING_PROGRESS, Math.min(data.get(COOKING_PROGRESS), total - 1));
            }
        }

        if (!be.tier.needsFuel()) {
            int flame = canBurn ? FUEL_FREE_FLAME : 0;
            data.set(LIT_TIME, flame);
            data.set(LIT_DURATION, FUEL_FREE_FLAME);
        } else {
            if (be.isLit()) {
                data.set(LIT_TIME, data.get(LIT_TIME) - 1);
            }
            if (!be.isLit() && !fuel.isEmpty() && canBurn) {
                int burn = be.getBurnDuration(fuel);
                data.set(LIT_TIME, burn);
                data.set(LIT_DURATION, burn);
                if (be.isLit()) {
                    changed = true;
                    if (fuel.hasCraftingRemainingItem()) {
                        be.items.set(SLOT_FUEL, fuel.getCraftingRemainingItem());
                    } else {
                        fuel.shrink(1);
                        if (fuel.isEmpty()) {
                            be.items.set(SLOT_FUEL, fuel.getCraftingRemainingItem());
                        }
                    }
                }
            }
        }

        if (be.isLit() && canBurn) {
            int progress = data.get(COOKING_PROGRESS) + 1;
            if (progress >= data.get(COOKING_TOTAL_TIME)) {
                progress = 0;
                if (be.burn(level.registryAccess(), recipe)) {
                    for (int i = 0; i < be.tier.experience(); i++) {
                        be.setRecipeUsed(recipe);
                    }
                }
                changed = true;
            }
            data.set(COOKING_PROGRESS, progress);
        } else if (!be.isLit() && data.get(COOKING_PROGRESS) > 0) {
            data.set(COOKING_PROGRESS, Math.max(0, Math.min(data.get(COOKING_PROGRESS) - 2, data.get(COOKING_TOTAL_TIME))));
        } else if (be.isLit() || !hasInput) {
            data.set(COOKING_PROGRESS, 0);
        }

        if (wasLit != be.isLit()) {
            changed = true;
            state = state.setValue(AbstractFurnaceBlock.LIT, be.isLit());
            level.setBlock(pos, state, 3);
        }
        if (changed) {
            setChanged(level, pos, state);
        }
    }

    private ItemStack result(RegistryAccess access, SmeltingRecipe recipe) {
        ItemStack result = recipe.assemble(this, access);
        if (!result.isEmpty()) {
            result.setCount(result.getCount() * outputMultiplier(items.get(SLOT_INPUT)));
        }
        return result;
    }

    private boolean canBurn(RegistryAccess access, SmeltingRecipe recipe) {
        if (items.get(SLOT_INPUT).isEmpty() || recipe == null) {
            return false;
        }
        ItemStack result = result(access, recipe);
        if (result.isEmpty()) {
            return false;
        }
        ItemStack out = items.get(SLOT_RESULT);
        if (out.isEmpty()) {
            return result.getCount() <= Math.min(getMaxStackSize(), result.getMaxStackSize());
        }
        if (!ItemStack.isSameItemSameTags(out, result)) {
            return false;
        }
        int total = out.getCount() + result.getCount();
        return total <= getMaxStackSize() && total <= out.getMaxStackSize();
    }

    private boolean burn(RegistryAccess access, SmeltingRecipe recipe) {
        if (!canBurn(access, recipe)) {
            return false;
        }
        ItemStack input = items.get(SLOT_INPUT);
        ItemStack result = result(access, recipe);
        ItemStack out = items.get(SLOT_RESULT);
        if (out.isEmpty()) {
            items.set(SLOT_RESULT, result);
        } else {
            out.grow(result.getCount());
        }
        if (input.is(Blocks.WET_SPONGE.asItem()) && items.get(SLOT_FUEL).is(Items.BUCKET)) {
            items.set(SLOT_FUEL, new ItemStack(Items.WATER_BUCKET));
        }
        input.shrink(1);
        return true;
    }
}
