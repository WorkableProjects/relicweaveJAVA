package com.relicweave.vestige;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Server configuration for The Vestige ({@code <world>/serverconfig/relicweave-vestige-server.toml}), synced to clients
 * by Forge. Every 0.0.4 balance value lives here so it can be audited in one place. GameTests run with the defaults.
 * Read values with {@code X.get()}; on the client only after {@code SPEC.isLoaded()}.
 */
public final class VestigeConfig {
    public static final ForgeConfigSpec SPEC;
    public static final String FILE_NAME = "relicweave-vestige-server.toml";

    // ---- Gate ----
    public static final ForgeConfigSpec.BooleanValue REQUIRE_DRAGON_KILL;
    public static final ForgeConfigSpec.IntValue TELEPORT_COOLDOWN_SECONDS;

    // ---- Concordance Cycle ----
    public static final ForgeConfigSpec.BooleanValue CYCLE_ENABLED;
    public static final ForgeConfigSpec.IntValue QUIET_SECONDS;
    public static final ForgeConfigSpec.IntValue RESONANCE_SECONDS;
    public static final ForgeConfigSpec.IntValue CONVERGENCE_SECONDS;
    public static final ForgeConfigSpec.IntValue CONVERGENCE_WARNING_SECONDS;

    // ---- Hazards ----
    public static final ForgeConfigSpec.BooleanValue RIFT_SATURATION;
    public static final ForgeConfigSpec.BooleanValue GRAVITY_SHEAR;
    public static final ForgeConfigSpec.BooleanValue STARFALL;
    public static final ForgeConfigSpec.BooleanValue NULL_POCKETS;
    public static final ForgeConfigSpec.BooleanValue HUSHFIELD;
    public static final ForgeConfigSpec.DoubleValue RIFT_EXPOSURE_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue RIFT_DECAY_PER_SECOND;
    public static final ForgeConfigSpec.IntValue RIFT_DEBUFF_SECONDS;
    public static final ForgeConfigSpec.DoubleValue CONVERGENCE_HAZARD_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue GRAVITY_PULSE_STRENGTH;
    public static final ForgeConfigSpec.IntValue GRAVITY_PULSE_INTERVAL_SECONDS;
    public static final ForgeConfigSpec.DoubleValue STARFALL_DAMAGE;
    public static final ForgeConfigSpec.IntValue STARFALL_WARNING_TICKS;
    public static final ForgeConfigSpec.IntValue STARFALL_INTERVAL_SECONDS;
    public static final ForgeConfigSpec.DoubleValue MAX_HAZARD_PROTECTION;

    // ---- Mobs ----
    public static final ForgeConfigSpec.DoubleValue NATURAL_SPAWN_RATE;
    public static final ForgeConfigSpec.IntValue LOCAL_HOSTILE_CAP;
    public static final ForgeConfigSpec.DoubleValue ELITE_SPAWN_RATE;
    public static final ForgeConfigSpec.DoubleValue HERALD_CHANCE;

    // ---- Bosses ----
    public static final ForgeConfigSpec.DoubleValue BOSS_HEALTH_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue BOSS_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue BOSS_PLAYER_SCALING;
    public static final ForgeConfigSpec.DoubleValue BOSS_MAX_SCALING;
    public static final ForgeConfigSpec.DoubleValue ARENA_HAZARD_STRENGTH;
    public static final ForgeConfigSpec.BooleanValue BOSS_REMATCHES;
    public static final ForgeConfigSpec.IntValue BOSS_REMATCH_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.IntValue BOSS_RESET_SECONDS;

    // ---- Relics ----
    public static final ForgeConfigSpec.DoubleValue VESTIGE_RELIC_COOLDOWN_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue SURVEYORS_LENS_COOLDOWN;
    public static final ForgeConfigSpec.IntValue GRAVITY_NAIL_COOLDOWN;
    public static final ForgeConfigSpec.IntValue RIFTGLASS_LANTERN_COOLDOWN;
    public static final ForgeConfigSpec.IntValue MNEMONIC_SPINDLE_COOLDOWN;
    public static final ForgeConfigSpec.IntValue SEALKEEPERS_SIGNET_COOLDOWN;

