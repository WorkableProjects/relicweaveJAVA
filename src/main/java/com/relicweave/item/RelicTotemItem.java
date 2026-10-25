package com.relicweave.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RelicTotemItem extends Item {
    private final TotemTier tier;

    public RelicTotemItem(TotemTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public TotemTier getTier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable((tier.worksInMainHand() ? "tooltip.relicweave.totem.either_hand" : "tooltip.relicweave.totem.offhand")
                + (RelicTooltips.totemsInCharmSlot() ? "_or_slot" : ""))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(getDescriptionId() + ".passive").withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable(getDescriptionId() + ".save").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.relicweave.totem.cooldown", tier.cooldownTicks() / 20).withStyle(ChatFormatting.DARK_GRAY));
    }
}
