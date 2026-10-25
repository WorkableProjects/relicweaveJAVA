package com.relicweave.infusion;

import com.relicweave.item.CelestialArmorItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Reads and writes the infusions stored on Celestial armor (NBT list of infusion IDs under {@value #TAG}). */
public final class Infusions {
    public static final String TAG = "RelicWeaveInfusions";
    public static final int MAX_INFUSIONS = 3;

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

    /** True if {@code stack} is Celestial armor with a free infusion slot that does not already hold {@code type}. */
    public static boolean canInfuse(ItemStack stack, InfusionType type) {
        if (!(stack.getItem() instanceof CelestialArmorItem)) {
            return false;
        }
        List<InfusionType> current = get(stack);
        return current.size() < MAX_INFUSIONS && !current.contains(type);
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
