package com.relicweave.vestige.client.relic;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.relicweave.RelicWeave;
import com.relicweave.vestige.client.VestigeClientState;
import com.relicweave.vestige.network.VestigeNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Draws line boxes around the blocks of the last {@code RevealPacket} (Surveyor's Lens, Mnemonic Spindle, Glyphfinder)
 * through walls until {@link VestigeClientState#revealUntilGameTime}. Teal for the Surveyor, gold for the Glyphfinder.
 * Purely cosmetic and client-only.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, value = Dist.CLIENT)
public final class RevealRenderer {
    private RevealRenderer() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.level.getGameTime() >= VestigeClientState.revealUntilGameTime
                || VestigeClientState.revealed.isEmpty()) {
            return;
        }
        List<BlockPos> positions = VestigeClientState.revealed;
        boolean gold = VestigeClientState.revealKind == VestigeNetwork.RevealPacket.KIND_GLYPHFINDER;
        float r = gold ? 0.97F : 0.35F;
        float g = gold ? 0.80F : 0.85F;
        float b = gold ? 0.30F : 0.80F;

        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(2.0F);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        int count = Math.min(VestigeNetwork.RevealPacket.MAX_POSITIONS, positions.size());
        for (int i = 0; i < count; i++) {
            BlockPos pos = positions.get(i);
            LevelRenderer.renderLineBox(pose, buffer, pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z,
                    pos.getX() + 1 - cam.x, pos.getY() + 1 - cam.y, pos.getZ() + 1 - cam.z, r, g, b, 1.0F);
        }
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.lineWidth(1.0F);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
