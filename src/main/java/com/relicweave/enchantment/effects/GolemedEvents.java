package com.relicweave.enchantment.effects;

import com.relicweave.RelicWeave;
import com.relicweave.registry.ModEnchantments;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/** Golemed: extra armor and knockback resistance on the chestplate, added as item attribute modifiers. */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class GolemedEvents {
    public static final double ARMOR_PER_LEVEL = 1.0;
    public static final double KNOCKBACK_RESISTANCE_PER_LEVEL = 0.1;
    private static final UUID ARMOR_ID = UUID.fromString("5b0f2c4e-7a31-4d68-9c1e-2f6a8d3b7e91");
    private static final UUID KNOCKBACK_ID = UUID.fromString("c47d1a90-3e5b-4f82-a6d0-9b18e52c4f73");

    private GolemedEvents() {}

    @SubscribeEvent
    public static void onItemAttributes(ItemAttributeModifierEvent event) {
        if (event.getSlotType() != EquipmentSlot.CHEST) {
            return;
        }
        int level = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.GOLEMED.get(), event.getItemStack());
        if (level <= 0) {
            return;
        }
        event.addModifier(Attributes.ARMOR, new AttributeModifier(ARMOR_ID, "relicweave:golemed_armor",
                ARMOR_PER_LEVEL * level, AttributeModifier.Operation.ADDITION));
        event.addModifier(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(KNOCKBACK_ID, "relicweave:golemed_knockback",
                KNOCKBACK_RESISTANCE_PER_LEVEL * level, AttributeModifier.Operation.ADDITION));
    }
}
