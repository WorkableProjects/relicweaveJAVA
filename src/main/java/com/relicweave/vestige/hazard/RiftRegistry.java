package com.relicweave.vestige.hazard;

import com.google.common.collect.ImmutableSet;
import com.relicweave.RelicWeave;
import com.relicweave.vestige.hazard.rift.RiftFissureBlock;
import com.relicweave.vestige.hazard.rift.RiftSicknessEffect;
import com.relicweave.vestige.hazard.rift.WardSalveItem;
import com.relicweave.vestige.hazard.rift.WardedEffect;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.registries.RegistryObject;
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

    public static final RegistryObject<Block> RIFT_FISSURE = BLOCKS.register("rift_fissure", RiftFissureBlock::new);
    public static final RegistryObject<Item> RIFT_FISSURE_ITEM = ITEMS.register("rift_fissure",
            () -> new BlockItem(RIFT_FISSURE.get(), new Item.Properties()));
    public static final RegistryObject<Item> WARD_SALVE = ITEMS.register("ward_salve", WardSalveItem::new);

    public static final RegistryObject<MobEffect> WARDED = EFFECTS.register("warded", WardedEffect::new);
    public static final RegistryObject<MobEffect> RIFT_SICKNESS = EFFECTS.register("rift_sickness", RiftSicknessEffect::new);

    /** POI for the fissure: Rift Saturation looks for one within 5 blocks of the player. */
    public static final RegistryObject<PoiType> RIFT_FISSURE_POI = POI_TYPES.register("rift_fissure",
            () -> new PoiType(ImmutableSet.copyOf(RIFT_FISSURE.get().getStateDefinition().getPossibleStates()), 0, 1));

    private RiftRegistry() {}
}
