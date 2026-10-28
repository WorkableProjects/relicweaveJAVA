package com.relicweave.vestige.registry;

import com.relicweave.RelicWeave;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 0.0.4 sound events. Files and subtitles are in {@code assets/relicweave/sounds.json}. Every warning sound has a
 * subtitle and a visual equivalent; sound is never the only cue.
 */
public final class VestigeSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RelicWeave.MOD_ID);

    // ---- Gate (T02) ----
    public static final RegistryObject<SoundEvent> GATE_ACTIVATE = register("block.concordance_gate.activate");
    public static final RegistryObject<SoundEvent> GATE_CHANNEL = register("block.concordance_gate.channel");
    public static final RegistryObject<SoundEvent> GATE_TELEPORT = register("block.concordance_gate.teleport");

    // ---- Cycle and Rift Saturation (T03) ----
    public static final RegistryObject<SoundEvent> CYCLE_QUIET = register("ambient.vestige.cycle.quiet");
    public static final RegistryObject<SoundEvent> CYCLE_RESONANCE = register("ambient.vestige.cycle.resonance");
    public static final RegistryObject<SoundEvent> CYCLE_CONVERGENCE = register("ambient.vestige.cycle.convergence");
    public static final RegistryObject<SoundEvent> CYCLE_WARNING = register("ambient.vestige.cycle.warning");
    public static final RegistryObject<SoundEvent> RIFT_WARNING = register("hazard.rift.warning");
    public static final RegistryObject<SoundEvent> RIFT_SICKNESS = register("hazard.rift.sickness");
    public static final RegistryObject<SoundEvent> WARD_SALVE_DRINK = register("item.ward_salve.drink");

    // ---- Field hazards (T04) ----
    public static final RegistryObject<SoundEvent> GRAVITY_TELEGRAPH = register("hazard.gravity.telegraph");
    public static final RegistryObject<SoundEvent> GRAVITY_PULSE = register("hazard.gravity.pulse");
    public static final RegistryObject<SoundEvent> STARFALL_WARNING = register("hazard.starfall.warning");
    public static final RegistryObject<SoundEvent> STARFALL_IMPACT = register("hazard.starfall.impact");
    public static final RegistryObject<SoundEvent> NULL_POCKET = register("hazard.null_pocket.enter");
    public static final RegistryObject<SoundEvent> HUSH_ALERT = register("hazard.hush.alert");

    // ---- World presentation (T01) ----
    public static final RegistryObject<SoundEvent> MUSIC_VESTIGE = register("music.vestige");
    public static final RegistryObject<SoundEvent> AMBIENT_CROWNFALL_BASIN = register("ambient.vestige.crownfall_basin");
    public static final RegistryObject<SoundEvent> AMBIENT_GLASSROOT_WILDS = register("ambient.vestige.glassroot_wilds");
    public static final RegistryObject<SoundEvent> AMBIENT_AURELIAN_CANOPY = register("ambient.vestige.aurelian_canopy");
    public static final RegistryObject<SoundEvent> AMBIENT_HUSHFEN = register("ambient.vestige.hushfen");
    public static final RegistryObject<SoundEvent> AMBIENT_SUNDERED_REACH = register("ambient.vestige.sundered_reach");
    public static final RegistryObject<SoundEvent> AMBIENT_STARFALL_CALDERA = register("ambient.vestige.starfall_caldera");
    public static final RegistryObject<SoundEvent> AMBIENT_GLOAM_ARCHIVE = register("ambient.vestige.gloam_archive");
    public static final RegistryObject<SoundEvent> AMBIENT_CROWN_OF_CONCORDANCE = register("ambient.vestige.crown_of_concordance");

    // ---- Boss fight music (one per boss; played by the client while the boss bar is shown) ----
    public static final RegistryObject<SoundEvent> MUSIC_BOSS_THORNED_REGENT = register("music.boss.thorned_regent");
    public static final RegistryObject<SoundEvent> MUSIC_BOSS_CHOIR_UNHEARD = register("music.boss.choir_unheard");
    public static final RegistryObject<SoundEvent> MUSIC_BOSS_GRAVITY_MAW = register("music.boss.gravity_maw");
    public static final RegistryObject<SoundEvent> MUSIC_BOSS_STARFORGED_COLOSSUS = register("music.boss.starforged_colossus");
    public static final RegistryObject<SoundEvent> MUSIC_BOSS_WARDEN_OF_SEALS = register("music.boss.warden_of_seals");
    public static final RegistryObject<SoundEvent> MUSIC_BOSS_ARCHIVIST_PRIME = register("music.boss.archivist_prime");

    // Later phases append below (mobs, bosses, relics, loom).

    private VestigeSounds() {}

    public static RegistryObject<SoundEvent> register(String path) {
        return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(RelicWeave.id(path)));
    }
}
