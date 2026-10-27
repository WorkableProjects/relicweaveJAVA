package com.relicweave.gametest;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.client.dread.DreadPalette;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** 0.0.4.5.b dread look: the numbers behind the Vestige's darker fog, light and HUD. */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DreadGameTests {
    private DreadGameTests() {}

    @GameTest(template = "platform")
    public static void dreadFogClosesInAndDreadZeroKeepsTheOldHaze(GameTestHelper helper) {
        float renderFar = 192.0F;
        helper.assertTrue(DreadPalette.fogFar(renderFar, 0.0, false) == renderFar * 0.92F, "dread 0 keeps the 0.0.4 haze");
        helper.assertTrue(DreadPalette.fogFar(renderFar, 1.0, false) == DreadPalette.FAR, "full dread: fog wall at 72");
        helper.assertTrue(DreadPalette.fogFar(renderFar, 1.0, true) == DreadPalette.FAR_CONVERGENCE, "Convergence: closer still");
        helper.assertTrue(DreadPalette.fogFar(32.0F, 1.0, false) == 32.0F, "Never farther than the render distance");
        float far = DreadPalette.fogFar(renderFar, 1.0, false);
        helper.assertTrue(DreadPalette.fogNear(far, 1.0) < 6.0F, "Fog starts almost at your feet");
        helper.assertTrue(DreadPalette.fogScale(1.0) <= 0.25F && DreadPalette.fogScale(0.0) == 1.0F, "Fog colour darkens");
        helper.succeed();
    }

    @GameTest(template = "platform")
    public static void dreadDimsDaylightButNotTorchlight(GameTestHelper helper) {
        helper.assertTrue(DreadPalette.lightScale(0, 1.0) == 1.0F - DreadPalette.MAX_DIM, "Sky-only light is dimmed");
        helper.assertTrue(DreadPalette.lightScale(15, 1.0) == 1.0F, "Full block light is untouched");
        helper.assertTrue(DreadPalette.lightScale(0, 0.0) == 1.0F, "dread 0 changes nothing");
        helper.assertTrue(DreadPalette.lightScale(7, 1.0) > DreadPalette.lightScale(0, 1.0), "More block light, less dimming");
        helper.succeed();
    }

    @GameTest(template = "platform")
    public static void darkHudPixelsAreDarkerAndKeepAlpha(GameTestHelper helper) {
        int heart = 0xFFFF1313;
        int dark = DreadPalette.darkenArgb(heart);
        helper.assertTrue(dark >>> 24 == 0xFF, "Alpha kept");
        helper.assertTrue(((dark >> 16) & 0xFF) < 0xFF && ((dark >> 16) & 0xFF) > 0x90, "Hearts stay a deep red");
        helper.assertTrue(DreadPalette.darkenArgb(0x00000000) == 0, "Transparent stays transparent");
        int grey = DreadPalette.darkenArgb(0xFF808080);
        helper.assertTrue((grey & 0xFF) > ((grey >> 8) & 0xFF), "Grey frames shift toward violet");
        helper.succeed();
    }
}
