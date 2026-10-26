package com.relicweave.vestige.registry;

import com.relicweave.RelicWeave;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 0.0.4 crafting materials (plain items). Uses and sources are listed in docs/PLANS/0.0.4.md section 8.1. */
public final class VestigeMaterials {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RelicWeave.MOD_ID);

    // ---- Common / regional ----
    public static final RegistryObject<Item> AETHERITE_SHARD = item("aetherite_shard", Rarity.COMMON, false);
    public static final RegistryObject<Item> AETHERITE_DUST = item("aetherite_dust", Rarity.COMMON, false);
    public static final RegistryObject<Item> AETHERITE_INGOT = item("aetherite_ingot", Rarity.UNCOMMON, false);
    public static final RegistryObject<Item> RIFTGLASS = item("riftglass", Rarity.UNCOMMON, false);
    public static final RegistryObject<Item> RIFTGLASS_LENS = item("riftglass_lens", Rarity.UNCOMMON, false);
    public static final RegistryObject<Item> INSCRIBED_PAGE = item("inscribed_page", Rarity.COMMON, false);
    public static final RegistryObject<Item> SEALED_PAGE = item("sealed_page", Rarity.UNCOMMON, false);
    public static final RegistryObject<Item> MNEMONIC_FILAMENT = item("mnemonic_filament", Rarity.RARE, false);
    public static final RegistryObject<Item> RELIQUARY_SHARD = item("reliquary_shard", Rarity.UNCOMMON, false);
    public static final RegistryObject<Item> AURELIAN_RESIN = item("aurelian_resin", Rarity.COMMON, false);
    public static final RegistryObject<Item> AETHER_DOWN = item("aether_down", Rarity.COMMON, false);

    // ---- Boss components ----
    public static final RegistryObject<Item> SOVEREIGN_THORN = item("sovereign_thorn", Rarity.RARE, true);
    public static final RegistryObject<Item> LIMINAL_CHORD = item("liminal_chord", Rarity.RARE, true);
    public static final RegistryObject<Item> GRAVITY_HEART = item("gravity_heart", Rarity.RARE, true);
    public static final RegistryObject<Item> STARFORGED_CORE = item("starforged_core", Rarity.RARE, true);
    public static final RegistryObject<Item> CONCORDANT_SEAL = item("concordant_seal", Rarity.EPIC, true);
    public static final RegistryObject<Item> PRIME_MNEMONIC_CORE = item("prime_mnemonic_core", Rarity.EPIC, true);

    // ---- Crafted ----
    public static final RegistryObject<Item> CONCORDANT_ALLOY = item("concordant_alloy", Rarity.RARE, true);

    private VestigeMaterials() {}

    private static RegistryObject<Item> item(String name, Rarity rarity, boolean fireResistant) {
        return ITEMS.register(name, () -> {
            Item.Properties properties = new Item.Properties().rarity(rarity);
            return new Item(fireResistant ? properties.fireResistant() : properties);
        });
    }
}
