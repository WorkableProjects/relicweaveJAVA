package com.relicweave.gametest;

import com.mojang.authlib.GameProfile;
import com.relicweave.RelicWeave;
import com.relicweave.item.RelicItem;
import com.relicweave.registry.ModItems;
import com.relicweave.relic.RelicAugment;
import com.relicweave.relic.RelicType;
import com.relicweave.vestige.VestigeConfig;
import com.relicweave.vestige.gear.GearRegistry;
import com.relicweave.vestige.gear.ReliquaryLoomBlockEntity;
import com.relicweave.vestige.gear.ReliquaryLoomMenu;
import com.relicweave.vestige.registry.VestigeMaterials;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

import java.util.UUID;

/** Reliquary Loom: server-validated augment application, replacement, removal and rejection paths. */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LoomGameTests {
    private static final String TEMPLATE = "platform";
    private static final String CUSTOM_TAG = "relicweave_test_marker";

    /**
     * Used only when the real Surveyor's Lens is not registered (the Vestige relic items come from another task). It is a
     * genuine {@link RelicItem}, registered only in development runs (the GameTest system property is set) and behaves
     * identically for the Loom.
     */
    private static Item fallbackLens;

    /** Registers the fallback relic while the item registry is still open; never active in a normal game. */
    @Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class TestItems {
        private TestItems() {}

        @SubscribeEvent
        public static void onRegister(RegisterEvent event) {
            if (System.getProperty("forge.enabledGameTestNamespaces") == null) {
                return;
            }
            event.register(ForgeRegistries.Keys.ITEMS, helper -> {
                if (!ForgeRegistries.ITEMS.containsKey(RelicType.SURVEYORS_LENS.itemId())) {
                    fallbackLens = new RelicItem(RelicType.SURVEYORS_LENS, new Item.Properties().stacksTo(1));
                    helper.register(RelicWeave.id("loom_test_lens"), fallbackLens);
                }
            });
        }
    }

    private LoomGameTests() {}

    private static ItemStack lens() {
        Item registered = BuiltInRegistries.ITEM.get(RelicType.SURVEYORS_LENS.itemId());
        return new ItemStack(registered == Items.AIR ? fallbackLens : registered);
    }

    private static ItemStack taggedLens() {
        ItemStack stack = lens();
        stack.setHoverName(Component.literal("Heirloom Lens"));
        stack.getOrCreateTag().putInt(CUSTOM_TAG, 7);
        return stack;
    }

    private static ServerPlayer makePlayer(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "relicweave-loom-test"));
        player.setGameMode(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        return player;
    }

    private static ReliquaryLoomMenu menu(ServerPlayer player, ItemStackHandler items) {
        return new ReliquaryLoomMenu(1, player.getInventory(), items, ContainerLevelAccess.NULL);
    }

    private static ItemStackHandler stock(ItemStack relic, int filament, ItemStack material) {
        ItemStackHandler items = new ItemStackHandler(ReliquaryLoomMenu.SLOT_COUNT);
        items.setStackInSlot(ReliquaryLoomMenu.SLOT_RELIC, relic);
        if (filament > 0) {
            items.setStackInSlot(ReliquaryLoomMenu.SLOT_FILAMENT, new ItemStack(VestigeMaterials.MNEMONIC_FILAMENT.get(), filament));
        }
        items.setStackInSlot(ReliquaryLoomMenu.SLOT_MATERIAL, material);
        return items;
    }

    @GameTest(template = TEMPLATE)
    public static void applyPathConsumesExactCostsAndKeepsData(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        int filamentCost = VestigeConfig.LOOM_AUGMENT_FILAMENT_COST.get();
        ItemStackHandler items = stock(taggedLens(), filamentCost + 3, new ItemStack(VestigeMaterials.AETHERITE_INGOT.get(), RelicAugment.MATERIAL_COST + 1));
        ReliquaryLoomMenu menu = menu(player, items);

        helper.assertTrue(menu.clickMenuButton(player, ReliquaryLoomMenu.BUTTON_PATH_A), "Path A accepted");
        ItemStack relic = items.getStackInSlot(ReliquaryLoomMenu.SLOT_RELIC);
        helper.assertTrue(RelicAugment.SURVEYOR_WIDE.isOn(relic), "Path A written");
        CompoundTag data = relic.getTag().getCompound(RelicAugment.TAG);
        helper.assertTrue("surveyors_lens/wide".equals(data.getString("id")) && data.getInt("v") == RelicAugment.VERSION, "Tag format {id, v}");
        helper.assertTrue(items.getStackInSlot(ReliquaryLoomMenu.SLOT_FILAMENT).getCount() == 3, "Exactly the filament cost consumed");
        helper.assertTrue(items.getStackInSlot(ReliquaryLoomMenu.SLOT_MATERIAL).getCount() == 1, "Exactly the material cost consumed");
        helper.assertTrue("Heirloom Lens".equals(relic.getHoverName().getString()), "Relic name survives");
        helper.assertTrue(relic.getTag().getInt(CUSTOM_TAG) == 7, "Unrelated tag survives");
        helper.assertTrue(relic.getCount() == 1 && relic.getItem() == lens().getItem(), "Same relic item");

        helper.assertFalse(menu.clickMenuButton(player, ReliquaryLoomMenu.BUTTON_PATH_A), "Path already applied is refused");
        helper.assertTrue(items.getStackInSlot(ReliquaryLoomMenu.SLOT_FILAMENT).getCount() == 3, "Refusal costs nothing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void switchingPathReplacesTheOther(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        int filamentCost = VestigeConfig.LOOM_AUGMENT_FILAMENT_COST.get();
        ItemStackHandler items = stock(taggedLens(), filamentCost * 2, new ItemStack(VestigeMaterials.AETHERITE_INGOT.get(), RelicAugment.MATERIAL_COST));
        ReliquaryLoomMenu menu = menu(player, items);
        helper.assertTrue(menu.clickMenuButton(player, ReliquaryLoomMenu.BUTTON_PATH_A), "A applied");

        helper.assertFalse(menu.clickMenuButton(player, ReliquaryLoomMenu.BUTTON_PATH_B), "B needs Riftglass, not leftover ingots");
        helper.assertTrue(RelicAugment.SURVEYOR_WIDE.isOn(items.getStackInSlot(ReliquaryLoomMenu.SLOT_RELIC)), "A kept after refusal");

        items.setStackInSlot(ReliquaryLoomMenu.SLOT_MATERIAL, new ItemStack(VestigeMaterials.RIFTGLASS.get(), RelicAugment.MATERIAL_COST));
        helper.assertTrue(menu.clickMenuButton(player, ReliquaryLoomMenu.BUTTON_PATH_B), "B applied with the full price");
        ItemStack relic = items.getStackInSlot(ReliquaryLoomMenu.SLOT_RELIC);
        helper.assertTrue(RelicAugment.SURVEYOR_FOCUSED.isOn(relic) && !RelicAugment.SURVEYOR_WIDE.isOn(relic), "B replaced A");
        helper.assertTrue(items.getStackInSlot(ReliquaryLoomMenu.SLOT_FILAMENT).isEmpty(), "Full filament price paid twice in total");
        helper.assertTrue(items.getStackInSlot(ReliquaryLoomMenu.SLOT_MATERIAL).isEmpty(), "Material consumed");
        helper.assertTrue(relic.getTag().getInt(CUSTOM_TAG) == 7, "Unrelated tag survives the switch");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void removingRestoresTheOriginalRelic(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        for (ItemStack original : new ItemStack[] {taggedLens(), lens()}) {
            ItemStackHandler items = stock(original.copy(), VestigeConfig.LOOM_AUGMENT_FILAMENT_COST.get() + 5,
                    new ItemStack(VestigeMaterials.AETHERITE_INGOT.get(), RelicAugment.MATERIAL_COST));
            ReliquaryLoomMenu menu = menu(player, items);
            helper.assertTrue(menu.clickMenuButton(player, ReliquaryLoomMenu.BUTTON_PATH_A), "Applied");
            int filamentBefore = items.getStackInSlot(ReliquaryLoomMenu.SLOT_FILAMENT).getCount();
            helper.assertTrue(menu.clickMenuButton(player, ReliquaryLoomMenu.BUTTON_REMOVE), "Removed");
            ItemStack relic = items.getStackInSlot(ReliquaryLoomMenu.SLOT_RELIC);
            helper.assertTrue(ItemStack.matches(original, relic), "Stack equals the original (same item, count and NBT)");
            helper.assertTrue(RelicAugment.of(relic).isEmpty(), "No augment left");
            int removed = filamentBefore - items.getStackInSlot(ReliquaryLoomMenu.SLOT_FILAMENT).getCount();
            helper.assertTrue(removed == VestigeConfig.LOOM_RESET_FILAMENT_COST.get(), "Removal cost only the reset filament price");
            helper.assertTrue(items.getStackInSlot(ReliquaryLoomMenu.SLOT_MATERIAL).isEmpty(), "Materials are not refunded");
            helper.assertFalse(menu.clickMenuButton(player, ReliquaryLoomMenu.BUTTON_REMOVE), "Nothing left to remove");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void invalidAugmentDataReadsAsNone(GameTestHelper helper) {
        ItemStack unknown = lens();
        CompoundTag bogus = new CompoundTag();
        bogus.putString("id", "surveyors_lens/bogus");
        bogus.putInt("v", 1);
        unknown.getOrCreateTag().put(RelicAugment.TAG, bogus);
        helper.assertTrue(RelicAugment.of(unknown).isEmpty(), "Unknown id reads as none");

        ItemStack wrongRelic = lens();
        CompoundTag other = new CompoundTag();
        other.putString("id", RelicAugment.GRAVITY_NAIL_LONG.id());
        other.putInt("v", 1);
        wrongRelic.getOrCreateTag().put(RelicAugment.TAG, other);
        helper.assertTrue(RelicAugment.of(wrongRelic).isEmpty(), "Another relic's augment reads as none");

        ItemStack malformed = lens();
        malformed.getOrCreateTag().put(RelicAugment.TAG, StringTag.valueOf("garbage"));
        helper.assertTrue(RelicAugment.of(malformed).isEmpty(), "Malformed tag reads as none");

        ItemStack future = lens();
        CompoundTag newer = new CompoundTag();
        newer.putString("id", RelicAugment.SURVEYOR_WIDE.id());
        newer.putInt("v", RelicAugment.VERSION + 5);
        future.getOrCreateTag().put(RelicAugment.TAG, newer);
        helper.assertTrue(RelicAugment.of(future).isEmpty(), "Newer data version reads as none");

        // The Loom treats such a relic as a plain base relic: nothing to remove, but a path can be applied cleanly.
        ServerPlayer player = makePlayer(helper);
        ItemStackHandler items = stock(unknown, VestigeConfig.LOOM_AUGMENT_FILAMENT_COST.get() + 2,
                new ItemStack(VestigeMaterials.AETHERITE_INGOT.get(), RelicAugment.MATERIAL_COST));
        ReliquaryLoomMenu menu = menu(player, items);
        helper.assertTrue(menu.currentAugment().isEmpty(), "Menu shows no current augment");
        helper.assertTrue(menu.clickMenuButton(player, ReliquaryLoomMenu.BUTTON_PATH_B) == false, "Path B lacks Riftglass");
        helper.assertTrue(menu.clickMenuButton(player, ReliquaryLoomMenu.BUTTON_PATH_A), "Path A overwrites the junk data");
        helper.assertTrue(RelicAugment.SURVEYOR_WIDE.isOn(items.getStackInSlot(ReliquaryLoomMenu.SLOT_RELIC)), "Valid augment written");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void unsupportedRelicIsRejected(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        ItemStack echo = new ItemStack(ModItems.ECHO_CHARM.get());
        helper.assertFalse(ReliquaryLoomMenu.accepts(ReliquaryLoomMenu.SLOT_RELIC, echo), "Echo Charm may not be placed in the relic slot");
        helper.assertFalse(ReliquaryLoomMenu.accepts(ReliquaryLoomMenu.SLOT_RELIC, new ItemStack(Items.DIAMOND)), "Non-relics are refused");
        helper.assertTrue(ReliquaryLoomMenu.accepts(ReliquaryLoomMenu.SLOT_RELIC, lens()), "A supported relic is accepted");
        helper.assertFalse(ReliquaryLoomMenu.accepts(ReliquaryLoomMenu.SLOT_FILAMENT, new ItemStack(Items.DIAMOND)), "Filament slot only takes filament");
        helper.assertTrue(ReliquaryLoomMenu.accepts(ReliquaryLoomMenu.SLOT_MATERIAL, new ItemStack(VestigeMaterials.RIFTGLASS.get())), "Material slot takes Riftglass");
        helper.assertFalse(ReliquaryLoomMenu.accepts(ReliquaryLoomMenu.SLOT_MATERIAL, new ItemStack(Items.DIAMOND)), "Material slot refuses others");

        // Even if the stack is forced into the slot (e.g. by a bad client), the server refuses every button.
        ItemStackHandler items = stock(echo, 10, new ItemStack(VestigeMaterials.AETHERITE_INGOT.get(), 10));
        ReliquaryLoomMenu menu = menu(player, items);
        for (int button = ReliquaryLoomMenu.BUTTON_PATH_A; button <= ReliquaryLoomMenu.BUTTON_REMOVE; button++) {
            helper.assertFalse(menu.clickMenuButton(player, button), "Button " + button + " refused for an unsupported relic");
        }
        helper.assertTrue(items.getStackInSlot(ReliquaryLoomMenu.SLOT_FILAMENT).getCount() == 10
                && items.getStackInSlot(ReliquaryLoomMenu.SLOT_MATERIAL).getCount() == 10, "Costs untouched");
        helper.assertFalse(items.getStackInSlot(ReliquaryLoomMenu.SLOT_RELIC).hasTag(), "Relic untouched");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void menuRefusesWithoutCostsAndBadIds(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        ItemStack relic = taggedLens();
        ItemStack snapshot = relic.copy();

        ItemStackHandler empty = stock(relic, 0, ItemStack.EMPTY);
        ReliquaryLoomMenu menu = menu(player, empty);
        helper.assertFalse(menu.clickMenuButton(player, ReliquaryLoomMenu.BUTTON_PATH_A), "No costs: refused");
        helper.assertFalse(menu.clickMenuButton(player, ReliquaryLoomMenu.BUTTON_PATH_B), "No costs: refused");
        helper.assertTrue(menu.unavailableReason(ReliquaryLoomMenu.BUTTON_PATH_A) != null, "The screen gets a reason");
        helper.assertTrue(ItemStack.matches(snapshot, empty.getStackInSlot(ReliquaryLoomMenu.SLOT_RELIC)), "Relic unchanged");

        // Filament but wrong or too little material.
        ItemStackHandler partial = stock(relic.copy(), VestigeConfig.LOOM_AUGMENT_FILAMENT_COST.get(), new ItemStack(VestigeMaterials.RIFTGLASS.get(), RelicAugment.MATERIAL_COST));
        ReliquaryLoomMenu partialMenu = menu(player, partial);
        helper.assertFalse(partialMenu.clickMenuButton(player, ReliquaryLoomMenu.BUTTON_PATH_A), "Path A needs Aetherite Ingots, not Riftglass");
        partial.setStackInSlot(ReliquaryLoomMenu.SLOT_MATERIAL, new ItemStack(VestigeMaterials.AETHERITE_INGOT.get(), RelicAugment.MATERIAL_COST - 1));
        helper.assertFalse(partialMenu.clickMenuButton(player, ReliquaryLoomMenu.BUTTON_PATH_A), "Too little material");
        helper.assertTrue(partial.getStackInSlot(ReliquaryLoomMenu.SLOT_FILAMENT).getCount() == VestigeConfig.LOOM_AUGMENT_FILAMENT_COST.get(), "Nothing consumed");

        helper.assertFalse(partialMenu.clickMenuButton(player, 7), "Unknown button id refused");
        helper.assertFalse(partialMenu.clickMenuButton(player, -1), "Negative button id refused");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void loomBlockEntityExposesNoAutomation(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, GearRegistry.RELIQUARY_LOOM.get());
        ReliquaryLoomBlockEntity loom = (ReliquaryLoomBlockEntity) helper.getBlockEntity(pos);
        helper.assertTrue(loom != null, "Block entity exists");
        helper.assertFalse(loom.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent(), "No item handler capability: hoppers cannot touch the relic");
        helper.assertFalse(loom.getItems().isItemValid(ReliquaryLoomMenu.SLOT_RELIC, new ItemStack(ModItems.ECHO_CHARM.get())), "Block entity filters the relic slot too");
        loom.getItems().setStackInSlot(ReliquaryLoomMenu.SLOT_FILAMENT, new ItemStack(VestigeMaterials.MNEMONIC_FILAMENT.get(), 3));
        var tag = loom.saveWithoutMetadata();
        helper.assertTrue(tag.contains("Items"), "Contents are saved");
        helper.succeed();
    }
}
