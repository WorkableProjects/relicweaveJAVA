package com.relicweave.item;

import com.relicweave.infusion.InfusionType;
import com.relicweave.infusion.Infusions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Celestial armor: the only armor that accepts infusions (see {@link Infusions}). */
public class CelestialArmorItem extends ArmorItem {
    public CelestialArmorItem(Type type, Properties properties) {
        super(CelestialMaterials.ARMOR, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        List<InfusionType> infusions = Infusions.get(stack);
        tooltip.add(Component.translatable("tooltip.relicweave.infusions", infusions.size(), Infusions.MAX_INFUSIONS)
                .withStyle(ChatFormatting.GRAY));
        for (InfusionType infusion : infusions) {
            tooltip.add(Component.literal(" ").append(infusion.displayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
