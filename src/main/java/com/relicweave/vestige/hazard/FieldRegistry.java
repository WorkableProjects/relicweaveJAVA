package com.relicweave.vestige.hazard;

import com.relicweave.RelicWeave;
import com.google.common.collect.ImmutableSet;
import com.relicweave.vestige.hazard.field.GravityAnchorBlock;
import com.relicweave.vestige.hazard.field.NullWardBlock;
import com.relicweave.vestige.hazard.field.StarfallMarker;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.registries.RegistryObject;
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

    public static final RegistryObject<Block> NULL_WARD = BLOCKS.register("null_ward", NullWardBlock::new);
    public static final RegistryObject<Block> GRAVITY_ANCHOR = BLOCKS.register("gravity_anchor", GravityAnchorBlock::new);
    public static final RegistryObject<Item> NULL_WARD_ITEM = ITEMS.register("null_ward",
            () -> new BlockItem(NULL_WARD.get(), new Item.Properties()));
    public static final RegistryObject<Item> GRAVITY_ANCHOR_ITEM = ITEMS.register("gravity_anchor",
            () -> new BlockItem(GRAVITY_ANCHOR.get(), new Item.Properties()));

    public static final RegistryObject<EntityType<StarfallMarker>> STARFALL_MARKER = ENTITIES.register("starfall_marker",
            () -> EntityType.Builder.<StarfallMarker>of(StarfallMarker::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).fireImmune().noSummon().clientTrackingRange(8).updateInterval(10)
                    .build("starfall_marker"));

    public static final RegistryObject<PoiType> NULL_WARD_POI = POI_TYPES.register("null_ward",
            () -> new PoiType(ImmutableSet.copyOf(NULL_WARD.get().getStateDefinition().getPossibleStates()), 0, 1));
    public static final RegistryObject<PoiType> GRAVITY_ANCHOR_POI = POI_TYPES.register("gravity_anchor",
            () -> new PoiType(ImmutableSet.copyOf(GRAVITY_ANCHOR.get().getStateDefinition().getPossibleStates()), 0, 1));

    private FieldRegistry() {}
}
