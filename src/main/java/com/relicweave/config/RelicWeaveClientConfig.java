package com.relicweave.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Client configuration ({@code config/relicweave-client.toml}). Only changes what this client shows or plays; it never
 * affects server-side damage, loot, progression or recipe costs.
 */
public final class RelicWeaveClientConfig {
    public static final ForgeConfigSpec SPEC;

    public enum ParticleDensity { FULL, REDUCED, MINIMAL }

    public enum HazardHud { FULL, MINIMAL }

    // ---- Controls ----
    /** Set once the 0.0.4 Cycle Relic key check has run, so a deliberate rebind back to G is never undone. */
    public static final ForgeConfigSpec.BooleanValue CYCLE_KEY_CHECKED;

    // ---- The Vestige ----
    public static final ForgeConfigSpec.BooleanValue VESTIGE_AMBIENCE;
    public static final ForgeConfigSpec.BooleanValue VESTIGE_MUSIC;
    public static final ForgeConfigSpec.BooleanValue BOSS_MUSIC;
    public static final ForgeConfigSpec.EnumValue<ParticleDensity> PARTICLE_DENSITY;
    public static final ForgeConfigSpec.DoubleValue DISTORTION_INTENSITY;
    public static final ForgeConfigSpec.EnumValue<HazardHud> HAZARD_HUD;
    public static final ForgeConfigSpec.DoubleValue DREAD;
    public static final ForgeConfigSpec.BooleanValue DARK_HUD;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("controls");
        CYCLE_KEY_CHECKED = b.comment("Internal: true once RelicWeave has checked the Cycle Relic key for the 0.0.3 G-key clash",
                        "with Curios. Leave it alone.")
                .define("cycleKeyChecked", false);
        b.pop();

        b.comment("The Vestige (0.0.4 dimension). Presentation only.").push("vestige");
        VESTIGE_AMBIENCE = b.comment("Play biome ambience and Concordance Cycle cues in The Vestige.")
                .define("ambience", true);
        VESTIGE_MUSIC = b.comment("Play The Vestige's music pool.")
                .define("music", true);
        BOSS_MUSIC = b.comment("Play fight music while you are in a Vestige boss arena during a fight.")
                .define("bossMusic", true);
        PARTICLE_DENSITY = b.comment("FULL, REDUCED or MINIMAL ambient and hazard particles. Warnings always show at least one marker.")
                .defineEnum("particleDensity", ParticleDensity.FULL);
        DISTORTION_INTENSITY = b.comment("Strength of screen tint/fog distortion from hazards (0 = off, 1 = full).")
                .defineInRange("distortionIntensity", 1.0, 0.0, 1.0);
        HAZARD_HUD = b.comment("FULL shows the exposure meter with labels; MINIMAL shows only an icon while exposure is rising.")
                .defineEnum("hazardHud", HazardHud.FULL);
        DREAD = b.comment("How dark and oppressive The Vestige looks: night-black sky, close dark fog, dimmer daylight",
                        "(torches stay bright) and drifting ash. 0 = the brighter 0.0.4 look, 1 = full dread.")
                .defineInRange("dread", 1.0, 0.0, 1.0);
        DARK_HUD = b.comment("In The Vestige, darken the HUD (hearts, hunger, armor, air, XP bar and hotbar) and show a",
                        "creeping dark vignette.")
                .define("darkHud", true);
        b.pop();

        SPEC = b.build();
    }

    private RelicWeaveClientConfig() {}
}
