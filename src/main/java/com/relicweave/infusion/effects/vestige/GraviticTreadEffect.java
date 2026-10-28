package com.relicweave.infusion.effects.vestige;

import com.relicweave.infusion.InfusionType;
import com.relicweave.infusion.Infusions;
import net.minecraft.world.entity.player.Player;

/**
 * Gravitic Tread (boots): contributes {@value #GRAVITY_SHEAR_PROTECTION} Gravity Shear protection. It registers no
 * contributor of its own: {@code GravityShearProtection} combines it with Riftwalker under one 0.5 cap.
 */
public final class GraviticTreadEffect {
    public static final double GRAVITY_SHEAR_PROTECTION = 0.30;

    private GraviticTreadEffect() {}

    /** 0.30 while the wearer has Gravitic Tread on their boots, else 0. */
    public static double protection(Player player) {
        return Infusions.isWearing(player, InfusionType.GRAVITIC_TREAD) ? GRAVITY_SHEAR_PROTECTION : 0.0;
    }
}
