package com.relicweave.relic;

import com.relicweave.RelicWeave;
import com.relicweave.config.RelicWeaveConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The 0.0.3 relics. They are found like charms ({@link RelicLocator}): each type counts once however many copies the
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
    GUARDIAN("guardian_relic", false, () -> RelicWeaveConfig.GUARDIAN_COOLDOWN);

    private final String itemPath;
    private final boolean activatable;
    @Nullable
    private final Supplier<ForgeConfigSpec.IntValue> cooldownSeconds;

    RelicType(String itemPath, boolean activatable, @Nullable Supplier<ForgeConfigSpec.IntValue> cooldownSeconds) {
        this.itemPath = itemPath;
        this.activatable = activatable;
        this.cooldownSeconds = cooldownSeconds;
    }

    /** Registry path of the matching item, e.g. {@code echo_charm}; also the stable key used in saved cooldowns. */
    public String itemPath() { return itemPath; }
    public ResourceLocation itemId() { return RelicWeave.id(itemPath); }
    /** True if the Activate Relic key does something for this relic. */
    public boolean activatable() { return activatable; }
    public boolean hasCooldown() { return cooldownSeconds != null; }

    /** Configured cooldown in ticks; 0 for relics without one. Server-side only. */
    public int cooldownTicks() {
        return cooldownSeconds == null ? 0 : cooldownSeconds.get().get() * 20;
    }

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
