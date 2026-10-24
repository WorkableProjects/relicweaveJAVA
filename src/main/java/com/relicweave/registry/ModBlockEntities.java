package com.relicweave.registry;

import com.relicweave.RelicWeave;
import com.relicweave.block.TransmutationTableBlockEntity;
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

    private ModBlockEntities() {}
}
