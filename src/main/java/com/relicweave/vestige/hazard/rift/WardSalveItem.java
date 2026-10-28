package com.relicweave.vestige.hazard.rift;

import com.relicweave.vestige.hazard.RiftRegistry;
import com.relicweave.vestige.registry.VestigeSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Drinkable that grants {@code warded} (50% slower Rift Saturation) for three minutes and returns a glass bottle. */
public class WardSalveItem extends Item {
    public static final int DURATION_TICKS = 3600;

    public WardSalveItem() {
        super(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return VestigeSounds.WARD_SALVE_DRINK.get();
    }

    @Override
    public SoundEvent getEatingSound() {
        return VestigeSounds.WARD_SALVE_DRINK.get();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        Player player = entity instanceof Player p ? p : null;
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
        }
        if (!level.isClientSide) {
            entity.addEffect(new MobEffectInstance(RiftRegistry.WARDED.get(), DURATION_TICKS, 0, false, true, true));
        }
        boolean creative = player != null && player.getAbilities().instabuild;
        if (player != null) {
            player.awardStat(Stats.ITEM_USED.get(this));
            if (!creative) {
                stack.shrink(1);
            }
        }
        entity.gameEvent(GameEvent.DRINK);
        if (creative) {
            return stack;
        }
        if (stack.isEmpty()) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }
        if (player != null) {
            player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.relicweave.ward_salve.1").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.relicweave.ward_salve.2").withStyle(ChatFormatting.GRAY));
    }
}
