package com.relicweave.item;

import com.relicweave.RelicWeave;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Sword, axe and pickaxe in one (0.0.4.5). Mines everything a pickaxe or an axe mines at its tier's speed, cuts cobwebs
 * like a sword, hits like a sword (with sweeping), strips/scrapes/unwaxes like an axe and can disable shields. Takes
 * enchantments for all three tools. Twice the durability of its tier; every use costs 1.
 */
public class MultitoolItem extends DiggerItem {
    /** Blocks it mines efficiently: {@code #minecraft:mineable/pickaxe} and {@code #minecraft:mineable/axe}. */
    public static final TagKey<Block> MINEABLE = TagKey.create(Registries.BLOCK, RelicWeave.id("mineable/multitool"));
    private static final Set<ToolAction> ACTIONS = Set.of(ToolActions.PICKAXE_DIG, ToolActions.AXE_DIG, ToolActions.SWORD_DIG,
            ToolActions.SWORD_SWEEP, ToolActions.AXE_STRIP, ToolActions.AXE_SCRAPE, ToolActions.AXE_WAX_OFF);
    /** Vanilla tools whose enchantments the Multitool accepts. */
    private static final ItemStack[] COMBINED = {new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.DIAMOND_AXE),
            new ItemStack(Items.DIAMOND_PICKAXE)};

    public MultitoolItem(Tier tier, Properties properties) {
        super(3.0F, -2.4F, tier, MINEABLE, properties.durability(tier.getUses() * 2));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.relicweave.multitool").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction action) {
        return ACTIONS.contains(action);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (state.is(Blocks.COBWEB)) {
            return 15.0F;
        }
        float speed = super.getDestroySpeed(stack, state);
        return state.is(BlockTags.SWORD_EFFICIENT) ? Math.max(speed, 1.5F) : speed;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return state.is(Blocks.COBWEB) || super.isCorrectToolForDrops(stack, state);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, e -> e.broadcastBreakEvent(net.minecraft.world.entity.EquipmentSlot.MAINHAND));
        return true;
    }

    @Override
    public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
        return true;
    }

    /** Any enchantment a sword, an axe or a pickaxe can take at an enchanting table. */
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        for (ItemStack tool : COMBINED) {
            if (enchantment.canApplyAtEnchantingTable(tool)) {
                return true;
            }
        }
        return super.canApplyAtEnchantingTable(stack, enchantment);
    }

    /** Strips logs, scrapes copper and removes wax, like an axe. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        ToolAction[] actions = {ToolActions.AXE_STRIP, ToolActions.AXE_SCRAPE, ToolActions.AXE_WAX_OFF};
        SoundEvent[] sounds = {SoundEvents.AXE_STRIP, SoundEvents.AXE_SCRAPE, SoundEvents.AXE_WAX_OFF};
        int[] events = {-1, 3005, 3004};
        for (int i = 0; i < actions.length; i++) {
            BlockState modified = state.getToolModifiedState(context, actions[i], false);
            if (modified == null) {
                continue;
            }
            level.playSound(player, pos, sounds[i], SoundSource.BLOCKS, 1.0F, 1.0F);
            if (events[i] >= 0) {
                level.levelEvent(player, events[i], pos, 0);
            }
            level.setBlock(pos, modified, 11);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, modified));
            if (player != null) {
                context.getItemInHand().hurtAndBreak(1, player, p -> p.broadcastBreakEvent(context.getHand()));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
