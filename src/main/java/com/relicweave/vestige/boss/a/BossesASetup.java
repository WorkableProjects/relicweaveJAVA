package com.relicweave.vestige.boss.a;

import com.relicweave.RelicWeave;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Mod-bus wiring for the T11 bosses: attributes. Bosses are altar-summoned only, so they have no spawn placement. */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BossesASetup {
    private BossesASetup() {}

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(BossesA.THORNED_REGENT.get(), ThornedRegent.createAttributes().build());
        event.put(BossesA.CHOIR_UNHEARD.get(), ChoirUnheard.createAttributes().build());
    }
}
