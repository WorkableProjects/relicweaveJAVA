package com.relicweave.vestige.entity.humanoid;

import com.relicweave.RelicWeave;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** T06 humanoid Vestige mobs (HumanoidModel/SkeletonModel based): entity types, spawn eggs and their own sound events. */
public final class HumanoidMobs {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RelicWeave.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RelicWeave.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RelicWeave.MOD_ID);

    private HumanoidMobs() {}
}
