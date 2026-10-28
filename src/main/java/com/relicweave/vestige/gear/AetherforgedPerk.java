package com.relicweave.vestige.gear;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.hazard.HazardProtection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * The only Aetherforged bonus, "Vestige-attuned": each worn piece gives {@link #RIFT_PROTECTION_PER_PIECE} Rift
 * Saturation protection (max 0.24 for four pieces), and Aetherforged tools mine {@code relicweave} blocks that suit the
 * tool 15% faster. There is deliberately no full-set bonus.
 */
public final class AetherforgedPerk {
    public static final double RIFT_PROTECTION_PER_PIECE = 0.06;
    public static final float MINING_SPEED_MULTIPLIER = 1.15F;

    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private AetherforgedPerk() {}

    /** Number of Aetherforged armor pieces {@code player} wears (0-4). */
    public static int wornPieces(Player player) {
        int count = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (player.getItemBySlot(slot).getItem() instanceof ArmorItem armor && armor.getMaterial() == AetherforgedMaterials.ARMOR) {
                count++;
            }
        }
        return count;
    }

    /** Summed Rift Saturation protection of the worn pieces. */
    public static double riftProtection(Player player) {
        return wornPieces(player) * RIFT_PROTECTION_PER_PIECE;
    }

    /** True if {@code tool} is an Aetherforged tool whose mining perk applies to {@code state}. */
    public static boolean boostsMining(ItemStack tool, BlockState state) {
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (key == null || !RelicWeave.MOD_ID.equals(key.getNamespace())) {
            return false;
        }
        var item = tool.getItem();
        if (item instanceof PickaxeItem p && p.getTier() == AetherforgedMaterials.TIER) {
            return state.is(BlockTags.MINEABLE_WITH_PICKAXE);
        }
        if (item instanceof AxeItem a && a.getTier() == AetherforgedMaterials.TIER) {
            return state.is(BlockTags.MINEABLE_WITH_AXE);
        }
        if (item instanceof ShovelItem s && s.getTier() == AetherforgedMaterials.TIER) {
            return state.is(BlockTags.MINEABLE_WITH_SHOVEL);
        }
        if (item instanceof HoeItem h && h.getTier() == AetherforgedMaterials.TIER) {
            return state.is(BlockTags.MINEABLE_WITH_HOE);
        }
        return false;
    }

    /** Tooltip line shared by all nine pieces. */
    public static void appendTooltip(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.relicweave.aetherforged.perk").withStyle(ChatFormatting.DARK_AQUA));
    }

    @Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        private Setup() {}

        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(() -> HazardProtection.register(HazardProtection.Hazard.RIFT_SATURATION, AetherforgedPerk::riftProtection));
        }
    }

    @Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
    public static final class Events {
        private Events() {}

        @SubscribeEvent
        public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
            if (boostsMining(event.getEntity().getMainHandItem(), event.getState())) {
                event.setNewSpeed(event.getNewSpeed() * MINING_SPEED_MULTIPLIER);
            }
        }
    }
}
