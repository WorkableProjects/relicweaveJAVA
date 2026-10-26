package com.relicweave.vestige.boss.a;

import com.relicweave.RelicWeave;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Boss registry for Thorned Regent and Choir Unheard (T11): boss entity types, their projectiles/helper entities, spawn eggs and sounds. */
public final class BossesA {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RelicWeave.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RelicWeave.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RelicWeave.MOD_ID);

    // ---- Sounds (every telegraph cue; each has a subtitle) ----
    public static final RegistryObject<SoundEvent> REGENT_VOLLEY = sound("entity.thorned_regent.volley");
    public static final RegistryObject<SoundEvent> REGENT_WALL = sound("entity.thorned_regent.wall");
    public static final RegistryObject<SoundEvent> REGENT_STRIKE = sound("entity.thorned_regent.strike");
    public static final RegistryObject<SoundEvent> REGENT_CALL = sound("entity.thorned_regent.call");
    public static final RegistryObject<SoundEvent> CHOIR_LANCE = sound("entity.choir_unheard.lance");
    public static final RegistryObject<SoundEvent> CHOIR_ZONES = sound("entity.choir_unheard.zones");
    public static final RegistryObject<SoundEvent> CHOIR_DETONATE = sound("entity.choir_unheard.detonate");
    public static final RegistryObject<SoundEvent> CHOIR_TONE_HIGH = sound("entity.choir_unheard.tone_high");
    public static final RegistryObject<SoundEvent> CHOIR_TONE_LOW = sound("entity.choir_unheard.tone_low");

    // ---- Entity types ----
    public static final RegistryObject<EntityType<ThornedRegent>> THORNED_REGENT = ENTITIES.register("thorned_regent",
            () -> EntityType.Builder.of(ThornedRegent::new, MobCategory.MONSTER).sized(2.0F, 3.5F).fireImmune()
                    .clientTrackingRange(10).build("thorned_regent"));
    public static final RegistryObject<EntityType<ChoirUnheard>> CHOIR_UNHEARD = ENTITIES.register("choir_unheard",
            () -> EntityType.Builder.of(ChoirUnheard::new, MobCategory.MONSTER).sized(1.2F, 3.2F).fireImmune()
                    .clientTrackingRange(10).build("choir_unheard"));
    public static final RegistryObject<EntityType<ThornSpike>> THORN_SPIKE = ENTITIES.register("thorn_spike",
            () -> EntityType.Builder.<ThornSpike>of(ThornSpike::new, MobCategory.MISC).sized(0.4F, 0.4F).fireImmune()
                    .clientTrackingRange(8).updateInterval(2).build("thorn_spike"));
    public static final RegistryObject<EntityType<ThornRoot>> THORN_ROOT = ENTITIES.register("thorn_root",
            () -> EntityType.Builder.<ThornRoot>of(ThornRoot::new, MobCategory.MISC).sized(1.0F, 2.0F).fireImmune()
                    .clientTrackingRange(10).updateInterval(20).build("thorn_root"));

    // ---- Spawn eggs ----
    public static final RegistryObject<Item> THORNED_REGENT_EGG = egg("thorned_regent", THORNED_REGENT, 0x2E4E30, 0xE2BE5C);
    public static final RegistryObject<Item> CHOIR_UNHEARD_EGG = egg("choir_unheard", CHOIR_UNHEARD, 0x1E2646, 0x96D6C8);

    private BossesA() {}

    private static RegistryObject<SoundEvent> sound(String path) {
        return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(RelicWeave.id(path)));
    }

    private static <T extends Mob> RegistryObject<Item> egg(String id, RegistryObject<EntityType<T>> type, int background, int highlight) {
        return ITEMS.register(id + "_spawn_egg", () -> new ForgeSpawnEggItem(type, background, highlight, new Item.Properties()));
    }
}
