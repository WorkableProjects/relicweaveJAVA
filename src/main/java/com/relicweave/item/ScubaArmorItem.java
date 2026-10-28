package com.relicweave.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.relicweave.RelicWeave;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Scuba Gear (0.0.4.5): four pieces with no armor protection at all that take the water out of the way.
 * <ul>
 *   <li>Mask: clear underwater vision, and mining at full speed underwater (even while floating);</li>
 *   <li>Air Tank: you never run out of air;</li>
 *   <li>Wetsuit and Flippers: faster swimming and underwater walking ({@code forge:swim_speed}).</li>
 * </ul>
 * Behaviour lives in {@link ScubaEvents} and the client fog hook.
 */
public class ScubaArmorItem extends ArmorItem {
    public static final ArmorMaterial MATERIAL = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(Type type) {
            int base = switch (type) {
                case HELMET -> 11;
                case CHESTPLATE -> 16;
                case LEGGINGS -> 15;
                case BOOTS -> 13;
            };
            return base * 12;
        }

        /** No protection: that is the trade-off. */
        @Override public int getDefenseForType(Type type) { return 0; }
        @Override public int getEnchantmentValue() { return 12; }
        @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_LEATHER; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(Items.DRIED_KELP); }
        @Override public String getName() { return RelicWeave.MOD_ID + ":scuba"; }
        @Override public float getToughness() { return 0.0F; }
        @Override public float getKnockbackResistance() { return 0.0F; }
    };

    private static final UUID LEGS_SWIM = UUID.fromString("5c2f7e1a-3b9d-4f6e-8a41-0d4b7c9e2a10");
    private static final UUID FEET_SWIM = UUID.fromString("9e41b3d2-7a6c-4c85-b1f0-6d2e8f3a5b27");

    public ScubaArmorItem(Type type, Properties properties) {
        super(MATERIAL, type, properties);
    }

    /** Swim speed bonus (added to the base 1.0) for this piece. */
    public double swimBonus() {
        return switch (type) {
            case LEGGINGS -> 0.5;
            case BOOTS -> 1.0;
            default -> 0.0;
        };
    }

    /** No armor or toughness modifiers, only swim speed on the Wetsuit and Flippers. */
    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot != getEquipmentSlot() || swimBonus() == 0.0) {
            return ImmutableMultimap.of();
        }
        UUID id = type == Type.LEGGINGS ? LEGS_SWIM : FEET_SWIM;
        return ImmutableMultimap.of(ForgeMod.SWIM_SPEED.get(),
                new AttributeModifier(id, "Scuba swim speed", swimBonus(), AttributeModifier.Operation.ADDITION));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.relicweave." + net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getKey(this).getPath()).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.relicweave.scuba.no_armor").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** True if {@code entity} wears {@code item} in its slot. */
    public static boolean wearing(LivingEntity entity, Item item) {
        return item instanceof ScubaArmorItem scuba && entity.getItemBySlot(scuba.getEquipmentSlot()).is(item);
    }
}
