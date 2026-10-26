package com.relicweave.vestige.boss;

import com.relicweave.vestige.VestigeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Summons and tracks one boss. Structure templates set its NBT: {@code Boss} (entity type id, required) and optionally
 * {@code SpawnOffset} ([x, y, z] relative to the altar, default [0, 1, 3]). Saved state: the living boss's UUID, whether
 * it was ever defeated, and the rematch cooldown. Awakening takes {@link #SUMMON_TICKS}; only one boss per altar can be
 * alive, and a boss whose entity no longer exists (killed by commands, lost) is treated as gone so the arena never locks.
 */
public class BossAltarBlockEntity extends BlockEntity {
    public static final int SUMMON_TICKS = 60;

    @Nullable
    private ResourceLocation bossType;
    private int[] spawnOffset = {0, 1, 3};
    @Nullable
    private UUID bossUuid;
    private boolean defeatedOnce;
    private long cooldownUntil;
    private int summonTicks = -1;

    public BossAltarBlockEntity(BlockPos pos, BlockState state) {
        super(BossRegistry.BOSS_ALTAR_BE.get(), pos, state);
    }

    @Nullable
    public ResourceLocation bossType() {
        return bossType;
    }

    public void setBossType(ResourceLocation type) {
        this.bossType = type;
        setChanged();
    }

    public boolean defeatedOnce() {
        return defeatedOnce;
    }

    public boolean isSummoning() {
        return summonTicks >= 0;
    }

    @Nullable
    public Entity livingBoss(ServerLevel level) {
        if (bossUuid == null) {
            return null;
        }
        Entity entity = level.getEntity(bossUuid);
        return entity != null && entity.isAlive() ? entity : null;
    }

    public void tryAwaken(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (bossType == null || !ForgeRegistries.ENTITY_TYPES.containsKey(bossType)) {
            player.displayClientMessage(Component.translatable("message.relicweave.altar.empty"), true);
            return;
        }
        if (isSummoning() || livingBoss(level) != null) {
            player.displayClientMessage(Component.translatable("message.relicweave.altar.awake"), true);
            return;
        }
        boolean rematches = !VestigeConfig.SPEC.isLoaded() || VestigeConfig.BOSS_REMATCHES.get();
        if (defeatedOnce && !rematches) {
            player.displayClientMessage(Component.translatable("message.relicweave.altar.slumbering"), true);
            return;
        }
        long now = level.getGameTime();
        if (now < cooldownUntil) {
            player.displayClientMessage(Component.translatable("message.relicweave.altar.cooldown", (cooldownUntil - now + 19) / 20), true);
            return;
        }
        summonTicks = 0;
        level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.5F, 0.6F);
        player.displayClientMessage(Component.translatable("message.relicweave.altar.awakening"), true);
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BossAltarBlockEntity altar) {
        if (altar.summonTicks < 0 || !(level instanceof ServerLevel server)) {
            return;
        }
        altar.summonTicks++;
        if (altar.summonTicks % 5 == 0) {
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 12, 0.6, 0.6, 0.6, 0.05);
        }
        if (altar.summonTicks >= SUMMON_TICKS) {
            altar.summonTicks = -1;
            altar.spawnBoss(server);
        }
    }

    /** Spawns the boss now (used by the summoning timer and tests). Returns the boss, or null if the type is invalid. */
    @Nullable
    public VestigeBoss spawnBoss(ServerLevel level) {
        EntityType<?> type = bossType == null ? null : ForgeRegistries.ENTITY_TYPES.getValue(bossType);
        if (type == null) {
            return null;
        }
        BlockPos at = worldPosition.offset(spawnOffset[0], spawnOffset[1], spawnOffset[2]);
        Entity entity = type.create(level);
        if (!(entity instanceof VestigeBoss boss)) {
            if (entity != null) {
                entity.discard();
            }
            return null;
        }
        boss.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
        boss.bindArena(at, worldPosition);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.TRIGGERED, null, null);
        level.addFreshEntity(boss);
        level.playSound(null, at, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.8F, 1.2F);
        bossUuid = boss.getUUID();
        setChanged();
        return boss;
    }

    /** Called by {@link BossRewards} when this altar's boss dies. */
    public void onBossDefeated(long gameTime) {
        defeatedOnce = true;
        bossUuid = null;
        int cooldown = VestigeConfig.SPEC.isLoaded() ? VestigeConfig.BOSS_REMATCH_COOLDOWN_SECONDS.get() : 300;
        cooldownUntil = gameTime + cooldown * 20L;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (bossType != null) {
            tag.putString("Boss", bossType.toString());
        }
        tag.putIntArray("SpawnOffset", spawnOffset);
        if (bossUuid != null) {
            tag.putUUID("BossUuid", bossUuid);
        }
        tag.putBoolean("DefeatedOnce", defeatedOnce);
        tag.putLong("CooldownUntil", cooldownUntil);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        bossType = tag.contains("Boss") ? ResourceLocation.tryParse(tag.getString("Boss")) : null;
        int[] offset = tag.getIntArray("SpawnOffset");
        if (offset.length == 3) {
            spawnOffset = offset;
        }
        bossUuid = tag.hasUUID("BossUuid") ? tag.getUUID("BossUuid") : null;
        defeatedOnce = tag.getBoolean("DefeatedOnce");
        cooldownUntil = tag.getLong("CooldownUntil");
    }
}
