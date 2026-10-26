package com.relicweave.vestige.hazard;

import com.relicweave.RelicWeave;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Gravity Shear, Starfall Impact, Null Pocket and Hushfield content (T04). */
public final class FieldRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RelicWeave.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RelicWeave.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RelicWeave.MOD_ID);

    /** Point-of-interest types for cheap proximity lookups (e.g. null_ward) via PoiManager. */
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(ForgeRegistries.POI_TYPES, RelicWeave.MOD_ID);

    private FieldRegistry() {}
}
