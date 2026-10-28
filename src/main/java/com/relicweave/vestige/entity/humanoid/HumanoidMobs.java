package com.relicweave.vestige.entity.humanoid;

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

/** T06 humanoid Vestige mobs (HumanoidModel/SkeletonModel based): entity types, spawn eggs and their own sound events. */
public final class HumanoidMobs {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RelicWeave.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RelicWeave.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RelicWeave.MOD_ID);

    // ---- Sounds (wind-up cues; subtitles live in the sounds.json fragment) ----
    public static final RegistryObject<SoundEvent> SENTINEL_GUARD = sound("entity.reliquary_sentinel.guard");
    public static final RegistryObject<SoundEvent> SCRIBE_CAST = sound("entity.scriptbound_scribe.cast");
    public static final RegistryObject<SoundEvent> HUSK_CRACK = sound("entity.gilded_husk.crack");
    public static final RegistryObject<SoundEvent> CANTOR_CHANT = sound("entity.mirebound_cantor.chant");
    public static final RegistryObject<SoundEvent> KNIGHT_SWEEP = sound("entity.oathbreaker_knight.sweep");
    public static final RegistryObject<SoundEvent> KNIGHT_LUNGE = sound("entity.oathbreaker_knight.lunge");
    public static final RegistryObject<SoundEvent> ARCHER_AIM = sound("entity.rift_archer.aim");
    public static final RegistryObject<SoundEvent> WEAVER_PULSE = sound("entity.echo_weaver.pulse");

    // ---- Entity types ----
    public static final RegistryObject<EntityType<ReliquarySentinel>> RELIQUARY_SENTINEL = ENTITIES.register("reliquary_sentinel",
            () -> EntityType.Builder.of(ReliquarySentinel::new, MobCategory.MONSTER).sized(0.66F, 2.15F).clientTrackingRange(8)
                    .build("reliquary_sentinel"));
    public static final RegistryObject<EntityType<ScriptboundScribe>> SCRIPTBOUND_SCRIBE = ENTITIES.register("scriptbound_scribe",
            () -> EntityType.Builder.of(ScriptboundScribe::new, MobCategory.MONSTER).sized(0.6F, 1.95F).clientTrackingRange(8)
                    .build("scriptbound_scribe"));
    public static final RegistryObject<EntityType<GildedHusk>> GILDED_HUSK = ENTITIES.register("gilded_husk",
            () -> EntityType.Builder.of(GildedHusk::new, MobCategory.MONSTER).sized(0.66F, 2.15F).clientTrackingRange(8)
                    .build("gilded_husk"));
    public static final RegistryObject<EntityType<MireboundCantor>> MIREBOUND_CANTOR = ENTITIES.register("mirebound_cantor",
            () -> EntityType.Builder.of(MireboundCantor::new, MobCategory.MONSTER).sized(0.6F, 1.95F).clientTrackingRange(8)
                    .build("mirebound_cantor"));
    public static final RegistryObject<EntityType<OathbreakerKnight>> OATHBREAKER_KNIGHT = ENTITIES.register("oathbreaker_knight",
            () -> EntityType.Builder.of(OathbreakerKnight::new, MobCategory.MONSTER).sized(0.75F, 2.45F).clientTrackingRange(10)
                    .build("oathbreaker_knight"));
    public static final RegistryObject<EntityType<VestigeDrifter>> VESTIGE_DRIFTER = ENTITIES.register("vestige_drifter",
            () -> EntityType.Builder.of(VestigeDrifter::new, MobCategory.CREATURE).sized(0.6F, 1.9F).clientTrackingRange(8)
                    .build("vestige_drifter"));
    public static final RegistryObject<EntityType<RiftArcher>> RIFT_ARCHER = ENTITIES.register("rift_archer",
            () -> EntityType.Builder.of(RiftArcher::new, MobCategory.MONSTER).sized(0.66F, 2.2F).clientTrackingRange(10)
                    .build("rift_archer"));
    public static final RegistryObject<EntityType<EchoWeaver>> ECHO_WEAVER = ENTITIES.register("echo_weaver",
            () -> EntityType.Builder.of(EchoWeaver::new, MobCategory.MONSTER).sized(0.6F, 1.8F).clientTrackingRange(8)
                    .build("echo_weaver"));
    public static final RegistryObject<EntityType<GlyphBolt>> GLYPH_BOLT = ENTITIES.register("glyph_bolt",
            () -> EntityType.Builder.<GlyphBolt>of(GlyphBolt::new, MobCategory.MISC).sized(0.3F, 0.3F).clientTrackingRange(8)
                    .updateInterval(1).build("glyph_bolt"));

    // ---- Spawn eggs ----
    static {
        egg("reliquary_sentinel", RELIQUARY_SENTINEL, 0xD8CFB8, 0xC4983A);
        egg("scriptbound_scribe", SCRIPTBOUND_SCRIBE, 0x243E6C, 0x60B2A6);
        egg("gilded_husk", GILDED_HUSK, 0x9E927C, 0xF7E196);
        egg("mirebound_cantor", MIREBOUND_CANTOR, 0x2E4E30, 0x96B2D0);
        egg("oathbreaker_knight", OATHBREAKER_KNIGHT, 0x1A2040, 0xA468CC);
        egg("vestige_drifter", VESTIGE_DRIFTER, 0xBFB49C, 0x3C8C86);
        egg("rift_archer", RIFT_ARCHER, 0x543290, 0xCCE0F0);
        egg("echo_weaver", ECHO_WEAVER, 0x7840A0, 0x96D6C8);
    }

    private HumanoidMobs() {}

    private static RegistryObject<SoundEvent> sound(String path) {
        return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(RelicWeave.id(path)));
    }

    private static <T extends Mob> void egg(String id, Supplier<EntityType<T>> type, int primary, int secondary) {
        ITEMS.register(id + "_spawn_egg", () -> new ForgeSpawnEggItem(type, primary, secondary, new Item.Properties()));
    }
}
