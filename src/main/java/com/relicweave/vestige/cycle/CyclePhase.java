package com.relicweave.vestige.cycle;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** The three Concordance Cycle phases, in order. Ordinals are sent over the network; never reorder. */
public enum CyclePhase {
    /** Baseline spawns, safest exploration window, slow hazard build-up. */
    QUIET("quiet", ChatFormatting.AQUA),
    /** Runes light up, elites more active, Starfall begins, some sealed paths open. */
    RESONANCE("resonance", ChatFormatting.GOLD),
    /** Highest risk: rift activity doubled, rare event mobs, special nodes active. */
    CONVERGENCE("convergence", ChatFormatting.LIGHT_PURPLE);

    private final String key;
    private final ChatFormatting color;

    CyclePhase(String key, ChatFormatting color) {
        this.key = key;
        this.color = color;
    }

    public String key() { return key; }
    public ChatFormatting color() { return color; }

    /** {@code cycle.relicweave.<key>} */
    public Component displayName() {
        return Component.translatable("cycle.relicweave." + key).withStyle(color);
    }

    public CyclePhase next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static CyclePhase byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : QUIET;
    }
}
