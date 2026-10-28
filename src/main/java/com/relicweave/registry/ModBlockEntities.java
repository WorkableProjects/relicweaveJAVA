package com.relicweave.registry;

import com.relicweave.RelicWeave;
import com.relicweave.block.TransmutationTableBlockEntity;
import com.relicweave.block.furnace.SuperFurnaceBlockEntity;
import com.relicweave.block.infuser.CelestialInfuserBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RelicWeave.MOD_ID);

    @SuppressWarnings("DataFlowIssue") // vanilla accepts a null data-fixer type
    public static final RegistryObject<BlockEntityType<TransmutationTableBlockEntity>> TRANSMUTATION_TABLE =
            BLOCK_ENTITIES.register("transmutation_table", () -> BlockEntityType.Builder
                    .of(TransmutationTableBlockEntity::new, ModBlocks.TRANSMUTATION_TABLE.get())
                    .build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<CelestialInfuserBlockEntity>> CELESTIAL_INFUSER =
            BLOCK_ENTITIES.register("celestial_infuser", () -> BlockEntityType.Builder
                    .of(CelestialInfuserBlockEntity::new, ModBlocks.CELESTIAL_INFUSER.get())
                    .build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<SuperFurnaceBlockEntity>> SUPER_FURNACE =
            BLOCK_ENTITIES.register("super_furnace", () -> BlockEntityType.Builder
                    .of(SuperFurnaceBlockEntity::new, ModBlocks.SUPER_FURNACES.stream().map(RegistryObject::get)
                            .toArray(net.minecraft.world.level.block.Block[]::new))
                    .build(null));

    private ModBlockEntities() {}
}
