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
 * Hearts and the XP bar get a black-to-purple recolour with a slow travelling shimmer (re-uploaded every 2 ticks).
 * The crosshair is left alone so it stays visible.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, value = Dist.CLIENT)
public final class DreadHudTextures {
    private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");
    private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");
    private static final ResourceLocation[] SHEETS = {ICONS, WIDGETS};
    /** Crosshair cell in icons.png. */
    private static final int CROSSHAIR = 16;

    /** Animated regions of icons.png (in 256-wide units): {x0, y0, x1, y1}. Heart rows, hardcore heart rows, XP bar. */
    private static final int[][] PURPLE_REGIONS = {{16, 0, 220, 9}, {16, 45, 220, 54}, {0, 64, 182, 74}};
    /** Ticks between shimmer frames. */
    private static final int FRAME_TICKS = 2;

    private static boolean applied;
    private static DynamicTexture icons;
    private static NativeImage iconsImage;
    /** Original (vanilla or pack) ARGB pixels of {@link #PURPLE_REGIONS}, one array per region, row-major. */
    private static int[][] purpleSource;
    private static int[][] purpleBox;
    private static int frame;

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
        if (applied && icons != null && ++frame % FRAME_TICKS == 0) {
            shimmer(mc.level.getGameTime());
        }
    }

    /** Re-tints hearts and the XP bar with a slow purple glow travelling left to right. */
    private static void shimmer(long gameTime) {
        float time = gameTime;
        for (int r = 0; r < purpleBox.length; r++) {
            int[] box = purpleBox[r];
            int[] source = purpleSource[r];
            int w = box[2] - box[0];
            for (int y = box[1]; y < box[3]; y++) {
                for (int x = box[0]; x < box[2]; x++) {
                    int argb = source[(y - box[1]) * w + (x - box[0])];
                    float glow = 0.5F + 0.5F * net.minecraft.util.Mth.sin(time * 0.12F - x * 0.06F);
                    iconsImage.setPixelRGBA(x, y, swap(DreadPalette.voidPurple(argb, glow * glow)));
                }
            }
        }
        icons.upload();
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
            int scale = Math.max(1, image.getWidth() / 256);
            int cross = sheet == ICONS ? scale * CROSSHAIR : 0;
            if (sheet == ICONS) {
                capturePurpleRegions(image, scale);
            }
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if (x < cross && y < cross) {
                        continue;
                    }
                    image.setPixelRGBA(x, y, darkenAbgr(image.getPixelRGBA(x, y)));
                }
            }
            DynamicTexture texture = new DynamicTexture(image);
            textures.register(sheet, texture);
            if (sheet == ICONS) {
                icons = texture;
                iconsImage = image;
                shimmer(mc.level.getGameTime());
            }
        }
        applied = true;
    }

    private static void capturePurpleRegions(NativeImage image, int scale) {
        purpleBox = new int[PURPLE_REGIONS.length][];
        purpleSource = new int[PURPLE_REGIONS.length][];
        for (int r = 0; r < PURPLE_REGIONS.length; r++) {
            int[] box = PURPLE_REGIONS[r];
            int x0 = Math.min(image.getWidth(), box[0] * scale);
            int y0 = Math.min(image.getHeight(), box[1] * scale);
            int x1 = Math.min(image.getWidth(), box[2] * scale);
            int y1 = Math.min(image.getHeight(), box[3] * scale);
            purpleBox[r] = new int[]{x0, y0, x1, y1};
            int[] pixels = new int[Math.max(0, (x1 - x0) * (y1 - y0))];
            for (int y = y0; y < y1; y++) {
                for (int x = x0; x < x1; x++) {
                    pixels[(y - y0) * (x1 - x0) + (x - x0)] = swap(image.getPixelRGBA(x, y));
                }
            }
            purpleSource[r] = pixels;
        }
    }

    private static void restore(Minecraft mc) {
        for (ResourceLocation sheet : SHEETS) {
            mc.getTextureManager().register(sheet, new SimpleTexture(sheet));
        }
        icons = null;
        iconsImage = null;
        applied = false;
    }

    /** NativeImage pixels are ABGR; {@link DreadPalette} works on ARGB. Swapping red and blue converts either way. */
    static int swap(int pixel) {
        return (pixel & 0xFF00FF00) | ((pixel & 0xFF) << 16) | ((pixel >> 16) & 0xFF);
    }

    static int darkenAbgr(int abgr) {
        return swap(DreadPalette.darkenArgb(swap(abgr)));
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
