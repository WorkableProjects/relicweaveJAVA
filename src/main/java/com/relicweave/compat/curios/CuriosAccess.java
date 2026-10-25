package com.relicweave.compat.curios;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.List;
import java.util.function.Predicate;

/** The only class that touches the Curios API. Load it only through {@link CuriosCompat}. */
final class CuriosAccess {
    private CuriosAccess() {}

    static List<ItemStack> findEquipped(Player player, Predicate<ItemStack> filter) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.findCurios(filter).stream().map(SlotResult::stack).toList())
                .orElse(List.of());
    }
}
