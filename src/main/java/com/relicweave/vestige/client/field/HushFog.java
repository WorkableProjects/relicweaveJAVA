package com.relicweave.vestige.client.field;

import com.relicweave.RelicWeave;
import com.relicweave.config.RelicWeaveClientConfig;
import com.relicweave.vestige.client.VestigeClientState;
import com.relicweave.vestige.hazard.field.StarfallMarker;
import com.relicweave.vestige.network.VestigeNetwork.HazardSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Hushfield fog: thicker fog (far plane scaled by {@code 1 - 0.5 x distortionIntensity}, never below 24 blocks) while
 * the server says the local player is in a Hushfield. It leaves fog alone while a Starfall marker is nearby so
 * telegraphs stay readable. Purely cosmetic.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, value = Dist.CLIENT)
public final class HushFog {
    public static final float MIN_FAR = 24.0F;

    private HushFog() {}

    /** Far plane after Hushfield fog: scaled down by half the intensity, never below {@link #MIN_FAR} (or the input). */
    public static float farPlane(float far, double intensity) {
        if (far <= MIN_FAR) {
            return far;
        }
        return Math.max(MIN_FAR, (float) (far * (1.0 - 0.5 * intensity)));
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if ((VestigeClientState.hazardFlags & HazardSyncPacket.IN_HUSHFIELD) == 0 || event.getType() != FogType.NONE) {
            return;
        }
        double intensity = RelicWeaveClientConfig.DISTORTION_INTENSITY.get();
        if (intensity <= 0.0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || markerNearby(mc, player)) {
            return;
        }
        float far = farPlane(event.getFarPlaneDistance(), intensity);
        if (far < event.getFarPlaneDistance()) {
            event.setFarPlaneDistance(far);
            event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), far * 0.25F));
            event.setCanceled(true);
        }
    }

    private static boolean markerNearby(Minecraft mc, LocalPlayer player) {
        return mc.level != null && !mc.level.getEntitiesOfClass(StarfallMarker.class, player.getBoundingBox().inflate(48.0)).isEmpty();
    }
}
