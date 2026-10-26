package com.relicweave.vestige.gate;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Consumed to activate a Concordance Gate. Always shows the enchantment glint. */
public class ThresholdSigilItem extends Item {
    public ThresholdSigilItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.relicweave.threshold_sigil").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.relicweave.threshold_sigil.requires").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
