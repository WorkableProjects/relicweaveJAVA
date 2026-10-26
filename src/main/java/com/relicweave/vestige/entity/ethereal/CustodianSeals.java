package com.relicweave.vestige.entity.ethereal;

import com.relicweave.RelicWeave;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Protects the containers recorded by a living {@link SealedCustodian}: they cannot be opened or broken until it dies.
 * Creative-mode players bypass the seal (builders). Known limitation: hoppers and other automation can still move
 * items out of sealed containers, and explosions are not intercepted.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class CustodianSeals {
    private CustodianSeals() {}

    /** True if a living custodian near {@code pos} has recorded the container at {@code pos}. */
    public static boolean isSealed(Level level, BlockPos pos) {
        return !level.getEntitiesOfClass(SealedCustodian.class, new AABB(pos).inflate(SealedCustodian.SEAL_RADIUS + 8.0),
                c -> c.isAlive() && c.isSealing(pos)).isEmpty();
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        if (level.isClientSide || player.isCreative() || !isSealed(level, event.getPos())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        if (player instanceof ServerPlayer serverPlayer && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
            serverPlayer.displayClientMessage(Component.translatable("message.relicweave.custodian.sealed"), true);
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide || event.getPlayer().isCreative()) {
            return;
        }
        if (isSealed(level, event.getPos())) {
            event.setCanceled(true);
            if (event.getPlayer() instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(Component.translatable("message.relicweave.custodian.sealed"), true);
            }
        }
    }
}
