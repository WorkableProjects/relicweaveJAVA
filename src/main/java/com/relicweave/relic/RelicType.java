package com.relicweave.relic;

import com.relicweave.RelicWeave;
import com.relicweave.config.RelicWeaveConfig;
import com.relicweave.vestige.VestigeConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The 0.0.3 relics and (appended) the 0.0.4 Vestige relics. Never reorder: names are saved cooldown keys. They are found like charms ({@link RelicLocator}): each type counts once however many copies the
 * player carries. Activatable relics are triggered with the single <i>Activate Relic</i> key after picking one with
 * <i>Cycle Relic</i>; passive relics work on their own.
 */
public enum RelicType {
    /** Toggle: pulls nearby dropped items toward the player. Activation flips it on/off; no cooldown. */
    LODESTONE("lodestone_charm", true, null),
    /** Passive: water breathing and better underwater visibility. */
    TIDAL("tidal_charm", false, null),
    /** Passive: halves fall damage; briefly applies Slow Falling during a dangerous fall. */
    FEATHERWOVEN("featherwoven_charm", false, () -> RelicWeaveConfig.FEATHERWOVEN_COOLDOWN),
    /** Active: channel to return to the bound location (same dimension only). */
    WAYFARER("wayfarers_relic", true, () -> RelicWeaveConfig.WAYFARER_COOLDOWN),
    /** Active: briefly highlights nearby hostile mobs. */
    ECHO("echo_charm", true, () -> RelicWeaveConfig.ECHO_COOLDOWN),
    /** Passive: a short absorption shield when health becomes critical. */
    GUARDIAN("guardian_relic", false, () -> RelicWeaveConfig.GUARDIAN_COOLDOWN),

    // ---- 0.0.4 Vestige relics (cooldowns in the vestige server config, times relics.cooldownMultiplier) ----
    /** Active: outlines hidden glyphs, false walls, seals and terminals nearby. */
    SURVEYORS_LENS("surveyors_lens", true, () -> VestigeConfig.SURVEYORS_LENS_COOLDOWN, true),
    /** Active: briefly anchors the wearer against gravity pulses and pulls. */
    GRAVITY_NAIL("gravity_nail", true, () -> VestigeConfig.GRAVITY_NAIL_COOLDOWN, true),
    /** Passive: shows the Concordance Cycle countdown and warns before transitions. */
    CYCLE_DIAL("cycle_dial", false, null, true),
    /** Active: a small temporary zone that slows Rift Saturation. */
    RIFTGLASS_LANTERN("riftglass_lantern", true, () -> VestigeConfig.RIFTGLASS_LANTERN_COOLDOWN, true),
    /** Active: decodes a nearby Ancient Terminal. */
    MNEMONIC_SPINDLE("mnemonic_spindle", true, () -> VestigeConfig.MNEMONIC_SPINDLE_COOLDOWN, true),
    /** Active: disrupts nearby non-boss Vestige wardens and opens adjacent Glyph Seals. */
    SEALKEEPERS_SIGNET("sealkeepers_signet", true, () -> VestigeConfig.SEALKEEPERS_SIGNET_COOLDOWN, true);

    private final String itemPath;
    private final boolean activatable;
    @Nullable
    private final Supplier<ForgeConfigSpec.IntValue> cooldownSeconds;
    private final boolean vestige;

    RelicType(String itemPath, boolean activatable, @Nullable Supplier<ForgeConfigSpec.IntValue> cooldownSeconds) {
        this(itemPath, activatable, cooldownSeconds, false);
    }

    RelicType(String itemPath, boolean activatable, @Nullable Supplier<ForgeConfigSpec.IntValue> cooldownSeconds, boolean vestige) {
        this.itemPath = itemPath;
        this.activatable = activatable;
        this.cooldownSeconds = cooldownSeconds;
        this.vestige = vestige;
    }

    /** Registry path of the matching item, e.g. {@code echo_charm}; also the stable key used in saved cooldowns. */
    public String itemPath() { return itemPath; }
    public ResourceLocation itemId() { return RelicWeave.id(itemPath); }
    /** True if the Activate Relic key does something for this relic. */
    public boolean activatable() { return activatable; }
    public boolean hasCooldown() { return cooldownSeconds != null; }

    /** Configured cooldown in ticks; 0 for relics without one. Server-side only. */
    public int cooldownTicks() {
        if (cooldownSeconds == null) {
            return 0;
        }
        int ticks = cooldownSeconds.get().get() * 20;
        return vestige ? (int) Math.round(ticks * VestigeConfig.VESTIGE_RELIC_COOLDOWN_MULTIPLIER.get()) : ticks;
    }

    /** True for the 0.0.4 Vestige relics. */
    public boolean vestige() { return vestige; }

    public Component displayName() {
        return Component.translatable("item." + RelicWeave.MOD_ID + "." + itemPath);
    }

    @Nullable
    public static RelicType byItemPath(String path) {
        for (RelicType type : values()) {
            if (type.itemPath.equals(path)) {
                return type;
            }
        }
        return null;
    }
}
