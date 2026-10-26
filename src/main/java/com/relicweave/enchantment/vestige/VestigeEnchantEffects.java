package com.relicweave.enchantment.vestige;

import com.relicweave.RelicWeave;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Mob effects used by the 0.0.4 enchantments and infusions (T16), e.g. Archivist's Mark. */
public final class VestigeEnchantEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, RelicWeave.MOD_ID);

    private VestigeEnchantEffects() {}
}
