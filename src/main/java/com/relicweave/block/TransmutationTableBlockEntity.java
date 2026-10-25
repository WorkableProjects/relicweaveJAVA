package com.relicweave.block;

import com.relicweave.menu.TransmutationTableMenu;
import com.relicweave.recipe.TransmutationRecipe;
import com.relicweave.recipe.TransmutationRecipeSerializer;
import com.relicweave.registry.ModBlockEntities;
import com.relicweave.registry.ModRecipes;
import com.relicweave.registry.ModSounds;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Holds the table's three slots and processing state. All inventory mutation happens in {@link #serverTick}.
 */
public class TransmutationTableBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_INPUT = TransmutationRecipe.INPUT_SLOT;
    public static final int SLOT_CATALYST = TransmutationRecipe.CATALYST_SLOT;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_COUNT = 3;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_MAX_PROGRESS = 1;
    /** {@link MachineStatus#ordinal()}; recomputed every tick, not saved. */
    public static final int DATA_STATUS = 2;
    /** Successful completions, wrapping at 32767 because data slots sync as shorts; the GUI flashes when it changes. */
    public static final int DATA_COMPLETIONS = 3;
    public static final int DATA_COUNT = 4;

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (level == null) {
                return slot != SLOT_OUTPUT;
            }
            return switch (slot) {
                case SLOT_INPUT -> TransmutationRecipe.isAnyInput(level, stack);
                case SLOT_CATALYST -> TransmutationRecipe.isAnyCatalyst(level, stack);
                default -> false;
            };
        }
    };
    private final RecipeWrapper recipeContainer = new RecipeWrapper(items);

    private int progress;
    private MachineStatus status = MachineStatus.IDLE;
    private int completions;
    private int maxProgress = TransmutationRecipeSerializer.DEFAULT_PROCESS_TIME;
    @Nullable
    private ResourceLocation activeRecipeId;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_MAX_PROGRESS -> maxProgress;
                case DATA_STATUS -> status.ordinal();
                case DATA_COMPLETIONS -> completions;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Client-side copies are synced by the menu; server state is never set from here.
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public TransmutationTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRANSMUTATION_TABLE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TransmutationTableBlockEntity table) {
        table.tick(level);
    }

    private void tick(Level level) {
        Optional<Pair<ResourceLocation, TransmutationRecipe>> match = level.getRecipeManager()
                .getRecipeFor(ModRecipes.TRANSMUTATION_TYPE.get(), recipeContainer, level, activeRecipeId);

        if (match.isEmpty()) {
            boolean filled = !items.getStackInSlot(SLOT_INPUT).isEmpty() && !items.getStackInSlot(SLOT_CATALYST).isEmpty();
            status = filled ? MachineStatus.MISSING_INGREDIENT : MachineStatus.IDLE;
            resetProgress();
            return;
        }

        ResourceLocation recipeId = match.get().getFirst();
        TransmutationRecipe recipe = match.get().getSecond();
        ItemStack result = recipe.createResult();
        if (result.isEmpty() || !canAcceptOutput(result)) {
            status = result.isEmpty() ? MachineStatus.MISSING_INGREDIENT : MachineStatus.OUTPUT_FULL;
            resetProgress();
            return;
        }

        if (!recipeId.equals(activeRecipeId)) {
            activeRecipeId = recipeId;
            progress = 0;
        }
        status = MachineStatus.WORKING;
        maxProgress = recipe.getProcessTime();
        progress++;

        if (progress >= maxProgress) {
            complete(recipe, result);
            progress = 0;
        }
        setChanged();
    }

    /**
     * Consumes ingredients and inserts the result. Only called after {@link TransmutationRecipe#matches} and
     * {@link #canAcceptOutput} succeeded in the same tick, so either everything happens or nothing does.
     */
    private void complete(TransmutationRecipe recipe, ItemStack result) {
        items.getStackInSlot(SLOT_INPUT).shrink(recipe.getInputCount());
        items.getStackInSlot(SLOT_CATALYST).shrink(recipe.getCatalystCount());

        ItemStack output = items.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            items.setStackInSlot(SLOT_OUTPUT, result);
        } else {
            output.grow(result.getCount());
        }
        // shrink()/grow() mutate stacks in place, so notify every slot explicitly.
        items.setStackInSlot(SLOT_INPUT, items.getStackInSlot(SLOT_INPUT));
        items.setStackInSlot(SLOT_CATALYST, items.getStackInSlot(SLOT_CATALYST));
        items.setStackInSlot(SLOT_OUTPUT, items.getStackInSlot(SLOT_OUTPUT));

        completions = (completions + 1) & 0x7FFF;
        if (level instanceof ServerLevel serverLevel) {
            BlockPos pos = getBlockPos();
            serverLevel.playSound(null, pos, ModSounds.TRANSMUTATION_COMPLETE.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                    24, 0.3, 0.2, 0.3, 0.6);
        }
    }

    private boolean canAcceptOutput(ItemStack result) {
        ItemStack output = items.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            return true;
        }
        int limit = Math.min(output.getMaxStackSize(), items.getSlotLimit(SLOT_OUTPUT));
        return ItemStack.isSameItemSameTags(output, result) && output.getCount() + result.getCount() <= limit;
    }

    private void resetProgress() {
        if (progress != 0 || activeRecipeId != null) {
            progress = 0;
            activeRecipeId = null;
            setChanged();
        }
    }

    public void dropContents(Level level, BlockPos pos) {
        SimpleContainer container = new SimpleContainer(items.getSlots());
        for (int i = 0; i < items.getSlots(); i++) {
            container.setItem(i, items.getStackInSlot(i));
        }
        Containers.dropContents(level, pos, container);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public int getProgress() {
        return progress;
    }

    public MachineStatus getStatus() {
        return status;
    }

    public int getCompletions() {
        return completions;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", items.serializeNBT());
        tag.putInt("progress", progress);
        tag.putInt("max_progress", maxProgress);
        if (activeRecipeId != null) {
            tag.putString("recipe", activeRecipeId.toString());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("inventory"));
        progress = Math.max(0, tag.getInt("progress"));
        maxProgress = tag.contains("max_progress") ? Math.max(1, tag.getInt("max_progress"))
                : TransmutationRecipeSerializer.DEFAULT_PROCESS_TIME;
        activeRecipeId = tag.contains("recipe") ? ResourceLocation.tryParse(tag.getString("recipe")) : null;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.relicweave.transmutation_table");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new TransmutationTableMenu(containerId, inventory, items,
                ContainerLevelAccess.create(player.level(), worldPosition), data);
    }
}
