package com.relicweave.vestige.cycle;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.eventbus.api.Event;

/** Events about the Concordance Cycle, posted on {@code MinecraftForge.EVENT_BUS} from the server thread. */
public abstract class VestigeCycleEvent extends Event {
    private final ServerLevel level;

    protected VestigeCycleEvent(ServerLevel level) {
        this.level = level;
    }

    public ServerLevel getLevel() {
        return level;
    }

    /** The cycle moved from one phase to the next (checked once per second, so within a second of the boundary). */
    public static class PhaseChanged extends VestigeCycleEvent {
        private final CyclePhase from;
        private final CyclePhase to;

        public PhaseChanged(ServerLevel level, CyclePhase from, CyclePhase to) {
            super(level);
            this.from = from;
            this.to = to;
        }

        public CyclePhase getFrom() { return from; }
        public CyclePhase getTo() { return to; }
    }
}
