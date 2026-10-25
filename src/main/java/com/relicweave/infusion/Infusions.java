package com.relicweave.infusion;

import com.relicweave.item.CelestialArmorItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Reads and writes the infusions stored on Celestial armor (NBT list of infusion IDs under {@value #TAG}). */
public final class Infusions {
    public static final String TAG = "RelicWeaveInfusions";
    public static final int MAX_INFUSIONS = 3;
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private Infusions() {}

    public static List<InfusionType> get(ItemStack stack) {
        List<InfusionType> result = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG, Tag.TAG_LIST)) {
            return result;
        }
        ListTag list = tag.getList(TAG, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            InfusionType type = id == null ? null : InfusionType.byId(id);
            if (type != null && !result.contains(type)) {
                result.add(type);
            }
        }
        return result;
    }

    public static boolean has(ItemStack stack, InfusionType type) {
        return get(stack).contains(type);
    }

    /**
     * True if {@code stack} is Celestial armor that {@code type} fits ({@link InfusionType#fits}), with a free infusion
     * slot that does not already hold {@code type}.
     */
    public static boolean canInfuse(ItemStack stack, InfusionType type) {
        return check(stack, type) == Result.OK;
    }

    /** Why an infusion can or cannot be applied; used for machine status messages. */
    public enum Result { OK, NOT_CELESTIAL, WRONG_PIECE, ALREADY_PRESENT, LIMIT_REACHED }

    public static Result check(ItemStack stack, InfusionType type) {
        if (!(stack.getItem() instanceof CelestialArmorItem armor)) {
            return Result.NOT_CELESTIAL;
        }
        if (!type.fits(armor.getEquipmentSlot())) {
            return Result.WRONG_PIECE;
        }
        List<InfusionType> current = get(stack);
        if (current.contains(type)) {
            return Result.ALREADY_PRESENT;
        }
        return current.size() < MAX_INFUSIONS ? Result.OK : Result.LIMIT_REACHED;
    }

    /** True if {@code entity} wears a Celestial armor piece carrying {@code type} in that piece's own slot. */
    public static boolean isWearing(LivingEntity entity, InfusionType type) {
        if (type.slot() != null) {
            ItemStack worn = entity.getItemBySlot(type.slot());
            return worn.getItem() instanceof CelestialArmorItem && has(worn, type);
        }
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack worn = entity.getItemBySlot(slot);
            if (worn.getItem() instanceof CelestialArmorItem && has(worn, type)) {
                return true;
            }
        }
        return false;
    }

    /** Returns a copy of {@code stack} (count 1) with {@code type} added. Caller must check {@link #canInfuse} first. */
    public static ItemStack withInfusion(ItemStack stack, InfusionType type) {
        ItemStack result = stack.copyWithCount(1);
        CompoundTag tag = result.getOrCreateTag();
        ListTag list = tag.contains(TAG, Tag.TAG_LIST) ? tag.getList(TAG, Tag.TAG_STRING) : new ListTag();
        list.add(StringTag.valueOf(type.id().toString()));
        tag.put(TAG, list);
        return result;
    }
}
