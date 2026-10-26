package com.relicweave.gametest;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.VestigeConfig;
import com.relicweave.vestige.cycle.ConcordanceCycle;
import com.relicweave.vestige.cycle.CyclePhase;
import com.relicweave.vestige.cycle.CycleTicker;
import com.relicweave.vestige.cycle.VestigeCycleEvent;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Concordance Cycle arithmetic (default durations) and the phase-change event. */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CycleGameTests {
    private static final String TEMPLATE = "platform";
    private static final long QUIET = 540 * 20L;
    private static final long RESONANCE = 360 * 20L;
    private static final long CONVERGENCE = 240 * 20L;
    private static final long CYCLE = QUIET + RESONANCE + CONVERGENCE;

    private CycleGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void cyclePhaseBoundaries(GameTestHelper helper) {
        helper.assertTrue(ConcordanceCycle.cycleTicks() == CYCLE, "Default cycle length is 1140 s");
        helper.assertTrue(ConcordanceCycle.phaseAt(0) == CyclePhase.QUIET, "0 is Quiet");
        helper.assertTrue(ConcordanceCycle.phaseAt(QUIET - 1) == CyclePhase.QUIET, "Last Quiet tick");
        helper.assertTrue(ConcordanceCycle.phaseAt(QUIET) == CyclePhase.RESONANCE, "First Resonance tick");
        helper.assertTrue(ConcordanceCycle.phaseAt(QUIET + RESONANCE - 1) == CyclePhase.RESONANCE, "Last Resonance tick");
        helper.assertTrue(ConcordanceCycle.phaseAt(QUIET + RESONANCE) == CyclePhase.CONVERGENCE, "First Convergence tick");
        helper.assertTrue(ConcordanceCycle.phaseAt(CYCLE - 1) == CyclePhase.CONVERGENCE, "Last Convergence tick");
        helper.assertTrue(ConcordanceCycle.phaseAt(CYCLE) == CyclePhase.QUIET, "Wraps to Quiet");
        helper.assertTrue(ConcordanceCycle.phaseAt(CYCLE * 5 + QUIET) == CyclePhase.RESONANCE, "Wraps repeatedly");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cycleTicksUntilNextPhase(GameTestHelper helper) {
        helper.assertTrue(ConcordanceCycle.ticksUntilNextPhase(0) == QUIET, "Full Quiet ahead at 0");
        helper.assertTrue(ConcordanceCycle.ticksUntilNextPhase(QUIET - 1) == 1, "One tick left of Quiet");
        helper.assertTrue(ConcordanceCycle.ticksUntilNextPhase(QUIET) == RESONANCE, "Full Resonance ahead");
        helper.assertTrue(ConcordanceCycle.ticksUntilNextPhase(QUIET + RESONANCE) == CONVERGENCE, "Full Convergence ahead");
        helper.assertTrue(ConcordanceCycle.ticksUntilNextPhase(CYCLE - 1) == 1, "One tick left of Convergence");
        helper.assertTrue(ConcordanceCycle.ticksUntilNextPhase(CYCLE) == QUIET, "Wrapped");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void disabledCycleStaysQuiet(GameTestHelper helper) {
        if (!VestigeConfig.SPEC.isLoaded()) {
            // Without a loaded config the cycle is always enabled; nothing to toggle.
            helper.assertTrue(ConcordanceCycle.enabled(), "Cycle is enabled by default");
            helper.succeed();
            return;
        }
        boolean before = VestigeConfig.CYCLE_ENABLED.get();
        try {
            VestigeConfig.CYCLE_ENABLED.set(false);
            helper.assertFalse(ConcordanceCycle.enabled(), "Cycle should report disabled");
            for (long t : new long[] {0, QUIET, QUIET + RESONANCE, CYCLE - 1, CYCLE * 3 + QUIET + 5}) {
                helper.assertTrue(ConcordanceCycle.phaseAt(t) == CyclePhase.QUIET, "Quiet forever at " + t);
            }
            helper.assertTrue(ConcordanceCycle.ticksUntilNextPhase(0) == Long.MAX_VALUE, "No next phase when disabled");
        } finally {
            VestigeConfig.CYCLE_ENABLED.set(before);
        }
        helper.assertTrue(ConcordanceCycle.phaseAt(QUIET) == CyclePhase.RESONANCE, "Restored");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void phaseChangeEventFiresOnlyOnChange(GameTestHelper helper) {
        List<String> seen = new ArrayList<>();
        Consumer<VestigeCycleEvent.PhaseChanged> listener = e -> seen.add(e.getFrom() + ">" + e.getTo());
        MinecraftForge.EVENT_BUS.addListener(listener);
        try {
            CycleTicker.reset();
            CycleTicker.checkPhase(helper.getLevel(), 0);
            helper.assertTrue(seen.isEmpty(), "First check only records the phase");
            CycleTicker.checkPhase(helper.getLevel(), 100);
            CycleTicker.checkPhase(helper.getLevel(), QUIET - 1);
            helper.assertTrue(seen.isEmpty(), "No event inside a phase");
            CycleTicker.checkPhase(helper.getLevel(), QUIET);
            helper.assertTrue(seen.equals(List.of("QUIET>RESONANCE")), "Quiet to Resonance, got " + seen);
            CycleTicker.checkPhase(helper.getLevel(), QUIET + 20);
            helper.assertTrue(seen.size() == 1, "No repeat event");
            CycleTicker.checkPhase(helper.getLevel(), QUIET + RESONANCE);
            CycleTicker.checkPhase(helper.getLevel(), CYCLE);
            helper.assertTrue(seen.equals(List.of("QUIET>RESONANCE", "RESONANCE>CONVERGENCE", "CONVERGENCE>QUIET")),
                    "Full loop, got " + seen);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(listener);
            CycleTicker.reset();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void warningWindowIsLastSecondsOfResonance(GameTestHelper helper) {
        long end = QUIET + RESONANCE;
        helper.assertFalse(CycleTicker.inWarningWindow(0), "Not in Quiet");
        helper.assertFalse(CycleTicker.inWarningWindow(end - 31 * 20), "31 s before is outside");
        helper.assertTrue(CycleTicker.inWarningWindow(end - 30 * 20), "30 s before is inside");
        helper.assertTrue(CycleTicker.inWarningWindow(end - 1), "Last tick is inside");
        helper.assertFalse(CycleTicker.inWarningWindow(end), "Convergence itself is not the warning");
        helper.succeed();
    }
}
