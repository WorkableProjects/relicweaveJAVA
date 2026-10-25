package com.relicweave.enchantment.attunement.client;

import com.relicweave.RelicWeave;
import com.relicweave.enchantment.attunement.SteadyDrawEvents;
import com.relicweave.registry.ModEnchantments;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side Steady Draw. In Forge 1.20.1 {@code LocalPlayer.aiStep} calls {@code input.tick(...)}, then fires
 * {@link MovementInputUpdateEvent}, and only afterwards (while {@code isUsingItem()}) multiplies
 * {@code leftImpulse}/{@code forwardImpulse} by 0.2 (verified in the Forge LocalPlayer patch). The event therefore
 * sees the un-slowed input, and scaling it by {@code target / 0.2} makes vanilla's 0.2 factor yield the target
 * multiplier (I = 0.5, II = 0.8). The target never exceeds 1, so the result never exceeds the un-slowed input.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, value = Dist.CLIENT)
public final class SteadyDrawClient {
    private SteadyDrawClient() {}

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Player player = event.getEntity();
        if (!player.isUsingItem()) {
            return;
        }
        ItemStack stack = player.getUseItem();
        if (!(stack.getItem() instanceof BowItem)) {
            return;
        }
        int level = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.STEADY_DRAW.get(), stack);
        if (level > 0) {
            float factor = SteadyDrawEvents.steadyDrawMultiplier(level) / SteadyDrawEvents.VANILLA_USE_ITEM_MULTIPLIER;
            event.getInput().leftImpulse *= factor;
            event.getInput().forwardImpulse *= factor;
        }
    }
}
