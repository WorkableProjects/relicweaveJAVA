package com.relicweave.enchantment.effects;

import com.relicweave.RelicWeave;
import com.relicweave.registry.ModEnchantments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Cobbled Up: a chance for one extra cobblestone when a pickaxe breaks stone. Implemented on
 * {@link BlockEvent.BreakEvent} (server side only) so the extra item is a plain bonus on top of vanilla drops.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class CobbledUpEvents {
    public static final float CHANCE_PER_LEVEL = 0.20F;

    private CobbledUpEvents() {}

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Player player = event.getPlayer();
        if (player.isCreative()) {
            return;
        }
        ItemStack tool = player.getMainHandItem();
        int enchantLevel = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.COBBLED_UP.get(), tool);
        BlockState state = event.getState();
        if (enchantLevel <= 0 || !(tool.getItem() instanceof PickaxeItem) || !player.hasCorrectToolForDrops(state)) {
            return;
        }
        boolean deepslate = state.is(Blocks.DEEPSLATE) || state.is(Blocks.COBBLED_DEEPSLATE);
        if (!(state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Blocks.COBBLESTONE) || deepslate)) {
            return;
        }
        if (level.getRandom().nextFloat() < CHANCE_PER_LEVEL * enchantLevel) {
            Block.popResource(level, event.getPos(), new ItemStack(deepslate ? Items.COBBLED_DEEPSLATE : Items.COBBLESTONE));
        }
    }
}
