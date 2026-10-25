package com.relicweave.block.infuser;

import com.relicweave.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

// STUB (wave 0): replaced by task T05.
public class CelestialInfuserMenu extends AbstractContainerMenu {
    public CelestialInfuserMenu(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        super(ModMenus.CELESTIAL_INFUSER.get(), containerId);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }
}
