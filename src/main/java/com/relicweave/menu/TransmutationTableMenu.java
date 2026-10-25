package com.relicweave.menu;

import com.relicweave.block.TransmutationTableBlockEntity;
import com.relicweave.recipe.TransmutationRecipe;
import com.relicweave.registry.ModBlocks;
import com.relicweave.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class TransmutationTableMenu extends AbstractContainerMenu {
    private static final int CONTAINER_SLOTS = TransmutationTableBlockEntity.SLOT_COUNT;
    private static final int INV_START = CONTAINER_SLOTS;
    private static final int INV_END = INV_START + 27;
    private static final int HOTBAR_END = INV_END + 9;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final Level level;

    /** Client-side constructor, invoked from the menu-open packet. Server state arrives through slot/data sync. */
    public TransmutationTableMenu(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        this(containerId, inventory, new ItemStackHandler(CONTAINER_SLOTS), ContainerLevelAccess.NULL,
                new SimpleContainerData(TransmutationTableBlockEntity.DATA_COUNT));
    }

    public TransmutationTableMenu(int containerId, Inventory inventory, IItemHandler items, ContainerLevelAccess access, ContainerData data) {
        super(ModMenus.TRANSMUTATION_TABLE.get(), containerId);
        checkContainerDataCount(data, TransmutationTableBlockEntity.DATA_COUNT);
        this.access = access;
        this.data = data;
        this.level = inventory.player.level();

        addSlot(new SlotItemHandler(items, TransmutationTableBlockEntity.SLOT_INPUT, 56, 17) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return TransmutationRecipe.isAnyInput(level, stack);
            }
        });
        addSlot(new SlotItemHandler(items, TransmutationTableBlockEntity.SLOT_CATALYST, 56, 53) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return TransmutationRecipe.isAnyCatalyst(level, stack);
            }
        });
        addSlot(new SlotItemHandler(items, TransmutationTableBlockEntity.SLOT_OUTPUT, 116, 35) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }

        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < CONTAINER_SLOTS) {
            // Table -> player inventory (hotbar last, like vanilla furnaces).
            if (!moveItemStackTo(stack, INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (TransmutationRecipe.isAnyInput(level, stack)
                && moveItemStackTo(stack, TransmutationTableBlockEntity.SLOT_INPUT, TransmutationTableBlockEntity.SLOT_INPUT + 1, false)) {
            // moved into input
        } else if (TransmutationRecipe.isAnyCatalyst(level, stack)
                && moveItemStackTo(stack, TransmutationTableBlockEntity.SLOT_CATALYST, TransmutationTableBlockEntity.SLOT_CATALYST + 1, false)) {
            // moved into catalyst
        } else if (index < INV_END) {
            if (!moveItemStackTo(stack, INV_END, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, INV_START, INV_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.TRANSMUTATION_TABLE.get());
    }

    public int getProgress() {
        return data.get(TransmutationTableBlockEntity.DATA_PROGRESS);
    }

    public int getMaxProgress() {
        return data.get(TransmutationTableBlockEntity.DATA_MAX_PROGRESS);
    }

    /** Arrow width in pixels for the progress indicator. */
    public int getScaledProgress(int width) {
        int max = getMaxProgress();
        int progress = getProgress();
        return max > 0 && progress > 0 ? Math.min(width, progress * width / max) : 0;
    }
}
