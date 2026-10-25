package com.relicweave.infusion.effects;

import com.relicweave.RelicWeave;
import com.relicweave.infusion.InfusionType;
import com.relicweave.infusion.Infusions;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Anchored (boots): while crouching, incoming knockback is scaled by {@value #ANCHORED_KNOCKBACK_FACTOR} (75 % less).
 * Never fully cancelled, so the wearer stays movable.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class AnchoredEffect {
    public static final float ANCHORED_KNOCKBACK_FACTOR = 0.25F;

    private AnchoredEffect() {}

    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide
                && player.isCrouching() && Infusions.isWearing(player, InfusionType.ANCHORED)) {
            event.setStrength(event.getStrength() * ANCHORED_KNOCKBACK_FACTOR);
        }
    }
}
