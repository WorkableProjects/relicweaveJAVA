package com.relicweave.vestige.gear;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** The nine Aetherforged item classes; each only adds the perk tooltip line to its vanilla base. */
public final class AetherforgedItems {
    private AetherforgedItems() {}

    public static final class Sword extends SwordItem {
        public Sword(int damage, float speed, Properties properties) { super(AetherforgedMaterials.TIER, damage, speed, properties); }
        @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) { AetherforgedPerk.appendTooltip(tooltip); }
    }

    public static final class Pickaxe extends PickaxeItem {
        public Pickaxe(int damage, float speed, Properties properties) { super(AetherforgedMaterials.TIER, damage, speed, properties); }
        @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) { AetherforgedPerk.appendTooltip(tooltip); }
    }

    public static final class Axe extends AxeItem {
        public Axe(float damage, float speed, Properties properties) { super(AetherforgedMaterials.TIER, damage, speed, properties); }
        @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) { AetherforgedPerk.appendTooltip(tooltip); }
    }

    public static final class Shovel extends ShovelItem {
        public Shovel(float damage, float speed, Properties properties) { super(AetherforgedMaterials.TIER, damage, speed, properties); }
        @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) { AetherforgedPerk.appendTooltip(tooltip); }
    }

    public static final class Hoe extends HoeItem {
        public Hoe(int damage, float speed, Properties properties) { super(AetherforgedMaterials.TIER, damage, speed, properties); }
        @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) { AetherforgedPerk.appendTooltip(tooltip); }
    }

    public static final class Armor extends ArmorItem {
        public Armor(Type type, Properties properties) { super(AetherforgedMaterials.ARMOR, type, properties); }
        @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) { AetherforgedPerk.appendTooltip(tooltip); }
    }
}
