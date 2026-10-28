package com.relicweave.vestige.boss;

import com.relicweave.RelicWeave;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/** Shared boss infrastructure: the Boss Altar, the Seal Gate and the Vestige Upgrade Template. */
public final class BossRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RelicWeave.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RelicWeave.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RelicWeave.MOD_ID);

    /** Placed by structures in boss arenas; its block entity names the boss it summons. Unbreakable in survival. */
    public static final RegistryObject<Block> BOSS_ALTAR = BLOCKS.register("boss_altar", () -> new BossAltarBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(-1.0F, 3600000.0F).noLootTable()
                    .lightLevel(state -> 9).sound(SoundType.LODESTONE)));
    public static final RegistryObject<Item> BOSS_ALTAR_ITEM = ITEMS.register("boss_altar",
            () -> new BlockItem(BOSS_ALTAR.get(), new Item.Properties().rarity(Rarity.EPIC)));

    /** Crown of Concordance gate: lets through only players who have defeated the Warden of Seals. */
    public static final RegistryObject<Block> SEAL_GATE = BLOCKS.register("seal_gate", () -> new SealGateBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(-1.0F, 3600000.0F).noLootTable()
                    .lightLevel(state -> 6).sound(SoundType.METAL)));
    public static final RegistryObject<Item> SEAL_GATE_ITEM = ITEMS.register("seal_gate",
            () -> new BlockItem(SEAL_GATE.get(), new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<BlockEntityType<BossAltarBlockEntity>> BOSS_ALTAR_BE = BLOCK_ENTITIES.register("boss_altar",
            () -> BlockEntityType.Builder.of(BossAltarBlockEntity::new, BOSS_ALTAR.get()).build(null));

    /** Netherite gear + this template + Concordant Alloy -> Aetherforged gear (smithing table). */
    public static final RegistryObject<Item> VESTIGE_UPGRADE_TEMPLATE = ITEMS.register("vestige_upgrade_template", () -> new SmithingTemplateItem(
            Component.translatable("item.relicweave.smithing_template.vestige_upgrade.applies_to").withStyle(ChatFormatting.BLUE),
            Component.translatable("item.relicweave.smithing_template.vestige_upgrade.ingredients").withStyle(ChatFormatting.BLUE),
            Component.translatable("upgrade.relicweave.vestige_upgrade").withStyle(ChatFormatting.GRAY),
            Component.translatable("item.relicweave.smithing_template.vestige_upgrade.base_slot_description"),
            Component.translatable("item.relicweave.smithing_template.vestige_upgrade.additions_slot_description"),
            List.of(slot("helmet"), slot("sword"), slot("chestplate"), slot("pickaxe"), slot("leggings"), slot("axe"),
                    slot("boots"), slot("hoe"), slot("shovel")),
            List.of(new ResourceLocation("item/empty_slot_ingot"))));

    private BossRegistry() {}

    private static ResourceLocation slot(String piece) {
        return switch (piece) {
            case "sword", "pickaxe", "axe", "hoe", "shovel" -> new ResourceLocation("item/empty_slot_" + piece);
            default -> new ResourceLocation("item/empty_armor_slot_" + piece);
        };
    }
}
