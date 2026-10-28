package com.relicweave.vestige.hazard.field;

import com.relicweave.config.RelicWeaveClientConfig.ParticleDensity;

/** Tiny helper turning the client particle-density setting into a 1-in-N spawn chance for ambient block particles. */
final class FieldParticles {
    private FieldParticles() {}

    static int chance(ParticleDensity density, int base) {
        return switch (density) {
            case FULL -> base;
            case REDUCED -> base * 3;
            case MINIMAL -> base * 8;
        };
    }
}
