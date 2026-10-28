package com.relicweave.infusion;

import com.relicweave.RelicWeave;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeMod;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Infusions for Celestial armor. Which item produces which infusion is defined by {@code relicweave:infusion} recipes.
 * <ul>
 *   <li><b>Attribute infusions</b> (0.0.2): fit any Celestial armor piece and grant the same attribute bonus whatever
 *   the slot, so wearing the same infusion on several pieces stacks.</li>
 *   <li><b>Effect infusions</b> (0.0.3): fit exactly one armor piece ({@link #slot()}) and have no attribute; their
 *   behaviour lives in {@code com.relicweave.infusion.effects}. Because each fits a single slot, a player can wear at
 *   most one instance, so identical effect infusions never stack.</li>
 * </ul>
 */
public enum InfusionType {
    COBBLED("cobbled", () -> Attributes.ARMOR, 1.0, AttributeModifier.Operation.ADDITION),
    OXIDIZED("oxidized", () -> Attributes.MOVEMENT_SPEED, 0.04, AttributeModifier.Operation.MULTIPLY_BASE),
    CONDUCTIVE("conductive", () -> Attributes.ATTACK_SPEED, 0.06, AttributeModifier.Operation.MULTIPLY_BASE),
    GOLEMED("golemed", () -> Attributes.KNOCKBACK_RESISTANCE, 0.05, AttributeModifier.Operation.ADDITION),
    PIGLINIFIED("piglinified", () -> Attributes.LUCK, 1.0, AttributeModifier.Operation.ADDITION),
    VILLAGERS_BEST_FRIEND("villagers_best_friend", () -> Attributes.MAX_HEALTH, 2.0, AttributeModifier.Operation.ADDITION),
    NECROMANCER("necromancer", () -> Attributes.ATTACK_DAMAGE, 1.0, AttributeModifier.Operation.ADDITION),
    NECROMANCED("necromanced", () -> Attributes.ARMOR_TOUGHNESS, 1.0, AttributeModifier.Operation.ADDITION),
    UPBOUND("upbound", ForgeMod.ENTITY_GRAVITY, -0.06, AttributeModifier.Operation.MULTIPLY_BASE),

    // 0.0.3 effect infusions (one armor piece each; see com.relicweave.infusion.effects)
    /** Helmet: water breathing while submerged. */
    TIDAL("tidal", EquipmentSlot.HEAD),
    /** Chestplate: periodically prepares one ward charge that softens the next hit. */
    WARDING("warding", EquipmentSlot.CHEST),
    /** Leggings: a brief speed burst after killing a hostile mob. */
    PURSUIT("pursuit", EquipmentSlot.LEGS),
    /** Boots: crouching greatly reduces incoming knockback. */
    ANCHORED("anchored", EquipmentSlot.FEET),

    // 0.0.4 Vestige effect infusions (one armor piece each; see com.relicweave.infusion.effects.vestige)
    /** Helmet: slower Rift Saturation build-up (never full immunity). */
    RIFTWARD("riftward", EquipmentSlot.HEAD),
    /** Chestplate: cancels one Starfall Impact hit, then recharges. */
    SEALGUARD("sealguard", EquipmentSlot.CHEST),
    /** Leggings: a brief mobility burst when the Concordance Cycle changes phase. */
    CYCLEWEAVE("cycleweave", EquipmentSlot.LEGS),
    /** Boots: weaker Gravity Shear pulls; shares a combined cap with Riftwalker. */
    GRAVITIC_TREAD("gravitic_tread", EquipmentSlot.FEET);

    private final ResourceLocation id;
    @Nullable
    private final Supplier<Attribute> attribute;
    private final double amount;
    private final AttributeModifier.Operation operation;
    @Nullable
    private final EquipmentSlot slot;

    InfusionType(String path, Supplier<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
        this.id = RelicWeave.id(path);
        this.attribute = attribute;
        this.amount = amount;
        this.operation = operation;
        this.slot = null;
    }

    InfusionType(String path, EquipmentSlot slot) {
        this.id = RelicWeave.id(path);
        this.attribute = null;
        this.amount = 0.0;
        this.operation = AttributeModifier.Operation.ADDITION;
        this.slot = slot;
    }

    public ResourceLocation id() { return id; }
    /** True for 0.0.2 attribute infusions; false for effect infusions, which have no attribute. */
    public boolean hasAttribute() { return attribute != null; }
    /** Only valid when {@link #hasAttribute()}. */
    public Attribute attribute() {
        if (attribute == null) {
            throw new IllegalStateException(id + " is an effect infusion and has no attribute");
        }
        return attribute.get();
    }
    public double amount() { return amount; }
    public AttributeModifier.Operation operation() { return operation; }

    /** The only armor slot this infusion fits, or null if it fits every Celestial armor piece. */
    @Nullable
    public EquipmentSlot slot() { return slot; }

    /** True if this infusion may be applied to armor worn in {@code armorSlot}. */
    public boolean fits(EquipmentSlot armorSlot) {
        return slot == null || slot == armorSlot;
    }

    /** Translation key {@code infusion.relicweave.<path>.desc}: one-line effect description for tooltips/JEI. */
    public Component description() {
        return Component.translatable("infusion." + id.getNamespace() + "." + id.getPath() + ".desc");
    }

    /** Translation key {@code infusion.relicweave.<path>}. */
    public Component displayName() {
        return Component.translatable("infusion." + id.getNamespace() + "." + id.getPath());
    }

    /** Stable modifier UUID per (infusion, armor slot index 0-3), so pieces in different slots stack. */
    public UUID modifierId(int armorSlotIndex) {
        return UUID.nameUUIDFromBytes((id + "#" + armorSlotIndex).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Nullable
    public static InfusionType byId(ResourceLocation id) {
        for (InfusionType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}
