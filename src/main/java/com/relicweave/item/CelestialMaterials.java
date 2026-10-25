package com.relicweave.item;

import com.relicweave.RelicWeave;
import com.relicweave.registry.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.TierSortingRegistry;

import java.util.List;

/** Celestial: one step above Netherite. */
public final class CelestialMaterials {
    public static final Tier TIER = TierSortingRegistry.registerTier(new Tier() {
        @Override public int getUses() { return 2531; }
        @Override public float getSpeed() { return 10.0F; }
        @Override public float getAttackDamageBonus() { return 5.0F; }
        @Override public int getLevel() { return 4; }
        @Override public int getEnchantmentValue() { return 18; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(ModItems.CELESTIAL_INGOT.get()); }
    }, RelicWeave.id("celestial"), List.of(Tiers.NETHERITE), List.of());

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
            return base * 42;
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return switch (type) {
                case HELMET, BOOTS -> 4;
                case CHESTPLATE -> 9;
                case LEGGINGS -> 7;
            };
        }

        @Override public int getEnchantmentValue() { return 18; }
        @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_NETHERITE; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(ModItems.CELESTIAL_INGOT.get()); }
        /** Namespaced so the armor layer textures resolve to relicweave:textures/models/armor/celestial_layer_N.png. */
        @Override public String getName() { return RelicWeave.MOD_ID + ":celestial"; }
        @Override public float getToughness() { return 4.0F; }
        @Override public float getKnockbackResistance() { return 0.15F; }
    };

    private CelestialMaterials() {}
}
