package com.relicweave.compat.curios;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.util.List;
import java.util.function.Predicate;

/**
 * Optional Curios integration. This class never references Curios types itself; {@link CuriosAccess} does, and it is
 * only class-loaded after confirming Curios is installed, so RelicWeave runs normally without Curios.
 * <p>
 * The {@code charm} slot is defined by data ({@code data/curios/curios/slots/charm.json} and
 * {@code data/relicweave/curios/entities/relicweave.json}) and items opt in through the {@code curios:charm} item tag.
 */
public final class CuriosCompat {
    public static final String MOD_ID = "curios";
    private static Boolean loaded;

    private CuriosCompat() {}

    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = ModList.get() != null && ModList.get().isLoaded(MOD_ID);
        }
        return loaded;
    }

    /** Stacks in the player's Curios slots that match {@code filter}; empty when Curios is absent. */
    public static List<ItemStack> findEquipped(Player player, Predicate<ItemStack> filter) {
        return isLoaded() ? CuriosAccess.findEquipped(player, filter) : List.of();
    }
}
