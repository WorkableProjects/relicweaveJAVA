package com.relicweave;

import com.relicweave.network.PurlingNetwork;
import com.relicweave.registry.ModBlockEntities;
import com.relicweave.registry.ModBlocks;
import com.relicweave.registry.ModEnchantments;
import com.relicweave.registry.ModMenus;
import com.relicweave.registry.ModRecipes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(RelicWeave.MOD_ID)
public class RelicWeave {
    public static final String MOD_ID = "relicweave";

    public RelicWeave() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModBlocks.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModEnchantments.ENCHANTMENTS.register(modBus);
        ModRecipes.RECIPE_TYPES.register(modBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::addCreative);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(PurlingNetwork::register);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.TRANSMUTATION_TABLE_ITEM);
        }
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
