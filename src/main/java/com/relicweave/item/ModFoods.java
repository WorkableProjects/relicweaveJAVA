package com.relicweave.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

public final class ModFoods {
    public static final FoodProperties DIAMOND_STEAK = new FoodProperties.Builder()
            .nutrition(10).saturationMod(1.2F).meat().alwaysEat()
            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 200, 0), 1.0F)
            .effect(() -> new MobEffectInstance(MobEffects.ABSORPTION, 1200, 0), 1.0F)
            .build();

    public static final FoodProperties NETHERITE_STEAK = new FoodProperties.Builder()
            .nutrition(12).saturationMod(1.4F).meat().alwaysEat()
            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 200, 1), 1.0F)
            .effect(() -> new MobEffectInstance(MobEffects.ABSORPTION, 2400, 1), 1.0F)
            .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2400, 0), 1.0F)
            .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 0), 1.0F)
            .build();

    private ModFoods() {}
}
