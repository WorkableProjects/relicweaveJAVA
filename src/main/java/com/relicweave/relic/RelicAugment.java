package com.relicweave.relic;

import com.relicweave.RelicWeave;
import com.relicweave.item.RelicItem;
import com.relicweave.vestige.registry.VestigeMaterials;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Reliquary Loom augments (0.0.4). Each supported relic has exactly two mutually exclusive paths: EXPLORATION (A) and
 * STABILITY (B) — sidegrades, not upgrades. An augment is stored on the relic stack as
 * {@code {"relicweave:augment": {id: "<relic>/<path>", v: 1}}}; nothing else on the stack is touched, so the relic keeps
 * its registry id, custom name and other data, and cooldowns (stored per player and relic type) are never affected.
 * Unknown or malformed augment data reads as "no augment" (the base relic still works).
 * <p>
 * Effects are applied by the relic's own code through {@link #of(ItemStack)}; the Loom only writes the tag.
 */
public enum RelicAugment {
    SURVEYOR_WIDE(RelicType.SURVEYORS_LENS, Path.EXPLORATION, "wide", VestigeMaterials.AETHERITE_INGOT),
    SURVEYOR_FOCUSED(RelicType.SURVEYORS_LENS, Path.STABILITY, "focused", VestigeMaterials.RIFTGLASS),
    GRAVITY_NAIL_LONG(RelicType.GRAVITY_NAIL, Path.EXPLORATION, "long", VestigeMaterials.AETHERITE_INGOT),
    GRAVITY_NAIL_QUICK(RelicType.GRAVITY_NAIL, Path.STABILITY, "quick", VestigeMaterials.RIFTGLASS),
    CYCLE_DIAL_FORESIGHT(RelicType.CYCLE_DIAL, Path.EXPLORATION, "foresight", VestigeMaterials.AETHERITE_INGOT),
    CYCLE_DIAL_CHIME(RelicType.CYCLE_DIAL, Path.STABILITY, "chime", VestigeMaterials.RIFTGLASS),
    LANTERN_BROAD(RelicType.RIFTGLASS_LANTERN, Path.EXPLORATION, "broad", VestigeMaterials.AETHERITE_INGOT),
    LANTERN_BRIGHT(RelicType.RIFTGLASS_LANTERN, Path.STABILITY, "bright", VestigeMaterials.RIFTGLASS),
    SPINDLE_DEEP(RelicType.MNEMONIC_SPINDLE, Path.EXPLORATION, "deep", VestigeMaterials.AETHERITE_INGOT),
    SPINDLE_SWIFT(RelicType.MNEMONIC_SPINDLE, Path.STABILITY, "swift", VestigeMaterials.RIFTGLASS),
    SIGNET_OPENING(RelicType.SEALKEEPERS_SIGNET, Path.EXPLORATION, "opening", VestigeMaterials.AETHERITE_INGOT),
    SIGNET_DISRUPTION(RelicType.SEALKEEPERS_SIGNET, Path.STABILITY, "disruption", VestigeMaterials.RIFTGLASS);

    public enum Path { EXPLORATION, STABILITY }

    /** Stack tag key. */
    public static final String TAG = RelicWeave.MOD_ID + ":augment";
    /** Data format version; bump with a migration in {@link #of} if the format ever changes. */
    public static final int VERSION = 1;
    /** Count of the path material consumed (in addition to Mnemonic Filament). */
    public static final int MATERIAL_COST = 2;

    private final RelicType relic;
    private final Path path;
    private final String key;
    private final Supplier<? extends Item> material;

    RelicAugment(RelicType relic, Path path, String key, Supplier<? extends Item> material) {
        this.relic = relic;
        this.path = path;
        this.key = key;
        this.material = material;
    }

    public RelicType relic() { return relic; }
    public Path path() { return path; }
    /** {@code <relic item path>/<key>}, e.g. {@code surveyors_lens/wide}. Stored on stacks; never rename. */
    public String id() { return relic.itemPath() + "/" + key; }
    public Item material() { return material.get(); }

    /** {@code augment.relicweave.<relic>.<key>} */
    public Component displayName() {
        return Component.translatable("augment." + RelicWeave.MOD_ID + "." + relic.itemPath() + "." + key);
    }

    /** {@code augment.relicweave.<relic>.<key>.desc} */
    public Component description() {
        return Component.translatable("augment." + RelicWeave.MOD_ID + "." + relic.itemPath() + "." + key + ".desc");
    }

    /** The two paths of {@code type} (empty for relics the Loom does not support). */
    public static List<RelicAugment> forRelic(RelicType type) {
        List<RelicAugment> list = new ArrayList<>(2);
        for (RelicAugment augment : values()) {
            if (augment.relic == type) {
                list.add(augment);
            }
        }
        return list;
    }

    @Nullable
    public static RelicAugment byId(String id) {
        for (RelicAugment augment : values()) {
            if (augment.id().equals(id)) {
                return augment;
            }
        }
        return null;
    }

    /** The augment on {@code stack}, if any and valid for that relic. Never throws on bad data. */
    public static Optional<RelicAugment> of(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG, CompoundTag.TAG_COMPOUND) || !(stack.getItem() instanceof RelicItem item)) {
            return Optional.empty();
        }
        CompoundTag data = tag.getCompound(TAG);
        RelicAugment augment = byId(data.getString("id"));
        if (augment == null || augment.relic != item.getRelicType() || data.getInt("v") > VERSION) {
            return Optional.empty();
        }
        return Optional.of(augment);
    }

    /** True if {@code stack} carries {@code this}. */
    public boolean isOn(ItemStack stack) {
        return of(stack).map(a -> a == this).orElse(false);
    }

    /** Writes this augment to {@code stack} (replacing any other). Caller validates the relic type and costs. */
    public void apply(ItemStack stack) {
        CompoundTag data = new CompoundTag();
        data.putString("id", id());
        data.putInt("v", VERSION);
        stack.getOrCreateTag().put(TAG, data);
    }

    /** Removes any augment tag from {@code stack}, leaving all other data untouched. */
    public static void clear(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(TAG);
            if (tag.isEmpty()) {
                stack.setTag(null);
            }
        }
    }

    /** Tooltip line for an augmented relic (no line when there is none). */
    public static void appendTooltip(ItemStack stack, List<Component> tooltip) {
        of(stack).ifPresent(augment -> {
            tooltip.add(Component.translatable("tooltip.relicweave.augment", augment.displayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.add(augment.description().copy().withStyle(ChatFormatting.DARK_PURPLE));
        });
    }
}
