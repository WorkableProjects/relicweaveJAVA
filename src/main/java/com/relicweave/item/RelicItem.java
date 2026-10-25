package com.relicweave.item;

import com.relicweave.relic.RelicActivation;
import com.relicweave.relic.RelicType;
import com.relicweave.relic.WayfarerChannel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A 0.0.3 relic. Behaviour lives in {@code com.relicweave.relic}; see {@link RelicType}. */
public class RelicItem extends Item {
    private final RelicType type;

    public RelicItem(RelicType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public RelicType getRelicType() {
        return type;
    }

    /**
     * Wayfarer's Relic only: sneak + use binds the current location; plain use activates it (same as the key). Other
     * relics are used through the Activate Relic key or work passively.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (type != RelicType.WAYFARER) {
            return InteractionResultHolder.pass(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (player.isShiftKeyDown()) {
                WayfarerChannel.bind(serverPlayer, stack);
            } else {
                RelicActivation.tryActivate(serverPlayer, type);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        RelicTooltips.append(type, stack, tooltip);
    }
}