    // ---- Reliquary Loom ----
    public static final ForgeConfigSpec.IntValue LOOM_AUGMENT_FILAMENT_COST;
    public static final ForgeConfigSpec.IntValue LOOM_RESET_FILAMENT_COST;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Concordance Gate (entry to The Vestige).").push("gate");
        REQUIRE_DRAGON_KILL = b.comment("Require the 'Free the End' advancement to activate a Concordance Gate.",
                        "Turn off only in development worlds.")
                .define("requireDragonKill", true);
        TELEPORT_COOLDOWN_SECONDS = b.comment("Seconds before a player can use a gate or Return Waystone again (prevents portal loops).")
                .defineInRange("teleportCooldownSeconds", 5, 1, 300);
        b.pop();

        b.comment("Concordance Cycle: Quiet -> Resonance -> Convergence, driven by world game time.").push("cycle");
        CYCLE_ENABLED = b.comment("If false, The Vestige stays in the Quiet phase forever.")
                .define("enabled", true);
        QUIET_SECONDS = b.defineInRange("quietSeconds", 540, 30, 7200);
        RESONANCE_SECONDS = b.defineInRange("resonanceSeconds", 360, 30, 7200);
        CONVERGENCE_SECONDS = b.defineInRange("convergenceSeconds", 240, 30, 7200);
        CONVERGENCE_WARNING_SECONDS = b.comment("Warning (sound, subtitle and on-screen text) this many seconds before Convergence.")
                .defineInRange("convergenceWarningSeconds", 30, 5, 300);
        b.pop();

        b.comment("Environmental hazards. They only ever affect players inside The Vestige.").push("hazards");
        RIFT_SATURATION = b.define("riftSaturation", true);
        GRAVITY_SHEAR = b.define("gravityShear", true);
        STARFALL = b.define("starfall", true);
        NULL_POCKETS = b.define("nullPockets", true);
        HUSHFIELD = b.define("hushfield", true);
        RIFT_EXPOSURE_PER_SECOND = b.comment("Rift Saturation gained per second while exposed (meter is 0-100), before protection.")
                .defineInRange("riftExposurePerSecond", 2.0, 0.0, 20.0);
        RIFT_DECAY_PER_SECOND = b.comment("Rift Saturation lost per second while not exposed.")
                .defineInRange("riftDecayPerSecond", 3.0, 0.1, 50.0);
        RIFT_DEBUFF_SECONDS = b.comment("Length of the Rift Sickness debuff when the meter fills. The meter then drops to 60.")
                .defineInRange("riftDebuffSeconds", 8, 1, 30);
        CONVERGENCE_HAZARD_MULTIPLIER = b.comment("Rift exposure rate multiplier during Convergence.")
                .defineInRange("convergenceHazardMultiplier", 2.0, 1.0, 5.0);
        GRAVITY_PULSE_STRENGTH = b.comment("Velocity of a Gravity Shear pulse before protection (blocks/tick).")
                .defineInRange("gravityPulseStrength", 0.6, 0.0, 1.5);
        GRAVITY_PULSE_INTERVAL_SECONDS = b.comment("Average seconds between Gravity Shear pulses per player.")
                .defineInRange("gravityPulseIntervalSeconds", 12, 4, 120);
        STARFALL_DAMAGE = b.comment("Damage of a Starfall Impact at its centre (falls off to half at the edge).")
                .defineInRange("starfallDamage", 6.0, 0.0, 20.0);
        STARFALL_WARNING_TICKS = b.comment("Ticks between the impact marker appearing and the strike.")
                .defineInRange("starfallWarningTicks", 60, 40, 200);
        STARFALL_INTERVAL_SECONDS = b.comment("Average seconds between impacts near each player in a Starfall biome.")
                .defineInRange("starfallIntervalSeconds", 12, 4, 120);
        MAX_HAZARD_PROTECTION = b.comment("Cap on combined protection from all sources (salves, infusions, relics, armor) per hazard.",
                        "0.8 = at most 80% reduction; nothing makes every hazard irrelevant.")
                .defineInRange("maxHazardProtection", 0.8, 0.0, 0.95);
        b.pop();

