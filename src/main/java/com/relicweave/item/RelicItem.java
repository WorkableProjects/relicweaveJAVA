package com.relicweave.item;

import com.relicweave.relic.RelicType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A 0.0.3 relic. Behaviour lives in {@code com.relicweave.relic}; see {@link RelicType}. */
public class RelicItem extends Item {
    private final RelicType type;

    public RelicItem(RelicType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public RelicType getRelicType() {
        return type;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        RelicTooltips.append(type, stack, tooltip);
    }
}
