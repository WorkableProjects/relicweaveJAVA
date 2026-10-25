package com.relicweave.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CharmItem extends Item {
    private final CharmType type;

    public CharmItem(CharmType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public CharmType getCharmType() {
        return type;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(RelicTooltips.locationLine());
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.BLUE));
    }
}
