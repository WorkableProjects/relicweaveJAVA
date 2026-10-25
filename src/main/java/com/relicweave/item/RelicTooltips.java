package com.relicweave.item;

import com.relicweave.relic.RelicType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Tooltip lines for relics: effect, valid locations, activation mode, cooldown, and duplicate rule.
 * TODO(T04): implement.
 */
public final class RelicTooltips {
    private RelicTooltips() {}

    public static void append(RelicType type, ItemStack stack, List<Component> tooltip) {
    }
}
