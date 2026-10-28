package com.relicweave.vestige.boss.c;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** One of Archivist-Prime's two Final Seal nodes: 30 HP and always exposed. */
public class IndexNode extends RuneNode {
    public static final double HEALTH = 30.0;

    public IndexNode(EntityType<? extends IndexNode> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean isExposed() {
        return true;
    }
}
