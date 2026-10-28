package com.relicweave.vestige.entity.ethereal;

import com.relicweave.RelicWeave;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** T08 flying, ethereal and guardian Vestige mobs: entity types, spawn eggs and their own sound events. */
public final class EtherealMobs {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RelicWeave.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RelicWeave.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RelicWeave.MOD_ID);

    public static final RegistryObject<EntityType<FractureWisp>> FRACTURE_WISP = ENTITIES.register("fracture_wisp",
            () -> EntityType.Builder.<FractureWisp>of(FractureWisp::new, MobCategory.MONSTER)
                    .sized(0.6F, 0.8F).clientTrackingRange(8).build("fracture_wisp"));
    public static final RegistryObject<EntityType<Nullwing>> NULLWING = ENTITIES.register("nullwing",
            () -> EntityType.Builder.<Nullwing>of(Nullwing::new, MobCategory.MONSTER)
                    .sized(0.9F, 0.7F).clientTrackingRange(10).build("nullwing"));
    public static final RegistryObject<EntityType<EclipseMoth>> ECLIPSE_MOTH = ENTITIES.register("eclipse_moth",
            () -> EntityType.Builder.<EclipseMoth>of(EclipseMoth::new, MobCategory.AMBIENT)
                    .sized(0.6F, 0.4F).clientTrackingRange(8).build("eclipse_moth"));
    public static final RegistryObject<EntityType<ConcordanceGuardian>> CONCORDANCE_GUARDIAN = ENTITIES.register("concordance_guardian",
            () -> EntityType.Builder.<ConcordanceGuardian>of(ConcordanceGuardian::new, MobCategory.MONSTER)
                    .sized(1.6F, 3.1F).clientTrackingRange(10).build("concordance_guardian"));
    public static final RegistryObject<EntityType<SealedCustodian>> SEALED_CUSTODIAN = ENTITIES.register("sealed_custodian",
            () -> EntityType.Builder.<SealedCustodian>of(SealedCustodian::new, MobCategory.MONSTER)
                    .sized(1.2F, 2.4F).clientTrackingRange(10).build("sealed_custodian"));
    public static final RegistryObject<EntityType<RuneWarden>> RUNE_WARDEN = ENTITIES.register("rune_warden",
            () -> EntityType.Builder.<RuneWarden>of(RuneWarden::new, MobCategory.MONSTER)
                    .sized(1.0F, 2.5F).clientTrackingRange(10).build("rune_warden"));
    public static final RegistryObject<EntityType<CycleHerald>> CYCLE_HERALD = ENTITIES.register("cycle_herald",
            () -> EntityType.Builder.<CycleHerald>of(CycleHerald::new, MobCategory.MONSTER)
                    .sized(1.0F, 2.2F).clientTrackingRange(10).build("cycle_herald"));
    public static final RegistryObject<EntityType<RuneShard>> RUNE_SHARD = ENTITIES.register("rune_shard",
            () -> EntityType.Builder.<RuneShard>of(RuneShard::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F).noSummon().clientTrackingRange(4).updateInterval(2).build("rune_shard"));

    public static final RegistryObject<Item> FRACTURE_WISP_EGG = egg("fracture_wisp", FRACTURE_WISP, 0x54287E, 0xCCA0EC);
    public static final RegistryObject<Item> NULLWING_EGG = egg("nullwing", NULLWING, 0x18203E, 0x7870B0);
    public static final RegistryObject<Item> ECLIPSE_MOTH_EGG = egg("eclipse_moth", ECLIPSE_MOTH, 0xA8C0E0, 0xF7E196);
    public static final RegistryObject<Item> CONCORDANCE_GUARDIAN_EGG = egg("concordance_guardian", CONCORDANCE_GUARDIAN, 0xBFB49C, 0x60B2A6);
    public static final RegistryObject<Item> SEALED_CUSTODIAN_EGG = egg("sealed_custodian", SEALED_CUSTODIAN, 0x246868, 0xF7E196);
    public static final RegistryObject<Item> RUNE_WARDEN_EGG = egg("rune_warden", RUNE_WARDEN, 0x9E927C, 0xA268CC);
    public static final RegistryObject<Item> CYCLE_HERALD_EGG = egg("cycle_herald", CYCLE_HERALD, 0x54287E, 0xE4F0FA);

    // Wind-up cues (each has a subtitle in sounds.json / lang).
    public static final RegistryObject<SoundEvent> WISP_DASH = sound("entity.fracture_wisp.dash");
    public static final RegistryObject<SoundEvent> NULLWING_SCREECH = sound("entity.nullwing.screech");
    public static final RegistryObject<SoundEvent> GUARDIAN_SHOCKWAVE = sound("entity.concordance_guardian.shockwave");
    public static final RegistryObject<SoundEvent> GUARDIAN_RUNE_BEAM = sound("entity.concordance_guardian.rune_beam");
    public static final RegistryObject<SoundEvent> CUSTODIAN_SLAM = sound("entity.sealed_custodian.slam");
    public static final RegistryObject<SoundEvent> CUSTODIAN_SHIELD = sound("entity.sealed_custodian.shield");
    public static final RegistryObject<SoundEvent> WARDEN_AWAKEN = sound("entity.rune_warden.awaken");
    public static final RegistryObject<SoundEvent> WARDEN_BURST = sound("entity.rune_warden.burst");
    public static final RegistryObject<SoundEvent> WARDEN_VOLLEY = sound("entity.rune_warden.volley");
    public static final RegistryObject<SoundEvent> HERALD_LANCE = sound("entity.cycle_herald.lance");

    private EtherealMobs() {}

    private static RegistryObject<SoundEvent> sound(String path) {
        return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(RelicWeave.id(path)));
    }

    private static <T extends net.minecraft.world.entity.Mob> RegistryObject<Item> egg(String id, RegistryObject<EntityType<T>> type, int bg, int fg) {
        return ITEMS.register(id + "_spawn_egg", () -> new ForgeSpawnEggItem(type, bg, fg, new Item.Properties()));
    }
}
