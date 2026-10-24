package com.relicweave.registry;

import com.relicweave.RelicWeave;
import com.relicweave.menu.TransmutationTableMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, RelicWeave.MOD_ID);

    public static final RegistryObject<MenuType<TransmutationTableMenu>> TRANSMUTATION_TABLE =
            MENUS.register("transmutation_table", () -> IForgeMenuType.create(TransmutationTableMenu::new));

    private ModMenus() {}
}
