package com.relicweave.vestige.gate;

import com.relicweave.quest.QuestEvents;
import com.relicweave.vestige.VestigeConfig;
import com.relicweave.vestige.registry.VestigeSounds;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Runs the activation ritual of a Concordance Gate. The ritual is deliberately not saved: after a restart or an unload
 * it simply never happened (no item was taken, the keystone stays inactive).
 */
public class GateKeystoneBlockEntity extends BlockEntity {
    /** Ritual length in ticks. */
    public static final int RITUAL_TICKS = 60;
    /** The player must stay within this many blocks of the keystone. */
    public static final double MAX_DISTANCE = 4.0;
    private static final ResourceLocation KILL_DRAGON = new ResourceLocation("minecraft", "end/kill_dragon");

    @Nullable
    private ServerPlayer ritualPlayer;
    private int ritualTicks;

    public GateKeystoneBlockEntity(BlockPos pos, BlockState state) {
        super(GateRegistry.GATE_KEYSTONE_BE.get(), pos, state);
    }

    public boolean isRitualRunning() {
        return ritualPlayer != null;
    }

    /** True when the player has freed the End (or the requirement is disabled). */
    public static boolean isUnlocked(ServerPlayer player) {
        if (!VestigeConfig.REQUIRE_DRAGON_KILL.get()) {
            return true;
        }
        Advancement advancement = player.server.getAdvancements().getAdvancement(KILL_DRAGON);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    /** Validates and starts the ritual. Server side only. Returns whether it started. */
    public boolean tryStart(ServerPlayer player) {
        if (level == null || level.isClientSide) {
            return false;
        }
        if (getBlockState().getValue(GateKeystoneBlock.ACTIVE)) {
            player.displayClientMessage(Component.translatable("message.relicweave.gate.already_active"), true);
            return false;
        }
        if (!GateShape.isComplete(level, worldPosition)) {
            player.displayClientMessage(Component.translatable("message.relicweave.gate.incomplete"), true);
            return false;
        }
        if (!isUnlocked(player)) {
            player.displayClientMessage(Component.translatable("message.relicweave.gate.locked"), true);
            return false;
        }
        if (ritualPlayer != null) {
            player.displayClientMessage(Component.translatable("message.relicweave.gate.busy"), true);
            return false;
        }
        ritualPlayer = player;
        ritualTicks = 0;
        return true;
    }

    private void cancel(String reason) {
        ServerPlayer player = ritualPlayer;
        ritualPlayer = null;
        ritualTicks = 0;
        if (player != null && reason != null) {
            player.displayClientMessage(Component.translatable(reason), true);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GateKeystoneBlockEntity keystone) {
        if (keystone.ritualPlayer != null && level instanceof ServerLevel serverLevel) {
            keystone.tickRitual(serverLevel, pos);
        }
    }

    private void tickRitual(ServerLevel level, BlockPos pos) {
        ServerPlayer player = ritualPlayer;
        if (player.isRemoved() || !player.isAlive() || player.level() != level) {
            cancel(null);
            return;
        }
        if (!GateShape.isComplete(level, pos) || getBlockState().getValue(GateKeystoneBlock.ACTIVE)
                || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > MAX_DISTANCE * MAX_DISTANCE) {
            cancel("message.relicweave.gate.interrupted");
            return;
        }
        ritualTicks++;
        if (ritualTicks % 10 == 0 && ritualTicks < RITUAL_TICKS) {
            level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 6, 0.2, 0.4, 0.2, 0.02);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dz != 0) {
                        level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5 + dx, pos.getY() + 1.0, pos.getZ() + 0.5 + dz,
                                4, 0.2, 0.3, 0.2, 0.05);
                    }
                }
            }
            level.playSound(null, pos, VestigeSounds.GATE_CHANNEL.get(), SoundSource.BLOCKS, 0.8F, 0.8F + ritualTicks / 100.0F);
        }
        if (ritualTicks >= RITUAL_TICKS) {
            complete(level, pos, player);
        }
    }

    private void complete(ServerLevel level, BlockPos pos, ServerPlayer player) {
        if (!player.getAbilities().instabuild && !consumeSigil(player)) {
            cancel("message.relicweave.gate.interrupted");
            return;
        }
        ritualPlayer = null;
        ritualTicks = 0;
        level.setBlock(pos, getBlockState().setValue(GateKeystoneBlock.ACTIVE, true), 3);
        level.playSound(null, pos, VestigeSounds.GATE_ACTIVATE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message.relicweave.gate.activated"), true);
        QuestEvents.fire(player, QuestEvents.GATE_ACTIVATED);
    }

    private static boolean consumeSigil(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(GateRegistry.THRESHOLD_SIGIL.get())) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }
}
