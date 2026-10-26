package com.relicweave.vestige.boss.c;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** One of the Warden of Seals' three seal nodes: 20 HP, hurt only while the Warden has exposed it. */
public class SealNode extends RuneNode {
    public static final double HEALTH = 20.0;

    public SealNode(EntityType<? extends SealNode> type, Level level) {
        super(type, level);
    }
}
