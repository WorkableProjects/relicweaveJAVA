package com.relicweave.registry;

import com.relicweave.RelicWeave;
import com.relicweave.enchantment.KeenEdgeEnchantment;
import com.relicweave.enchantment.PurlingEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, RelicWeave.MOD_ID);

    public static final RegistryObject<Enchantment> KEEN_EDGE = ENCHANTMENTS.register("keen_edge", KeenEdgeEnchantment::new);
    public static final RegistryObject<Enchantment> PURLING = ENCHANTMENTS.register("purling", PurlingEnchantment::new);

    private ModEnchantments() {}
}
