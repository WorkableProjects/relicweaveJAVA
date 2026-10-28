package com.relicweave.vestige.entity.beast;

import com.relicweave.RelicWeave;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** T07 beast and creature Vestige mobs: entity types, spawn eggs and their own sound events. */
public final class BeastMobs {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RelicWeave.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RelicWeave.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RelicWeave.MOD_ID);

    // ---- Sounds (wind-up cues; each has a subtitle) ----
    public static final RegistryObject<SoundEvent> STALKER_REVEAL = sound("entity.glassroot_stalker.reveal");
    public static final RegistryObject<SoundEvent> BRUTE_SLAM = sound("entity.shardback_brute.slam");
    public static final RegistryObject<SoundEvent> BRUTE_CHARGE = sound("entity.shardback_brute.charge");
    public static final RegistryObject<SoundEvent> STRIDER_CHARGE = sound("entity.starfall_strider.charge");
    public static final RegistryObject<SoundEvent> LEECH_PULL = sound("entity.gravity_leech.pull");
    public static final RegistryObject<SoundEvent> MIMIC_CREAK = sound("entity.archive_mimic.creak");

    // ---- Entity types ----
    public static final RegistryObject<EntityType<GlassrootStalker>> GLASSROOT_STALKER = ENTITIES.register("glassroot_stalker",
            () -> EntityType.Builder.of(GlassrootStalker::new, MobCategory.MONSTER).sized(0.7F, 1.5F)
                    .clientTrackingRange(8).build("glassroot_stalker"));
    public static final RegistryObject<EntityType<RuinHound>> RUIN_HOUND = ENTITIES.register("ruin_hound",
            () -> EntityType.Builder.of(RuinHound::new, MobCategory.MONSTER).sized(0.7F, 0.9F)
                    .clientTrackingRange(8).build("ruin_hound"));
    public static final RegistryObject<EntityType<AetherGrazer>> AETHER_GRAZER = ENTITIES.register("aether_grazer",
            () -> EntityType.Builder.of(AetherGrazer::new, MobCategory.CREATURE).sized(0.9F, 1.3F)
                    .clientTrackingRange(10).build("aether_grazer"));
    public static final RegistryObject<EntityType<ShardbackBrute>> SHARDBACK_BRUTE = ENTITIES.register("shardback_brute",
            () -> EntityType.Builder.of(ShardbackBrute::new, MobCategory.MONSTER).sized(1.6F, 2.2F)
                    .clientTrackingRange(10).build("shardback_brute"));
    public static final RegistryObject<EntityType<StarfallStrider>> STARFALL_STRIDER = ENTITIES.register("starfall_strider",
            () -> EntityType.Builder.of(StarfallStrider::new, MobCategory.MONSTER).sized(0.8F, 1.6F).fireImmune()
                    .clientTrackingRange(8).build("starfall_strider"));
    public static final RegistryObject<EntityType<GravityLeech>> GRAVITY_LEECH = ENTITIES.register("gravity_leech",
            () -> EntityType.Builder.of(GravityLeech::new, MobCategory.MONSTER).sized(1.0F, 0.8F)
                    .clientTrackingRange(8).build("gravity_leech"));
    public static final RegistryObject<EntityType<ArchiveMimic>> ARCHIVE_MIMIC = ENTITIES.register("archive_mimic",
            () -> EntityType.Builder.of(ArchiveMimic::new, MobCategory.MONSTER).sized(1.0F, 1.0F)
                    .clientTrackingRange(8).build("archive_mimic"));

    // ---- Spawn eggs ----
    public static final RegistryObject<Item> GLASSROOT_STALKER_EGG = egg("glassroot_stalker", GLASSROOT_STALKER, 0x96D6C8, 0x24686A);
    public static final RegistryObject<Item> RUIN_HOUND_EGG = egg("ruin_hound", RUIN_HOUND, 0x9E927C, 0x182040);
    public static final RegistryObject<Item> AETHER_GRAZER_EGG = egg("aether_grazer", AETHER_GRAZER, 0xE4EEFA, 0x9AAAC8);
    public static final RegistryObject<Item> SHARDBACK_BRUTE_EGG = egg("shardback_brute", SHARDBACK_BRUTE, 0x26325C, 0xA268CC);
    public static final RegistryObject<Item> STARFALL_STRIDER_EGG = egg("starfall_strider", STARFALL_STRIDER, 0xD2681E, 0x0E1226);
    public static final RegistryObject<Item> GRAVITY_LEECH_EGG = egg("gravity_leech", GRAVITY_LEECH, 0x54184E, 0x78409E);
    public static final RegistryObject<Item> ARCHIVE_MIMIC_EGG = egg("archive_mimic", ARCHIVE_MIMIC, 0x9C7228, 0xA268CC);

    private BeastMobs() {}

    private static RegistryObject<SoundEvent> sound(String path) {
        return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(RelicWeave.id(path)));
    }

    private static <T extends Mob> RegistryObject<Item> egg(String id, RegistryObject<EntityType<T>> type, int background, int highlight) {
        return ITEMS.register(id + "_spawn_egg", () -> new ForgeSpawnEggItem(type, background, highlight, new Item.Properties()));
    }
}
