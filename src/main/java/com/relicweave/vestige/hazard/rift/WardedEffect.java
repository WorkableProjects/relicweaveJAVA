package com.relicweave.vestige.hazard.rift;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Beneficial marker effect from a Ward Salve. Its protection is a {@code HazardProtection} contributor (see {@link RiftSetup}). */
public class WardedEffect extends MobEffect {
    public static final double RIFT_PROTECTION = 0.5;

    public WardedEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x3A8C86);
    }
}
