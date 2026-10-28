package com.relicweave.enchantment.vestige;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Marker effect from Archivist's Mark; the damage bonus lives in {@link ArchivistsMarkEvents}. */
public class ArchivistsMarkEffect extends MobEffect {
    public ArchivistsMarkEffect() {
        super(MobEffectCategory.HARMFUL, 0xE0B040);
    }
}
