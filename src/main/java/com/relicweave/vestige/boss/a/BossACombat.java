package com.relicweave.vestige.boss.a;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Small geometry, victim and NBT helpers shared by the T11 bosses. */
final class BossACombat {
    private BossACombat() {}

    /** Living survival/adventure players inside {@code box} (the only things boss hazards hurt). */
    static List<Player> victims(Level level, AABB box) {
        return level.getEntitiesOfClass(Player.class, box, p -> p.isAlive() && !p.isSpectator() && !p.isCreative());
    }

    /** Distance from {@code p} to the segment a-b. */
    static double distToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double len2 = ab.lengthSqr();
        if (len2 < 1.0E-9) {
            return p.distanceTo(a);
        }
        double t = Math.max(0.0, Math.min(1.0, p.subtract(a).dot(ab) / len2));
        return p.distanceTo(a.add(ab.scale(t)));
    }

    /** Y of the top of the first solid block at or below {@code y + 2} (searching 8 blocks down), or NaN if none. */
    static double groundY(Level level, double x, double y, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Math.floor(x), Math.floor(y) + 2, Math.floor(z));
        for (int i = 0; i < 10; i++) {
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return pos.getY() + 1.0;
            }
            pos.move(0, -1, 0);
        }
        return Double.NaN;
    }

    static Vec3 flat(Vec3 v) {
        return new Vec3(v.x, 0.0, v.z);
    }

    static void putVec(CompoundTag tag, String key, @Nullable Vec3 v) {
        if (v != null) {
            ListTag list = new ListTag();
            list.add(DoubleTag.valueOf(v.x));
            list.add(DoubleTag.valueOf(v.y));
            list.add(DoubleTag.valueOf(v.z));
            tag.put(key, list);
        }
    }

    @Nullable
    static Vec3 getVec(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_LIST)) {
            return null;
        }
        ListTag list = tag.getList(key, Tag.TAG_DOUBLE);
        return list.size() == 3 ? new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2)) : null;
    }
}
