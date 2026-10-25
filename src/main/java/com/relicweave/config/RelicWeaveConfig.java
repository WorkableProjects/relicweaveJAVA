package com.relicweave.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Server configuration ({@code <world>/serverconfig/relicweave-server.toml}), synced to clients by Forge. Read values
 * with {@code RelicWeaveConfig.X.get()} on the server only; GameTests run with the defaults.
 */
public final class RelicWeaveConfig {
    public static final ForgeConfigSpec SPEC;

    /** Where charms (including the 0.0.3 relics) are found. */
    public enum CharmActivation {
        /** Anywhere in the main inventory, offhand, or (with Curios) a Curios slot. 0.0.2 behaviour. */
        INVENTORY_OR_EQUIPPED,
        /** Only the offhand or (with Curios) a Curios slot. */
        EQUIPPED_ONLY
    }

    // ---- Relic locations ----
    public static final ForgeConfigSpec.EnumValue<CharmActivation> CHARM_ACTIVATION;
    public static final ForgeConfigSpec.BooleanValue CURIOS_TOTEMS;

    // ---- Relic cooldowns (seconds) ----
    public static final ForgeConfigSpec.IntValue WAYFARER_COOLDOWN;
    public static final ForgeConfigSpec.IntValue ECHO_COOLDOWN;
    public static final ForgeConfigSpec.IntValue GUARDIAN_COOLDOWN;
    public static final ForgeConfigSpec.IntValue FEATHERWOVEN_COOLDOWN;

    // ---- Duplicate / overlapping effects ----
    public static final ForgeConfigSpec.IntValue ABSORPTION_CAP;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Where relics work. Curios (optional) adds a 'charm' slot; without Curios, 'equipped' means the offhand.")
                .push("relics");
        CHARM_ACTIVATION = b.comment("INVENTORY_OR_EQUIPPED: charms work anywhere in the inventory, offhand or a Curios slot.",
                        "EQUIPPED_ONLY: charms only work from the offhand or a Curios slot.",
                        "Either way, each charm type counts once no matter how many copies you carry.")
                .defineEnum("charmActivation", CharmActivation.INVENTORY_OR_EQUIPPED);
        CURIOS_TOTEMS = b.comment("Allow RelicWeave totems to work from a Curios charm slot as well as the hand.",
                        "Off by default: totems in a Curios slot free up the offhand, which is a balance change.")
                .define("curiosTotems", false);
        b.pop();

        b.comment("Relic cooldowns in seconds. Cooldowns are stored per player and per relic type, so moving, dropping,",
                        "swapping or re-equipping a relic, dying, changing dimension or reconnecting never resets them.")
                .push("cooldowns");
        WAYFARER_COOLDOWN = b.defineInRange("wayfarersRelic", 300, 10, 86400);
        ECHO_COOLDOWN = b.defineInRange("echoCharm", 45, 5, 86400);
        GUARDIAN_COOLDOWN = b.defineInRange("guardianRelic", 120, 10, 86400);
        FEATHERWOVEN_COOLDOWN = b.defineInRange("featherwovenCharm", 10, 1, 86400);
        b.pop();

        b.comment("How overlapping effects from different sources combine.").push("duplicateEffects");
        ABSORPTION_CAP = b.comment("Maximum absorption (half-hearts) that the Guardian Relic, Soulguard and Warding can",
                        "grant together. They never add absorption above this cap.")
                .defineInRange("absorptionCap", 8, 0, 40);
        b.pop();

        SPEC = b.build();
    }

    private RelicWeaveConfig() {}
}
