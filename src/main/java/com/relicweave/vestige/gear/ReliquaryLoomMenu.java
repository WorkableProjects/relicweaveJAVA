package com.relicweave.vestige.gear;

import com.relicweave.quest.QuestEvents;
import com.relicweave.item.RelicItem;
import com.relicweave.relic.RelicAugment;
import com.relicweave.vestige.VestigeConfig;
import com.relicweave.vestige.registry.VestigeMaterials;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Menu of the Reliquary Loom. Button ids: {@link #BUTTON_PATH_A} (Exploration), {@link #BUTTON_PATH_B} (Stability),
 * {@link #BUTTON_REMOVE}. The server re-validates everything in {@link #clickMenuButton}; the screen uses the very same
 * {@link #unavailableReason} to grey buttons out, so what it shows is what the server will accept.
 */
public class ReliquaryLoomMenu extends AbstractContainerMenu {
    public static final int SLOT_RELIC = 0;
    public static final int SLOT_FILAMENT = 1;
    public static final int SLOT_MATERIAL = 2;
    public static final int SLOT_COUNT = 3;

    public static final int BUTTON_PATH_A = 0;
    public static final int BUTTON_PATH_B = 1;
    public static final int BUTTON_REMOVE = 2;

    private static final int INV_START = SLOT_COUNT;
    private static final int INV_END = INV_START + 27;
    private static final int HOTBAR_END = INV_END + 9;

    /** Screen geometry shared with the screen: slot positions and the inventory origin. */
    public static final int[] SLOT_X = {10, 10, 10};
    public static final int[] SLOT_Y = {22, 46, 70};
    public static final int INVENTORY_X = 40;
    public static final int INVENTORY_Y = 140;

    private final ContainerLevelAccess access;
    private final IItemHandler items;

    /** Client-side constructor, invoked from the menu-open packet. */
    public ReliquaryLoomMenu(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        this(containerId, inventory, new ItemStackHandler(SLOT_COUNT), ContainerLevelAccess.NULL);
    }

    public ReliquaryLoomMenu(int containerId, Inventory inventory, IItemHandler items, ContainerLevelAccess access) {
        super(GearRegistry.RELIQUARY_LOOM_MENU.get(), containerId);
        this.access = access;
        this.items = items;

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            final int index = slot;
            addSlot(new SlotItemHandler(items, slot, SLOT_X[slot], SLOT_Y[slot]) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return accepts(index, stack);
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
        }
    }

    /** Slot filter: relic slot takes only relics the Loom supports; the others take their cost items. */
    public static boolean accepts(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_RELIC -> supportedRelic(stack);
            case SLOT_FILAMENT -> stack.is(VestigeMaterials.MNEMONIC_FILAMENT.get());
            case SLOT_MATERIAL -> stack.is(VestigeMaterials.AETHERITE_INGOT.get()) || stack.is(VestigeMaterials.RIFTGLASS.get());
            default -> false;
        };
    }

    /** True if {@code stack} is a relic with at least one augment path. */
    public static boolean supportedRelic(ItemStack stack) {
        return stack.getItem() instanceof RelicItem relic && !RelicAugment.forRelic(relic.getRelicType()).isEmpty();
    }

    public ItemStack relic() { return items.getStackInSlot(SLOT_RELIC); }

    /** The augment {@code button} (path A/B) would apply to the current relic, if the relic is supported. */
    @Nullable
    public RelicAugment augmentFor(int button) {
        ItemStack relic = relic();
        if (!supportedRelic(relic) || (button != BUTTON_PATH_A && button != BUTTON_PATH_B)) {
            return null;
        }
        RelicAugment.Path path = button == BUTTON_PATH_A ? RelicAugment.Path.EXPLORATION : RelicAugment.Path.STABILITY;
        for (RelicAugment augment : RelicAugment.forRelic(((RelicItem) relic.getItem()).getRelicType())) {
            if (augment.path() == path) {
                return augment;
            }
        }
        return null;
    }

    public Optional<RelicAugment> currentAugment() {
        return RelicAugment.of(relic());
    }

    public int augmentFilamentCost() { return VestigeConfig.LOOM_AUGMENT_FILAMENT_COST.get(); }
    public int resetFilamentCost() { return VestigeConfig.LOOM_RESET_FILAMENT_COST.get(); }

    /** Why {@code button} cannot be pressed right now, or {@code null} if it can. Used by server and screen alike. */
    @Nullable
    public Component unavailableReason(int button) {
        ItemStack relic = relic();
        if (relic.isEmpty()) {
            return Component.translatable("gui.relicweave.loom.no_relic");
        }
        if (!supportedRelic(relic)) {
            return Component.translatable("gui.relicweave.loom.unsupported");
        }
        ItemStack filament = items.getStackInSlot(SLOT_FILAMENT);
        ItemStack material = items.getStackInSlot(SLOT_MATERIAL);
        if (button == BUTTON_REMOVE) {
            if (currentAugment().isEmpty()) {
                return Component.translatable("gui.relicweave.loom.nothing_to_remove");
            }
            return hasFilament(filament, resetFilamentCost()) ? null
                    : Component.translatable("gui.relicweave.loom.missing", resetFilamentCost(), VestigeMaterials.MNEMONIC_FILAMENT.get().getDescription());
        }
        RelicAugment augment = augmentFor(button);
        if (augment == null) {
            return Component.translatable("gui.relicweave.loom.unsupported");
        }
        if (augment.isOn(relic)) {
            return Component.translatable("gui.relicweave.loom.already_applied");
        }
        if (!hasFilament(filament, augmentFilamentCost())) {
            return Component.translatable("gui.relicweave.loom.missing", augmentFilamentCost(), VestigeMaterials.MNEMONIC_FILAMENT.get().getDescription());
        }
        Item needed = augment.material();
        if (!material.is(needed) || material.getCount() < RelicAugment.MATERIAL_COST) {
            return Component.translatable("gui.relicweave.loom.missing", RelicAugment.MATERIAL_COST, needed.getDescription());
        }
        return null;
    }

    private static boolean hasFilament(ItemStack filament, int cost) {
        return cost <= 0 || (filament.is(VestigeMaterials.MNEMONIC_FILAMENT.get()) && filament.getCount() >= cost);
    }

    /**
     * Server-side action. Re-validates relic support, the path and the costs, consumes exactly the costs and writes (or
     * clears) the augment tag. Nothing else on the stack changes.
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide || id < BUTTON_PATH_A || id > BUTTON_REMOVE || unavailableReason(id) != null) {
            return false;
        }
        ItemStack relic = relic();
        if (id == BUTTON_REMOVE) {
            take(SLOT_FILAMENT, resetFilamentCost());
            RelicAugment.clear(relic);
        } else {
            RelicAugment augment = augmentFor(id);
            take(SLOT_FILAMENT, augmentFilamentCost());
            take(SLOT_MATERIAL, RelicAugment.MATERIAL_COST);
            augment.apply(relic);
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                QuestEvents.fire(serverPlayer, QuestEvents.LOOM_AUGMENT);
            }
        }
        if (items instanceof ItemStackHandler handler) {
            handler.setStackInSlot(SLOT_RELIC, relic);
        }
        broadcastChanges();
        return true;
    }

    private void take(int slot, int count) {
        items.extractItem(slot, count, false);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < SLOT_COUNT) {
            if (!moveItemStackTo(stack, INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            for (int target = 0; target < SLOT_COUNT && !moved; target++) {
                if (accepts(target, stack)) {
                    moved = moveItemStackTo(stack, target, target + 1, false);
                }
            }
            if (!moved) {
                if (index < INV_END) {
                    if (!moveItemStackTo(stack, INV_END, HOTBAR_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!moveItemStackTo(stack, INV_START, INV_END, false)) {
                    return ItemStack.EMPTY;
                }
            }
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
        return stillValid(access, player, GearRegistry.RELIQUARY_LOOM.get());
    }
}
