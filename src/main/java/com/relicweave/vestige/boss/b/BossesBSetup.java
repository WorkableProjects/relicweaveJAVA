package com.relicweave.vestige.boss.b;

import com.relicweave.RelicWeave;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Mod-bus wiring for the T12 bosses: attributes. Bosses never spawn naturally, so there are no spawn placements. */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BossesBSetup {
    private BossesBSetup() {}

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(BossesB.GRAVITY_MAW.get(), GravityMaw.createAttributes().build());
        event.put(BossesB.STARFORGED_COLOSSUS.get(), StarforgedColossus.createAttributes().build());
    }
}
