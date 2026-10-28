package com.relicweave.vestige.gear;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.registry.VestigeMaterials;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.TierSortingRegistry;

import java.util.List;

/**
 * Aetherforged: a post-Netherite sidegrade (a little more durability, toughness and enchantability, plus the
 * Vestige-attuned perk). Celestial remains the strongest tier, so it is sorted after this one.
 */
public final class AetherforgedMaterials {
    public static final Tier TIER = TierSortingRegistry.registerTier(new Tier() {
        @Override public int getUses() { return 2400; }
        @Override public float getSpeed() { return 9.5F; }
        @Override public float getAttackDamageBonus() { return 4.0F; }
        @Override public int getLevel() { return 4; }
        @Override public int getEnchantmentValue() { return 16; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(VestigeMaterials.CONCORDANT_ALLOY.get()); }
    }, RelicWeave.id("aetherforged"), List.of(Tiers.NETHERITE), List.of(RelicWeave.id("celestial")));

    public static final ArmorMaterial ARMOR = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            // Vanilla per-slot base durability (13, 15, 16, 11) times the material multiplier (Netherite uses 37).
            int base = switch (type) {
                case HELMET -> 11;
                case CHESTPLATE -> 16;
                case LEGGINGS -> 15;
                case BOOTS -> 13;
            };
            return base * 40;
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return switch (type) {
                case HELMET, BOOTS -> 3;
                case CHESTPLATE -> 8;
                case LEGGINGS -> 6;
            };
        }

        @Override public int getEnchantmentValue() { return 16; }
        @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_NETHERITE; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(VestigeMaterials.CONCORDANT_ALLOY.get()); }
        /** Namespaced so the layer textures resolve to relicweave:textures/models/armor/aetherforged_layer_N.png. */
        @Override public String getName() { return RelicWeave.MOD_ID + ":aetherforged"; }
        @Override public float getToughness() { return 3.5F; }
        @Override public float getKnockbackResistance() { return 0.12F; }
    };

    private AetherforgedMaterials() {}
}
