package com.relicweave.client;

import com.relicweave.enchantment.upbound.UpboundHandler;
import com.relicweave.network.PurlingNetwork;
import com.relicweave.registry.ModEnchantments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

/** Client-side Upbound: detects a Jump press in mid-air, applies the jump locally, and tells the server. */
public final class UpboundClient {
    /** Ticks since last on ground before an air jump is allowed, so the normal jump's own key press never counts. */
    private static final int MIN_TICKS_OFF_GROUND = 3;

    private static boolean jumpWasDown;
    private static int airJumpsUsed;
    private static int lastGroundTick;

    private UpboundClient() {}

    public static void onClientTick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null) {
            jumpWasDown = false;
            return;
        }
        boolean jumpDown = mc.options.keyJump.isDown();
        boolean risingEdge = jumpDown && !jumpWasDown;
        jumpWasDown = jumpDown;

        if (player.onGround()) {
            airJumpsUsed = 0;
            lastGroundTick = player.tickCount;
            return;
        }
        if (!risingEdge || mc.screen != null
                || player.getAbilities().flying || player.isFallFlying() || player.isPassenger()
                || player.isInWater() || player.isInLava() || player.onClimbable()
                || player.tickCount - lastGroundTick < MIN_TICKS_OFF_GROUND) {
            return;
        }
        int level = EnchantmentHelper.getItemEnchantmentLevel(
                ModEnchantments.UPBOUND.get(), player.getItemBySlot(EquipmentSlot.FEET));
        if (level <= 0 || airJumpsUsed >= level) {
            return;
        }

        Vec3 v = player.getDeltaMovement();
        player.setDeltaMovement(v.x, UpboundHandler.JUMP_VELOCITY, v.z);
        player.fallDistance = 0;
        airJumpsUsed++;
        PurlingNetwork.CHANNEL.sendToServer(new PurlingNetwork.UpboundJumpPacket());
    }
}
