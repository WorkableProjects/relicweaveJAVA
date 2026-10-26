package com.relicweave.vestige.boss;

import com.relicweave.quest.QuestEvents;
import com.relicweave.RelicWeave;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Crown of Concordance's final-region gate. It is never opened by an item (so no catalyst is consumed): a player who
 * has the {@code relicweave:vestige/warden_of_seals} advancement right-clicks it and is moved through to the first safe
 * spot up to {@link #MAX_DEPTH} blocks behind it (along the face they clicked). Server-validated; the block never opens
 * for anyone else.
 */
public class SealGateBlock extends HorizontalDirectionalBlock {
    public static final int MAX_DEPTH = 6;

    public SealGateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    public static boolean mayPass(ServerPlayer player) {
        return player.isCreative() || BossRewards.hasDefeated(player, RelicWeave.id("warden_of_seals"));
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (!mayPass(serverPlayer)) {
            serverPlayer.displayClientMessage(Component.translatable("message.relicweave.seal_gate.sealed"), true);
            return InteractionResult.CONSUME;
        }
        Direction through = hit.getDirection().getAxis().isHorizontal() ? hit.getDirection().getOpposite() : state.getValue(FACING).getOpposite();
        for (int depth = 1; depth <= MAX_DEPTH; depth++) {
            BlockPos feet = pos.relative(through, depth);
            if (level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                    && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                    && !level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty()) {
                serverPlayer.teleportTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
                QuestEvents.fire(serverPlayer, QuestEvents.SEAL_GATE_PASSED);
                level.playSound(null, feet, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.8F, 1.4F);
                return InteractionResult.CONSUME;
            }
        }
        serverPlayer.displayClientMessage(Component.translatable("message.relicweave.seal_gate.blocked"), true);
        return InteractionResult.CONSUME;
    }
}
