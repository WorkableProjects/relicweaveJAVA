package com.relicweave.enchantment.vestige;

import com.relicweave.RelicWeave;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Mob effects used by the 0.0.4 enchantments and infusions (T16), e.g. Archivist's Mark. */
public final class VestigeEnchantEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, RelicWeave.MOD_ID);

    /** Harmful marker: the holder takes {@link ArchivistsMarkEvents#DAMAGE_MULTIPLIER} times damage from players. */
    public static final RegistryObject<MobEffect> ARCHIVISTS_MARK = EFFECTS.register("archivists_mark", ArchivistsMarkEffect::new);

    private VestigeEnchantEffects() {}
}
