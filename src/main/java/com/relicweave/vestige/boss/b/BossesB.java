package com.relicweave.vestige.boss.b;

import com.relicweave.RelicWeave;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Boss registry for Gravity Maw and Starforged Colossus (T12): boss entity types, spawn eggs and sounds. */
public final class BossesB {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RelicWeave.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RelicWeave.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RelicWeave.MOD_ID);

    // ---- Sounds (telegraph cues; each has a subtitle) ----
    public static final RegistryObject<SoundEvent> MAW_INHALE = sound("entity.gravity_maw.inhale");
    public static final RegistryObject<SoundEvent> MAW_REPULSE = sound("entity.gravity_maw.repulse");
    public static final RegistryObject<SoundEvent> MAW_BITE = sound("entity.gravity_maw.bite");
    public static final RegistryObject<SoundEvent> MAW_DEBRIS = sound("entity.gravity_maw.debris");
    public static final RegistryObject<SoundEvent> COLOSSUS_OVERHEAT = sound("entity.starforged_colossus.overheat");
    public static final RegistryObject<SoundEvent> COLOSSUS_SLAM = sound("entity.starforged_colossus.slam");
    public static final RegistryObject<SoundEvent> COLOSSUS_HEAT_PULSE = sound("entity.starforged_colossus.heat_pulse");
    public static final RegistryObject<SoundEvent> COLOSSUS_BARRAGE = sound("entity.starforged_colossus.barrage");

    // ---- Entity types ----
    public static final RegistryObject<EntityType<GravityMaw>> GRAVITY_MAW = ENTITIES.register("gravity_maw",
            () -> EntityType.Builder.of(GravityMaw::new, MobCategory.MONSTER).sized(3.0F, 3.0F)
                    .clientTrackingRange(10).build("gravity_maw"));
    public static final RegistryObject<EntityType<StarforgedColossus>> STARFORGED_COLOSSUS = ENTITIES.register("starforged_colossus",
            () -> EntityType.Builder.of(StarforgedColossus::new, MobCategory.MONSTER).sized(3.0F, 5.0F).fireImmune()
                    .clientTrackingRange(10).build("starforged_colossus"));

    // ---- Spawn eggs ----
    public static final RegistryObject<Item> GRAVITY_MAW_EGG = ITEMS.register("gravity_maw_spawn_egg",
            () -> new ForgeSpawnEggItem(GRAVITY_MAW, 0x2A1448, 0xA268CC, new Item.Properties()));
    public static final RegistryObject<Item> STARFORGED_COLOSSUS_EGG = ITEMS.register("starforged_colossus_spawn_egg",
            () -> new ForgeSpawnEggItem(STARFORGED_COLOSSUS, 0x3A2A24, 0xF2A03C, new Item.Properties()));

    private BossesB() {}

    private static RegistryObject<SoundEvent> sound(String path) {
        return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(RelicWeave.id(path)));
    }
}
