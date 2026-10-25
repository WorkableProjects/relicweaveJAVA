package com.relicweave.item;

import com.relicweave.infusion.InfusionType;
import com.relicweave.infusion.Infusions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.loading.FMLEnvironment;
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
            if (!infusion.hasAttribute()) {
                tooltip.add(Component.literal("  ").append(infusion.description()).withStyle(ChatFormatting.GRAY));
            }
        }
        if (FMLEnvironment.dist.isClient() && ClientKeys.shiftDown()) {
            for (InfusionType infusion : InfusionType.values()) {
                if (!infusion.hasAttribute() && infusion.fits(getEquipmentSlot()) && !infusions.contains(infusion)) {
                    tooltip.add(Component.translatable("tooltip.relicweave.infusion.fits", infusion.displayName())
                            .withStyle(ChatFormatting.AQUA));
                }
            }
            tooltip.add(Component.translatable("tooltip.relicweave.infusion.rule_limit").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.relicweave.infusion.rule_stacking").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.relicweave.hold_shift").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** Isolates the client-only {@code Screen} class so a dedicated server never loads it. */
    private static final class ClientKeys {
        static boolean shiftDown() {
            return net.minecraft.client.gui.screens.Screen.hasShiftDown();
        }
    }
}
