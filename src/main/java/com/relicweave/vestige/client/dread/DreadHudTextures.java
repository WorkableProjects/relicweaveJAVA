package com.relicweave.vestige.client.dread;

import com.mojang.blaze3d.platform.NativeImage;
import com.relicweave.RelicWeave;
import com.relicweave.config.RelicWeaveClientConfig;
import com.relicweave.vestige.VestigeKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.InputStream;
import java.util.Optional;

/**
 * Darkens the vanilla HUD while the player is in The Vestige: the hearts, hunger, armor, air and XP bar sheet
 * ({@code textures/gui/icons.png}) and the hotbar sheet ({@code textures/gui/widgets.png}). The darkened copies are
 * built at runtime from whatever resource pack is active (so packs keep working) and registered under the vanilla
 * ids; leaving The Vestige, turning {@code darkHud} off or reloading resources puts the originals back.
 * The crosshair is left alone so it stays visible.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, value = Dist.CLIENT)
public final class DreadHudTextures {
    private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");
    private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");
    private static final ResourceLocation[] SHEETS = {ICONS, WIDGETS};
    /** Crosshair cell in icons.png. */
    private static final int CROSSHAIR = 16;

    private static boolean applied;

    private DreadHudTextures() {}

    public static boolean isApplied() {
        return applied;
    }

    private static boolean wanted(Minecraft mc) {
        return mc.level != null && mc.player != null && mc.level.dimension() == VestigeKeys.THE_VESTIGE
                && RelicWeaveClientConfig.DARK_HUD.get();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        boolean wanted = wanted(mc);
        if (wanted && !applied) {
            apply(mc);
        } else if (!wanted && applied) {
            restore(mc);
        }
    }

    private static void apply(Minecraft mc) {
        TextureManager textures = mc.getTextureManager();
        for (ResourceLocation sheet : SHEETS) {
            NativeImage image = read(mc, sheet);
            if (image == null) {
                continue;
            }
            if (image.format() != NativeImage.Format.RGBA) {
                image.close();
                continue;
            }
            int cross = sheet == ICONS ? Math.max(1, image.getWidth() / 256) * CROSSHAIR : 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if (x < cross && y < cross) {
                        continue;
                    }
                    image.setPixelRGBA(x, y, darkenAbgr(image.getPixelRGBA(x, y)));
                }
            }
            textures.register(sheet, new DynamicTexture(image));
        }
        applied = true;
    }

    private static void restore(Minecraft mc) {
        for (ResourceLocation sheet : SHEETS) {
            mc.getTextureManager().register(sheet, new SimpleTexture(sheet));
        }
        applied = false;
    }

    /** NativeImage pixels are ABGR; {@link DreadPalette#darkenArgb} works on ARGB. */
    static int darkenAbgr(int abgr) {
        int argb = (abgr & 0xFF00FF00) | ((abgr & 0xFF) << 16) | ((abgr >> 16) & 0xFF);
        int dark = DreadPalette.darkenArgb(argb);
        return (dark & 0xFF00FF00) | ((dark & 0xFF) << 16) | ((dark >> 16) & 0xFF);
    }

    private static NativeImage read(Minecraft mc, ResourceLocation id) {
        Optional<Resource> resource = mc.getResourceManager().getResource(id);
        if (resource.isEmpty()) {
            return null;
        }
        try (InputStream in = resource.get().open()) {
            return NativeImage.read(in);
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().warn("Could not darken {}: {}", id, e.toString());
            return null;
        }
    }

    @Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        private ModBus() {}

        /** After a resource reload the vanilla sheets are fresh again; rebuild the dark copies on the next tick. */
        @SubscribeEvent
        public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) manager -> {
                if (applied) {
                    restore(Minecraft.getInstance());
                }
            });
        }
    }
}
