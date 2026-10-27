package com.relicweave.client;

import com.relicweave.RelicWeave;
import com.relicweave.item.ScubaArmorItem;
import com.relicweave.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Scuba Mask: clear water. No underwater fog wall and no water overlay on the screen. */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, value = Dist.CLIENT)
public final class ScubaClientEvents {
    private ScubaClientEvents() {}

    private static boolean masked(Entity entity) {
        return entity instanceof LivingEntity living && ScubaArmorItem.wearing(living, ModItems.SCUBA_MASK.get());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (event.getType() != FogType.WATER || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN
                || !masked(event.getCamera().getEntity())) {
            return;
        }
        float far = Minecraft.getInstance().gameRenderer.getRenderDistance();
        event.setNearPlaneDistance(far * 0.5F);
        event.setFarPlaneDistance(far);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onScreenEffect(RenderBlockScreenEffectEvent event) {
        if (event.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.WATER && masked(event.getPlayer())) {
            event.setCanceled(true);
        }
    }
}
