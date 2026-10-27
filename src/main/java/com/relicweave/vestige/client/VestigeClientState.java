package com.relicweave.vestige.client;

import com.relicweave.vestige.cycle.CyclePhase;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Client-side copy of server-authoritative Vestige state, written only by {@code VestigeNetwork} packet handlers and
 * read by HUD/render code. Purely cosmetic: nothing here grants items or effects.
 */
public final class VestigeClientState {
    // Concordance Cycle (CycleSyncPacket)
    public static CyclePhase phase = CyclePhase.QUIET;
    /** Ticks until the next phase when the packet arrived, and the client game time at that moment. */
    public static long ticksRemaining;
    public static long syncedAtGameTime;
    /** True while the pre-Convergence warning is active. */
    public static boolean convergenceWarning;
    public static boolean cycleEnabled = true;

    // Hazards (HazardSyncPacket)
    /** Rift Saturation 0-100. */
    public static int riftExposure;
    /** Bit flags, see {@code HazardSyncPacket}. */
    public static int hazardFlags;
    /** Current Rift protection, 0-100 %. */
    public static int riftProtectionPercent;

    // Reveals (RevealPacket)
    public static List<BlockPos> revealed = List.of();
    public static int revealKind;
    public static long revealUntilGameTime;

    // Boss music (BossMusicPacket): the sound event to play while in a boss arena, or null.
    @org.jetbrains.annotations.Nullable
    public static net.minecraft.resources.ResourceLocation bossMusic;

    private VestigeClientState() {}

    public static void reset() {
        phase = CyclePhase.QUIET;
        ticksRemaining = 0;
        syncedAtGameTime = 0;
        convergenceWarning = false;
        cycleEnabled = true;
        riftExposure = 0;
        hazardFlags = 0;
        riftProtectionPercent = 0;
        revealed = List.of();
        revealKind = 0;
        revealUntilGameTime = 0;
        bossMusic = null;
    }
}
