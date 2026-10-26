package com.relicweave.vestige.hazard.rift;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Debuff from a full Rift Saturation meter: slower, weaker, slower attacks. Never deals damage. */
public class RiftSicknessEffect extends MobEffect {
    public RiftSicknessEffect() {
        super(MobEffectCategory.HARMFUL, 0x7840A0);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, "4b1c5a2e-7d0a-4a52-9a53-6c0f2f6a1001", -0.15, AttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, "4b1c5a2e-7d0a-4a52-9a53-6c0f2f6a1002", -2.0, AttributeModifier.Operation.ADDITION);
        addAttributeModifier(Attributes.ATTACK_SPEED, "4b1c5a2e-7d0a-4a52-9a53-6c0f2f6a1003", -0.2, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
