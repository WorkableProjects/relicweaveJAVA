package com.relicweave.vestige;

import com.relicweave.RelicWeave;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.structure.Structure;

/** Stable 0.0.4 IDs for The Vestige. Never rename these after release: worlds store them. */
public final class VestigeKeys {
    public static final ResourceKey<Level> THE_VESTIGE = ResourceKey.create(Registries.DIMENSION, RelicWeave.id("the_vestige"));
    public static final ResourceKey<DimensionType> THE_VESTIGE_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, RelicWeave.id("the_vestige"));

    // ---- Biomes ----
    public static final ResourceKey<Biome> CROWNFALL_BASIN = biome("crownfall_basin");
    public static final ResourceKey<Biome> GLASSROOT_WILDS = biome("glassroot_wilds");
    public static final ResourceKey<Biome> AURELIAN_CANOPY = biome("aurelian_canopy");
    public static final ResourceKey<Biome> HUSHFEN = biome("hushfen");
    public static final ResourceKey<Biome> SUNDERED_REACH = biome("sundered_reach");
    public static final ResourceKey<Biome> STARFALL_CALDERA = biome("starfall_caldera");
    public static final ResourceKey<Biome> GLOAM_ARCHIVE = biome("gloam_archive");
    public static final ResourceKey<Biome> CROWN_OF_CONCORDANCE = biome("crown_of_concordance");

    // ---- Structures (data-driven, added in Phase 3) ----
    public static final ResourceKey<Structure> WAYSTONE_MONOLITH = structure("waystone_monolith");
    public static final ResourceKey<Structure> ARCHIVISTS_OUTPOST = structure("archivists_outpost");
    public static final ResourceKey<Structure> RUINED_AETHER_AQUEDUCT = structure("ruined_aether_aqueduct");
    public static final ResourceKey<Structure> GILDED_CIVIC_TEMPLE = structure("gilded_civic_temple");
    public static final ResourceKey<Structure> GLASSROOT_WARREN = structure("glassroot_warren");
    public static final ResourceKey<Structure> HUSH_ARCHIVE = structure("hush_archive");
    public static final ResourceKey<Structure> SUNDERED_OBSERVATORY = structure("sundered_observatory");
    public static final ResourceKey<Structure> GRAVITY_VAULT = structure("gravity_vault");
    public static final ResourceKey<Structure> STARFALL_FORGE = structure("starfall_forge");
    public static final ResourceKey<Structure> CYCLE_OBSERVATORY = structure("cycle_observatory");
    public static final ResourceKey<Structure> HALL_OF_NINE_SEALS = structure("hall_of_nine_seals");
    public static final ResourceKey<Structure> CROWN_BASTION = structure("crown_bastion");
    public static final ResourceKey<Structure> CROWN_OF_CONCORDANCE_KEEP = structure("crown_of_concordance");
    public static final ResourceKey<Structure> RIFT_SCAR = structure("rift_scar");

    // ---- Tags ----
    /** Every Vestige biome. */
    public static final TagKey<Biome> IS_VESTIGE = TagKey.create(Registries.BIOME, RelicWeave.id("is_vestige"));
    /** Biomes where Rift Saturation builds up anywhere outdoors (not only near fissures). */
    public static final TagKey<Biome> RIFT_SATURATED = TagKey.create(Registries.BIOME, RelicWeave.id("rift_saturated"));
    /** Biomes with Gravity Shear pulses. */
    public static final TagKey<Biome> GRAVITY_SHEAR = TagKey.create(Registries.BIOME, RelicWeave.id("gravity_shear"));
    /** Biomes with Starfall Impacts during Resonance and Convergence. */
    public static final TagKey<Biome> STARFALL = TagKey.create(Registries.BIOME, RelicWeave.id("starfall"));
    /** Biomes with Hushfield (sound-sensitive enemies, dense fog). */
    public static final TagKey<Biome> HUSHFIELD = TagKey.create(Registries.BIOME, RelicWeave.id("hushfield"));
    /** Vestige elites and bosses (Sovereign's Bane, Sealkeeper's Signet targeting). */
    public static final TagKey<EntityType<?>> VESTIGE_ELITES = TagKey.create(Registries.ENTITY_TYPE, RelicWeave.id("vestige_elites"));
    public static final TagKey<EntityType<?>> VESTIGE_BOSSES = TagKey.create(Registries.ENTITY_TYPE, RelicWeave.id("vestige_bosses"));
    /** Every Vestige mob (hazard immunity for natives, Surveyor/Glyph reveals ignore them). */
    public static final TagKey<EntityType<?>> VESTIGE_MOBS = TagKey.create(Registries.ENTITY_TYPE, RelicWeave.id("vestige_mobs"));
    /** Blocks Sealbreaker speeds up and the Sealkeeper's Signet can open (never boss locks). */
    public static final TagKey<Block> BREAKABLE_SEALS = TagKey.create(Registries.BLOCK, RelicWeave.id("breakable_seals"));
    /** Hidden glyphs / false walls revealed by Glyphfinder and the Surveyor's Lens. */
    public static final TagKey<Block> HIDDEN_GLYPHS = TagKey.create(Registries.BLOCK, RelicWeave.id("hidden_glyphs"));

    private VestigeKeys() {}

    private static ResourceKey<Biome> biome(String path) {
        return ResourceKey.create(Registries.BIOME, RelicWeave.id(path));
    }

    private static ResourceKey<Structure> structure(String path) {
        return ResourceKey.create(Registries.STRUCTURE, RelicWeave.id(path));
    }
}
