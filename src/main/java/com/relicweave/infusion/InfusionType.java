package com.relicweave.infusion;

import com.relicweave.RelicWeave;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeMod;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Infusions for Celestial armor. Each infused piece grants the same attribute bonus, whatever its slot, so wearing
 * the same infusion on several pieces stacks. Which item produces which infusion is defined by
 * {@code relicweave:infusion} recipes.
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
    UPBOUND("upbound", ForgeMod.ENTITY_GRAVITY, -0.06, AttributeModifier.Operation.MULTIPLY_BASE);

    private final ResourceLocation id;
    private final Supplier<Attribute> attribute;
    private final double amount;
    private final AttributeModifier.Operation operation;

    InfusionType(String path, Supplier<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
        this.id = RelicWeave.id(path);
        this.attribute = attribute;
        this.amount = amount;
        this.operation = operation;
    }

    public ResourceLocation id() { return id; }
    public Attribute attribute() { return attribute.get(); }
    public double amount() { return amount; }
    public AttributeModifier.Operation operation() { return operation; }

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
