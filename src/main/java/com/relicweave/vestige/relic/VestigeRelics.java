package com.relicweave.vestige.relic;

import com.relicweave.RelicWeave;
import com.relicweave.item.RelicItem;
import com.relicweave.relic.RelicAugment;
import com.relicweave.relic.RelicType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Optional;

/** 0.0.4 relic items (T15) and the shared augment helpers used by the six relics' server code. */
public final class VestigeRelics {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RelicWeave.MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RelicWeave.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RelicWeave.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, RelicWeave.MOD_ID);

    public static final RegistryObject<Item> SURVEYORS_LENS = relic(RelicType.SURVEYORS_LENS, Rarity.UNCOMMON);
    public static final RegistryObject<Item> GRAVITY_NAIL = relic(RelicType.GRAVITY_NAIL, Rarity.RARE);
    public static final RegistryObject<Item> CYCLE_DIAL = relic(RelicType.CYCLE_DIAL, Rarity.UNCOMMON);
    public static final RegistryObject<Item> RIFTGLASS_LANTERN = relic(RelicType.RIFTGLASS_LANTERN, Rarity.RARE);
    public static final RegistryObject<Item> MNEMONIC_SPINDLE = relic(RelicType.MNEMONIC_SPINDLE, Rarity.RARE);
    public static final RegistryObject<Item> SEALKEEPERS_SIGNET = relic(RelicType.SEALKEEPERS_SIGNET, Rarity.EPIC);

    private VestigeRelics() {}

    private static RegistryObject<Item> relic(RelicType type, Rarity rarity) {
        return ITEMS.register(type.itemPath(), () -> new RelicItem(type, new Item.Properties().stacksTo(1).rarity(rarity)));
    }

    /** The augment on {@code stack}, or empty. Never throws. */
    public static Optional<RelicAugment> augment(ItemStack stack) {
        return stack.isEmpty() ? Optional.empty() : RelicAugment.of(stack);
    }

    /** True if {@code stack} carries {@code augment}. */
    public static boolean has(ItemStack stack, RelicAugment augment) {
        return !stack.isEmpty() && augment.isOn(stack);
    }

    /**
     * Configured cooldown (ticks) of {@code type} adjusted by the augment on {@code stack}: Wide Survey x1.25, Focused
     * Survey x0.7, Long Anchor x1.3, Quick Anchor x0.65, Swift Reading x0.5. Other augments leave it unchanged.
     */
    public static int cooldownTicks(RelicType type, ItemStack stack) {
        double factor = 1.0;
        Optional<RelicAugment> augment = augment(stack);
        if (augment.isPresent()) {
            factor = switch (augment.get()) {
                case SURVEYOR_WIDE -> 1.25;
                case SURVEYOR_FOCUSED -> 0.7;
                case GRAVITY_NAIL_LONG -> 1.3;
                case GRAVITY_NAIL_QUICK -> 0.65;
                case SPINDLE_SWIFT -> 0.5;
                default -> 1.0;
            };
        }
        return Math.max(1, (int) Math.round(type.cooldownTicks() * factor));
    }
}
