package com.relicweave.enchantment.vestige;

import com.relicweave.RelicWeave;
import com.relicweave.infusion.effects.vestige.GraviticTreadEffect;
import com.relicweave.registry.ModEnchantments;
import com.relicweave.vestige.hazard.HazardProtection;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * The single Gravity Shear contributor for Riftwalker (boots enchantment, 0.20 / 0.35) and the Gravitic Tread infusion
 * (0.30). Both live on boots, and together they are capped at {@value #COMBINED_CAP} here before the global cap applies.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GravityShearProtection {
    public static final double COMBINED_CAP = 0.5;

    private GravityShearProtection() {}

    /** Riftwalker's own reduction for {@code level}: 0.20 for I, 0.35 for II. */
    public static double riftwalker(int level) {
        return level <= 0 ? 0.0 : level == 1 ? 0.20 : 0.35;
    }

    /** {@code min(0.5, riftwalker + graviticTread)} for what {@code player} wears. */
    public static double protection(Player player) {
        int level = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.RIFTWALKER.get(), player.getItemBySlot(EquipmentSlot.FEET));
        return Math.min(COMBINED_CAP, riftwalker(level) + GraviticTreadEffect.protection(player));
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> HazardProtection.register(HazardProtection.Hazard.GRAVITY_SHEAR, GravityShearProtection::protection));
    }
}
