package com.relicweave.vestige.boss.c;

import com.relicweave.RelicWeave;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Boss registry for Warden of Seals and Archivist-Prime (T13): boss entity types, their projectiles/helper entities, spawn eggs and sounds. */
public final class BossesC {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RelicWeave.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RelicWeave.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RelicWeave.MOD_ID);

    private BossesC() {}
}
