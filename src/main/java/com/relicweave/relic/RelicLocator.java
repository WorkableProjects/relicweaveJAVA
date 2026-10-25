package com.relicweave.relic;

import com.relicweave.config.RelicWeaveConfig;
import com.relicweave.compat.curios.CuriosCompat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.relicweave.item.RelicItem;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Finds charm-like items (0.0.2 charms and 0.0.3 relics) according to the server's {@code charmActivation} setting,
 * including Curios slots when Curios is installed. Safe to call on either side; on the client the config is the
 * synced server value.
 */
public final class RelicLocator {
    private RelicLocator() {}

    /** Every stack matching {@code filter} in a valid location. Callers must de-duplicate by type. */
    public static List<ItemStack> find(Player player, Predicate<ItemStack> filter) {
        List<ItemStack> found = new ArrayList<>();
        boolean inventory = mode() == RelicWeaveConfig.CharmActivation.INVENTORY_OR_EQUIPPED;
        if (inventory) {
            for (ItemStack stack : player.getInventory().items) {
                if (!stack.isEmpty() && filter.test(stack)) {
                    found.add(stack);
                }
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (!stack.isEmpty() && filter.test(stack)) {
                found.add(stack);
            }
        }
        found.addAll(CuriosCompat.findEquipped(player, filter));
        return found;
    }

    public static boolean has(Player player, Predicate<ItemStack> filter) {
        return !find(player, filter).isEmpty();
    }

    /** True if the player carries {@code type} in a valid location. */
    public static boolean has(Player player, RelicType type) {
        return has(player, stack -> stack.getItem() instanceof RelicItem relic && relic.getRelicType() == type);
    }

    /** Every relic type the player carries in a valid location, in {@link RelicType#values()} order. */
    public static Set<RelicType> carried(Player player) {
        Set<RelicType> types = EnumSet.noneOf(RelicType.class);
        for (ItemStack stack : find(player, s -> s.getItem() instanceof RelicItem)) {
            types.add(((RelicItem) stack.getItem()).getRelicType());
        }
        return types;
    }

    /** First stack of {@code type} in a valid location, or {@link ItemStack#EMPTY}. */
    public static ItemStack first(Player player, RelicType type) {
        List<ItemStack> stacks = find(player, s -> s.getItem() instanceof RelicItem relic && relic.getRelicType() == type);
        return stacks.isEmpty() ? ItemStack.EMPTY : stacks.get(0);
    }

    private static RelicWeaveConfig.CharmActivation mode() {
        // Before a world's server config loads (e.g. on the title screen) fall back to the default.
        return RelicWeaveConfig.SPEC.isLoaded() ? RelicWeaveConfig.CHARM_ACTIVATION.get()
                : RelicWeaveConfig.CharmActivation.INVENTORY_OR_EQUIPPED;
    }
}
