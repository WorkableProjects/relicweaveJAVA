package com.relicweave.block;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * What a machine is doing, synced to its open GUI through {@code ContainerData} (as {@link #ordinal()}) and shown as a
 * status line instead of silently stopping. Shared by the Transmutation Table and the Celestial Infuser.
 */
public enum MachineStatus {
    /** Nothing to do: an input slot is empty. No message. */
    IDLE(null, ChatFormatting.GRAY),
    /** A recipe is running. */
    WORKING("working", ChatFormatting.DARK_GREEN),
    /** Inputs are present, but no recipe accepts them (wrong item, too few items, or the wrong combination). */
    MISSING_INGREDIENT("missing_ingredient", ChatFormatting.RED),
    /** A recipe matches but the output slot cannot take the result. */
    OUTPUT_FULL("output_full", ChatFormatting.RED),
    /** Infuser: the armor already holds {@code Infusions.MAX_INFUSIONS} infusions. */
    MAX_INFUSIONS("max_infusions", ChatFormatting.RED),
    /** Infuser: the armor already holds this infusion. */
    ALREADY_INFUSED("already_infused", ChatFormatting.RED),
    /** Infuser: this infusion only fits a different armor piece. */
    WRONG_ARMOR_PIECE("wrong_armor_piece", ChatFormatting.RED);

    @Nullable
    private final String key;
    private final ChatFormatting color;

    MachineStatus(@Nullable String key, ChatFormatting color) {
        this.key = key;
        this.color = color;
    }

    /** {@code status.relicweave.machine.<key>}, or null for {@link #IDLE}. */
    @Nullable
    public Component message() {
        return key == null ? null : Component.translatable("status.relicweave.machine." + key).withStyle(color);
    }

    public static MachineStatus byOrdinal(int ordinal) {
        MachineStatus[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : IDLE;
    }
}
