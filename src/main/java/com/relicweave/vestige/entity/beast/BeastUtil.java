package com.relicweave.vestige.entity.beast;

import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Shared helpers for the beast mobs: player lookup (replaceable by tests) and facing maths. */
public final class BeastUtil {
    /** Fake players registered by tests (they are not part of the level's player list). */
    private static final List<Player> TEST_PLAYERS = new CopyOnWriteArrayList<>();

    private BeastUtil() {}

    /** Test hook: make a fake player visible to the beast mobs' player lookups. */
    public static void addTestPlayer(Player player) {
        TEST_PLAYERS.add(player);
    }

    /** Test hook: undo {@link #addTestPlayer}. */
    public static void removeTestPlayer(Player player) {
        TEST_PLAYERS.remove(player);
    }

    /** Alive survival/adventure players within {@code radius} of {@code pos}, nearest first. */
    public static List<Player> playersNear(Level level, Vec3 pos, double radius) {
        List<Player> result = new ArrayList<>();
        List<Player> candidates = new ArrayList<>(level.players());
        for (Player fake : TEST_PLAYERS) {
            if (fake.level() == level) {
                candidates.add(fake);
            }
        }
        for (Player player : candidates) {
            if (player.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player)
                    && player.position().distanceToSqr(pos) <= radius * radius) {
                result.add(player);
            }
        }
        result.sort((a, b) -> Double.compare(a.position().distanceToSqr(pos), b.position().distanceToSqr(pos)));
        return result;
    }

    /** Nearest player within {@code radius}, or null. */
    public static Player nearestPlayer(Level level, Vec3 pos, double radius) {
        List<Player> list = playersNear(level, pos, radius);
        return list.isEmpty() ? null : list.get(0);
    }

    /** Horizontal unit vector the entity's body faces. */
    public static Vec3 facing(LivingEntity entity) {
        double rad = Math.toRadians(entity.yBodyRot);
        return new Vec3(-Math.sin(rad), 0.0, Math.cos(rad));
    }

    /** Horizontal unit vector from {@code from} to {@code to}, or zero if they coincide. */
    public static Vec3 flatDirection(Vec3 from, Vec3 to) {
        Vec3 d = new Vec3(to.x - from.x, 0.0, to.z - from.z);
        return d.lengthSqr() < 1.0E-6 ? Vec3.ZERO : d.normalize();
    }
}
