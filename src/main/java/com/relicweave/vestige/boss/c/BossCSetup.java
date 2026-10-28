package com.relicweave.vestige.boss.c;

import com.relicweave.RelicWeave;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Mod-bus setup for the T13 bosses and their helper entities: attributes. */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BossCSetup {
    private BossCSetup() {}

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(BossesC.WARDEN_OF_SEALS.get(), WardenOfSeals.attributes().build());
        event.put(BossesC.ARCHIVIST_PRIME.get(), ArchivistPrime.attributes().build());
        event.put(BossesC.SEAL_NODE.get(), RuneNode.attributes(SealNode.HEALTH).build());
        event.put(BossesC.INDEX_NODE.get(), RuneNode.attributes(IndexNode.HEALTH).build());
    }
}
