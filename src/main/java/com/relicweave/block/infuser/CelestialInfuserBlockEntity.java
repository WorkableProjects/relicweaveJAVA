package com.relicweave.block.infuser;

import com.mojang.datafixers.util.Pair;
import com.relicweave.block.MachineStatus;
import com.relicweave.item.CelestialArmorItem;
import com.relicweave.registry.ModBlockEntities;
import com.relicweave.registry.ModRecipes;
import com.relicweave.registry.ModSounds;
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
 * Holds the infuser's three slots and processing state. All inventory mutation happens in {@link #serverTick}.
 */
public class CelestialInfuserBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_ARMOR = InfusionRecipe.ARMOR_SLOT;
    public static final int SLOT_MATERIAL = InfusionRecipe.MATERIAL_SLOT;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_COUNT = 3;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_MAX_PROGRESS = 1;
    /** {@link MachineStatus#ordinal()}; recomputed every tick, not saved. */
    public static final int DATA_STATUS = 2;
    /** Successful completions, wrapping at 32767 because data slots sync as shorts; the GUI flashes when it changes. */
    public static final int DATA_COMPLETIONS = 3;
    public static final int DATA_COUNT = 4;
    /** Ticks between the resonance and the chime sound. */
    public static final int CHIME_DELAY_TICKS = 6;

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case SLOT_ARMOR -> stack.getItem() instanceof CelestialArmorItem;
                case SLOT_MATERIAL -> level == null || InfusionRecipe.isAnyMaterial(level, stack);
                default -> false;
            };
        }
    };
    private final RecipeWrapper recipeContainer = new RecipeWrapper(items);

    private int progress;
    private MachineStatus status = MachineStatus.IDLE;
    private int completions;
    /** Ticks until the completion chime plays; 0 = none pending. Transient: skipped if the block unloads. */
    private int chimeDelay;
    private int maxProgress = InfusionRecipeSerializer.DEFAULT_PROCESS_TIME;
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

    public CelestialInfuserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CELESTIAL_INFUSER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CelestialInfuserBlockEntity infuser) {
        infuser.tick(level);
    }

    private void tick(Level level) {
        // Armor does not stack, so a non-empty output always blocks the next operation.
        if (chimeDelay > 0 && --chimeDelay == 0) {
            level.playSound(null, worldPosition, ModSounds.INFUSER_CHIME.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
        }
        ItemStack armor = items.getStackInSlot(SLOT_ARMOR);
        ItemStack material = items.getStackInSlot(SLOT_MATERIAL);
        boolean inputsPresent = !armor.isEmpty() && !material.isEmpty();
        if (!items.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
            status = inputsPresent ? MachineStatus.OUTPUT_FULL : MachineStatus.IDLE;
            resetProgress();
            return;
        }
        Optional<Pair<ResourceLocation, InfusionRecipe>> match = level.getRecipeManager()
                .getRecipeFor(ModRecipes.INFUSION_TYPE.get(), recipeContainer, level, activeRecipeId);
        if (match.isEmpty()) {
            status = inputsPresent ? InfusionRecipe.diagnose(level, armor, material) : MachineStatus.IDLE;
            resetProgress();
            return;
        }

        ResourceLocation recipeId = match.get().getFirst();
        InfusionRecipe recipe = match.get().getSecond();
        ItemStack result = recipe.assemble(recipeContainer, level.registryAccess());
        if (result.isEmpty()) {
            status = MachineStatus.MISSING_INGREDIENT;
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
     * Consumes the armor and materials and fills the output. Only called after {@link InfusionRecipe#matches} and the
     * empty-output check succeeded in the same tick, so either everything happens or nothing does.
     */
    private void complete(InfusionRecipe recipe, ItemStack result) {
        items.getStackInSlot(SLOT_ARMOR).shrink(1);
        items.getStackInSlot(SLOT_MATERIAL).shrink(recipe.getCount());
        items.setStackInSlot(SLOT_OUTPUT, result);
        // shrink() mutates stacks in place, so notify the input slots explicitly.
        items.setStackInSlot(SLOT_ARMOR, items.getStackInSlot(SLOT_ARMOR));
        items.setStackInSlot(SLOT_MATERIAL, items.getStackInSlot(SLOT_MATERIAL));

        completions = (completions + 1) & 0x7FFF;
        if (level instanceof ServerLevel serverLevel) {
            BlockPos pos = getBlockPos();
            serverLevel.playSound(null, pos, ModSounds.INFUSER_RESONANCE.get(), SoundSource.BLOCKS, 0.6F, 0.8F);
            chimeDelay = CHIME_DELAY_TICKS;
            serverLevel.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    16, 0.3, 0.2, 0.3, 0.02);
        }
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
                : InfusionRecipeSerializer.DEFAULT_PROCESS_TIME;
        activeRecipeId = tag.contains("recipe") ? ResourceLocation.tryParse(tag.getString("recipe")) : null;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.relicweave.celestial_infuser");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CelestialInfuserMenu(containerId, inventory, items,
                ContainerLevelAccess.create(player.level(), worldPosition), data);
    }
}
