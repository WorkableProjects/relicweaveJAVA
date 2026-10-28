package com.relicweave.vestige.boss;

import com.relicweave.RelicWeave;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

import java.util.HashSet;
import java.util.Set;

/**
 * One-time boss unlocks. Each boss has an advancement {@code relicweave:vestige/<boss path>} with a single
 * {@code minecraft:impossible} criterion named {@code defeated}; every player in the arena (and the killer) is awarded
 * it. Advancements are saved per player and awarding twice does nothing, so relogs, restarts, rematches and two players
 * landing the final blow together can never duplicate an unlock. Item rewards come from the loot table on each kill.
 */
public final class BossRewards {
    public static final String CRITERION = "defeated";

    private BossRewards() {}

    public static ResourceLocation advancementId(ResourceLocation bossId) {
        return RelicWeave.id("vestige/" + bossId.getPath());
    }

    public static void onDefeated(VestigeBoss boss, DamageSource source) {
        Set<ServerPlayer> players = new HashSet<>(boss.arenaPlayers());
        if (source.getEntity() instanceof ServerPlayer killer) {
            players.add(killer);
        }
        for (ServerPlayer player : players) {
            award(player, boss.bossId());
        }
        BlockPos altar = boss.altar();
        if (altar != null && boss.level() instanceof ServerLevel level
                && level.getBlockEntity(altar) instanceof BossAltarBlockEntity be) {
            be.onBossDefeated(level.getGameTime());
        }
    }

    /** Awards the boss advancement; returns true if it was newly completed. */
    public static boolean award(ServerPlayer player, ResourceLocation bossId) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(advancementId(bossId));
        if (advancement == null) {
            return false;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        if (progress.isDone()) {
            return false;
        }
        return player.getAdvancements().award(advancement, CRITERION);
    }

    public static boolean hasDefeated(ServerPlayer player, ResourceLocation bossId) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(advancementId(bossId));
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }
}
