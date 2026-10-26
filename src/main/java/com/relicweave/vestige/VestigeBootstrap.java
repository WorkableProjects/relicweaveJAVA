package com.relicweave.vestige;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.boss.BossRegistry;
import com.relicweave.vestige.boss.a.BossesA;
import com.relicweave.vestige.boss.b.BossesB;
import com.relicweave.vestige.boss.c.BossesC;
import com.relicweave.vestige.entity.beast.BeastMobs;
import com.relicweave.vestige.entity.ethereal.EtherealMobs;
import com.relicweave.vestige.entity.humanoid.HumanoidMobs;
import com.relicweave.vestige.gate.GateRegistry;
import com.relicweave.vestige.hazard.FieldRegistry;
import com.relicweave.vestige.hazard.RiftRegistry;
import com.relicweave.vestige.network.VestigeNetwork;
import com.relicweave.vestige.registry.VestigeMaterials;
import com.relicweave.vestige.registry.VestigeSounds;
import com.relicweave.vestige.world.VestigeWorldBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * Registers everything added by 0.0.4 (The Vestige Update). Each feature area owns its own registry class; this class
 * only wires them to the mod bus, so feature code never needs to touch {@link RelicWeave}.
 */
public final class VestigeBootstrap {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RelicWeave.MOD_ID);

    /** Item registries shown in the Vestige creative tab, in display order. */
    private static final List<DeferredRegister<Item>> TAB_ITEMS = List.of(
            VestigeWorldBlocks.ITEMS,
            GateRegistry.ITEMS,
            RiftRegistry.ITEMS,
            FieldRegistry.ITEMS,
            VestigeMaterials.ITEMS,
            HumanoidMobs.ITEMS,
            BeastMobs.ITEMS,
            EtherealMobs.ITEMS,
            BossRegistry.ITEMS,
            BossesA.ITEMS,
            BossesB.ITEMS,
            BossesC.ITEMS);

    public static final RegistryObject<CreativeModeTab> VESTIGE_TAB = TABS.register("the_vestige", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.relicweave.the_vestige"))
            .icon(() -> new ItemStack(VestigeWorldBlocks.RUNE_VESTIGE_BRICKS.get()))
            .displayItems((parameters, output) -> {
                for (DeferredRegister<Item> register : TAB_ITEMS) {
                    for (RegistryObject<Item> item : register.getEntries()) {
                        output.accept(item.get());
                    }
                }
            })
            .build());

    private VestigeBootstrap() {}

    public static void register(IEventBus modBus) {
        VestigeWorldBlocks.BLOCKS.register(modBus);
        VestigeWorldBlocks.ITEMS.register(modBus);
        GateRegistry.BLOCKS.register(modBus);
        GateRegistry.ITEMS.register(modBus);
        GateRegistry.BLOCK_ENTITIES.register(modBus);
        RiftRegistry.BLOCKS.register(modBus);
        RiftRegistry.ITEMS.register(modBus);
        RiftRegistry.EFFECTS.register(modBus);
        RiftRegistry.POI_TYPES.register(modBus);
        FieldRegistry.BLOCKS.register(modBus);
        FieldRegistry.ITEMS.register(modBus);
        FieldRegistry.ENTITIES.register(modBus);
        FieldRegistry.POI_TYPES.register(modBus);
        VestigeMaterials.ITEMS.register(modBus);
        for (DeferredRegister<?> register : List.of(
                HumanoidMobs.ENTITIES, HumanoidMobs.ITEMS, HumanoidMobs.SOUNDS,
                BeastMobs.ENTITIES, BeastMobs.ITEMS, BeastMobs.SOUNDS,
                EtherealMobs.ENTITIES, EtherealMobs.ITEMS, EtherealMobs.SOUNDS,
                BossRegistry.BLOCKS, BossRegistry.ITEMS, BossRegistry.BLOCK_ENTITIES,
                BossesA.ENTITIES, BossesA.ITEMS, BossesA.SOUNDS,
                BossesB.ENTITIES, BossesB.ITEMS, BossesB.SOUNDS,
                BossesC.ENTITIES, BossesC.ITEMS, BossesC.SOUNDS)) {
            register.register(modBus);
        }
        VestigeSounds.SOUNDS.register(modBus);
        TABS.register(modBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, VestigeConfig.SPEC, VestigeConfig.FILE_NAME);
        modBus.addListener(VestigeBootstrap::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(VestigeNetwork::register);
    }
}
