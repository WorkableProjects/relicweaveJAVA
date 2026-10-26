package com.relicweave.vestige.hazard;

import com.relicweave.RelicWeave;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Rift Saturation and Concordance Cycle content: Ward Salve, effects, rift fissure block (T03). */
public final class RiftRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RelicWeave.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RelicWeave.MOD_ID);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, RelicWeave.MOD_ID);

    /** Point-of-interest types for cheap proximity lookups (e.g. rift_fissure) via PoiManager. */
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(ForgeRegistries.POI_TYPES, RelicWeave.MOD_ID);

    private RiftRegistry() {}
}
