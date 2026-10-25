package com.relicweave.client;

import com.relicweave.enchantment.attunement.ResonanceEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

/** Client-side handling of server -> client packets. Only reached through {@code DistExecutor} on the client. */
public final class ClientPacketHandlers {
    private ClientPacketHandlers() {}

    public static void resonanceStreak(ResourceLocation blockId, int count) {
        LocalPlayer player = Minecraft.getInstance().player;
        Block block = ForgeRegistries.BLOCKS.getValue(blockId);
        if (player != null && block != null) {
            ResonanceEvents.applyStreak(player.getUUID(), block, count, player.level().getGameTime());
        }
    }
}
