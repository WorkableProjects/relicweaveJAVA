package com.relicweave.registry;

import com.relicweave.RelicWeave;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Sound events; the files and subtitles are defined in {@code assets/relicweave/sounds.json}. */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RelicWeave.MOD_ID);

    /** Transmutation Table finished a book: a short magical shimmer. */
    public static final RegistryObject<SoundEvent> TRANSMUTATION_COMPLETE = register("block.transmutation_table.complete");
    /** Celestial Infuser finished: a deep resonance... */
    public static final RegistryObject<SoundEvent> INFUSER_RESONANCE = register("block.celestial_infuser.resonance");
    /** ...followed a few ticks later by a short celestial chime. */
    public static final RegistryObject<SoundEvent> INFUSER_CHIME = register("block.celestial_infuser.chime");
    /** A relic was activated (Echo Charm pulse, Wayfarer's Relic return, Guardian Relic shield). */
    public static final RegistryObject<SoundEvent> RELIC_ACTIVATE = register("item.relic.activate");

    private ModSounds() {}

    private static RegistryObject<SoundEvent> register(String path) {
        return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(RelicWeave.id(path)));
    }
}
