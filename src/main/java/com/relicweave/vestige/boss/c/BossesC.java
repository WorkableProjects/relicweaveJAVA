package com.relicweave.vestige.boss.c;

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

import java.util.function.Supplier;

/** Boss registry for Warden of Seals and Archivist-Prime (T13): boss entity types, their projectiles/helper entities, spawn eggs and sounds. */
public final class BossesC {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RelicWeave.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RelicWeave.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RelicWeave.MOD_ID);

    // ---- Sounds (wind-up cues; subtitles live in the sounds.json fragment) ----
    public static final RegistryObject<SoundEvent> WARDEN_SLAM = sound("entity.warden_of_seals.slam");
    public static final RegistryObject<SoundEvent> WARDEN_LANCES = sound("entity.warden_of_seals.lances");
    public static final RegistryObject<SoundEvent> WARDEN_SUMMON = sound("entity.warden_of_seals.summon");
    public static final RegistryObject<SoundEvent> SEAL_NODE_EXPOSED = sound("entity.seal_node.exposed");
    public static final RegistryObject<SoundEvent> PRIME_LANCES = sound("entity.archivist_prime.lances");
    public static final RegistryObject<SoundEvent> PRIME_VOLLEY = sound("entity.archivist_prime.volley");
    public static final RegistryObject<SoundEvent> PRIME_RINGS = sound("entity.archivist_prime.rings");
    public static final RegistryObject<SoundEvent> PRIME_CYCLE_SHIFT = sound("entity.archivist_prime.cycle_shift");
    public static final RegistryObject<SoundEvent> PRIME_FINAL_SEAL = sound("entity.archivist_prime.final_seal");

    // ---- Entity types ----
    public static final RegistryObject<EntityType<WardenOfSeals>> WARDEN_OF_SEALS = ENTITIES.register("warden_of_seals",
            () -> EntityType.Builder.of(WardenOfSeals::new, MobCategory.MONSTER).sized(1.2F, 3.2F).fireImmune().clientTrackingRange(10)
                    .build("warden_of_seals"));
    public static final RegistryObject<EntityType<ArchivistPrime>> ARCHIVIST_PRIME = ENTITIES.register("archivist_prime",
            () -> EntityType.Builder.of(ArchivistPrime::new, MobCategory.MONSTER).sized(1.1F, 3.8F).fireImmune().clientTrackingRange(10)
                    .build("archivist_prime"));
    public static final RegistryObject<EntityType<SealNode>> SEAL_NODE = ENTITIES.register("seal_node",
            () -> EntityType.Builder.of(SealNode::new, MobCategory.MISC).sized(0.9F, 2.0F).fireImmune().clientTrackingRange(10)
                    .build("seal_node"));
    public static final RegistryObject<EntityType<IndexNode>> INDEX_NODE = ENTITIES.register("index_node",
            () -> EntityType.Builder.of(IndexNode::new, MobCategory.MISC).sized(0.9F, 2.2F).fireImmune().clientTrackingRange(10)
                    .build("index_node"));
    public static final RegistryObject<EntityType<IndexPage>> INDEX_PAGE = ENTITIES.register("index_page",
            () -> EntityType.Builder.<IndexPage>of(IndexPage::new, MobCategory.MISC).sized(0.3F, 0.3F).fireImmune().clientTrackingRange(8)
                    .updateInterval(1).build("index_page"));

    // ---- Spawn eggs ----
    static {
        egg("warden_of_seals", WARDEN_OF_SEALS, 0x2A3A66, 0xE2BE5C);
        egg("archivist_prime", ARCHIVIST_PRIME, 0x54307E, 0xF7E196);
    }

    private BossesC() {}

    private static RegistryObject<SoundEvent> sound(String path) {
        return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(RelicWeave.id(path)));
    }

    private static <T extends Mob> void egg(String id, Supplier<EntityType<T>> type, int primary, int secondary) {
        ITEMS.register(id + "_spawn_egg", () -> new ForgeSpawnEggItem(type, primary, secondary, new Item.Properties()));
    }
}
