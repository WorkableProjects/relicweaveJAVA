package com.relicweave.vestige.gear;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the Loom's three slots. It deliberately exposes no item-handler capability, so hoppers and pipes can never
 * move the relic in or out: it only leaves through the player taking it (or the block being broken).
 */
public class ReliquaryLoomBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler items = new ItemStackHandler(ReliquaryLoomMenu.SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return ReliquaryLoomMenu.accepts(slot, stack);
        }
    };

    public ReliquaryLoomBlockEntity(BlockPos pos, BlockState state) {
        super(GearRegistry.RELIQUARY_LOOM_BE.get(), pos, state);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    /** Drops every stored stack (called when the block is broken). */
    public void dropContents(Level level, BlockPos pos) {
        SimpleContainer container = new SimpleContainer(items.getSlots());
        for (int i = 0; i < items.getSlots(); i++) {
            container.setItem(i, items.getStackInSlot(i));
        }
        Containers.dropContents(level, pos, container);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.relicweave.reliquary_loom");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ReliquaryLoomMenu(containerId, inventory, items, ContainerLevelAccess.create(level, worldPosition));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Items"));
    }
}
