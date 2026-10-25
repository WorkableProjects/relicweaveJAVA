package com.relicweave.infusion;

import com.relicweave.RelicWeave;
import com.relicweave.item.CelestialArmorItem;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Adds each infusion's attribute bonus to Celestial armor while worn in its own slot (also shown in tooltips). */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class InfusionEvents {
    private InfusionEvents() {}

    @SubscribeEvent
    public static void onItemAttributes(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof CelestialArmorItem armor) || event.getSlotType() != armor.getEquipmentSlot()) {
            return;
        }
        int slotIndex = armor.getEquipmentSlot().getIndex();
        for (InfusionType infusion : Infusions.get(stack)) {
            if (!infusion.hasAttribute()) {
                continue; // effect infusions are handled in com.relicweave.infusion.effects
            }
            event.addModifier(infusion.attribute(), new AttributeModifier(infusion.modifierId(slotIndex),
                    RelicWeave.MOD_ID + ":infusion/" + infusion.id().getPath(), infusion.amount(), infusion.operation()));
        }
    }
}