        b.comment("Vestige creatures.").push("mobs");
        NATURAL_SPAWN_RATE = b.comment("Fraction of natural Vestige spawn attempts allowed (1 = all, 0 = none).")
                .defineInRange("naturalSpawnRate", 1.0, 0.0, 1.0);
        LOCAL_HOSTILE_CAP = b.comment("Natural spawns are refused when this many Vestige hostiles are within 48 blocks.")
                .defineInRange("localHostileCap", 14, 1, 100);
        ELITE_SPAWN_RATE = b.comment("Multiplier on the chance that an elite (Oathbreaker Knight, Rift Archer, Shardback Brute) spawns naturally.")
                .defineInRange("eliteSpawnRate", 1.0, 0.0, 4.0);
        HERALD_CHANCE = b.comment("Chance per player per Convergence phase that a Cycle Herald appears nearby.")
                .defineInRange("heraldChance", 0.25, 0.0, 1.0);
        b.pop();

        b.comment("Vestige bosses. The defaults are balanced for one prepared player; you shouldn't need to change them.").push("bosses");
        BOSS_HEALTH_MULTIPLIER = b.defineInRange("healthMultiplier", 1.0, 0.25, 10.0);
        BOSS_DAMAGE_MULTIPLIER = b.defineInRange("damageMultiplier", 1.0, 0.25, 10.0);
        BOSS_PLAYER_SCALING = b.comment("Extra boss health per additional player in the arena (0.35 = +35% each).")
                .defineInRange("playerScaling", 0.35, 0.0, 1.0);
        BOSS_MAX_SCALING = b.comment("Cap on the player-count health multiplier.")
                .defineInRange("maxScaling", 2.5, 1.0, 10.0);
        ARENA_HAZARD_STRENGTH = b.comment("Multiplier on arena hazards (roots, silence zones, debris, heat, seals).")
                .defineInRange("arenaHazardStrength", 1.0, 0.0, 3.0);
        BOSS_REMATCHES = b.comment("Allow summoning a boss again at its altar after it was defeated.")
                .define("rematches", true);
        BOSS_REMATCH_COOLDOWN_SECONDS = b.defineInRange("rematchCooldownSeconds", 300, 0, 86400);
        BOSS_RESET_SECONDS = b.comment("A boss with no player in its arena for this long heals fully and returns to its altar.")
                .defineInRange("resetSeconds", 30, 5, 600);
        b.pop();

        b.comment("0.0.4 relics. Cooldowns in seconds; like 0.0.3 relics they are stored per player and relic type.").push("relics");
        VESTIGE_RELIC_COOLDOWN_MULTIPLIER = b.comment("Multiplier on every 0.0.4 relic cooldown.")
                .defineInRange("cooldownMultiplier", 1.0, 0.1, 10.0);
        SURVEYORS_LENS_COOLDOWN = b.defineInRange("surveyorsLens", 30, 5, 86400);
        GRAVITY_NAIL_COOLDOWN = b.defineInRange("gravityNail", 20, 5, 86400);
        RIFTGLASS_LANTERN_COOLDOWN = b.defineInRange("riftglassLantern", 90, 10, 86400);
        MNEMONIC_SPINDLE_COOLDOWN = b.defineInRange("mnemonicSpindle", 10, 1, 86400);
        SEALKEEPERS_SIGNET_COOLDOWN = b.defineInRange("sealkeepersSignet", 60, 10, 86400);
        b.pop();

        b.comment("Reliquary Loom augment costs.").push("loom");
        LOOM_AUGMENT_FILAMENT_COST = b.defineInRange("augmentFilamentCost", 2, 1, 64);
        LOOM_RESET_FILAMENT_COST = b.comment("Removing an augment (returns the base relic; the augment materials are not refunded).")
                .defineInRange("resetFilamentCost", 1, 0, 64);
        b.pop();

        SPEC = b.build();
    }

    private VestigeConfig() {}
}
