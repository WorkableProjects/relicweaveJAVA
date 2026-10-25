package com.relicweave.registry;

import com.relicweave.RelicWeave;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RelicWeave.MOD_ID);

    public static final RegistryObject<CreativeModeTab> RELICWEAVE = TABS.register("relicweave", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.relicweave"))
            .icon(() -> new ItemStack(ModBlocks.TRANSMUTATION_TABLE_ITEM.get()))
            .displayItems((parameters, output) -> {
                for (RegistryObject<Item> item : ModBlocks.ITEMS.getEntries()) {
                    output.accept(item.get());
                }
                for (RegistryObject<Item> item : ModItems.ITEMS.getEntries()) {
                    output.accept(item.get());
                }
                for (RegistryObject<Enchantment> enchantment : ModEnchantments.ENCHANTMENTS.getEntries()) {
                    Enchantment e = enchantment.get();
                    for (int level = 1; level <= e.getMaxLevel(); level++) {
                        output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(e, level)));
                    }
                }
            })
            .build());

    private ModCreativeTabs() {}
}
