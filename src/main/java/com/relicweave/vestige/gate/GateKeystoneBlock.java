package com.relicweave.vestige.gate;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Centre of a Concordance Gate. Right-click with a Threshold Sigil to activate; right-click when active to travel. */
public class GateKeystoneBlock extends BaseEntityBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public GateKeystoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GateKeystoneBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, GateRegistry.GATE_KEYSTONE_BE.get(), GateKeystoneBlockEntity::serverTick);
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
        ItemStack held = player.getItemInHand(hand);
        boolean sigil = held.is(GateRegistry.THRESHOLD_SIGIL.get());
        boolean active = state.getValue(ACTIVE);
        if (sigil) {
            if (active) {
                serverPlayer.displayClientMessage(Component.translatable("message.relicweave.gate.already_active"), true);
            } else if (level.getBlockEntity(pos) instanceof GateKeystoneBlockEntity keystone) {
                keystone.tryStart(serverPlayer);
            }
        } else if (!active) {
            serverPlayer.displayClientMessage(Component.translatable("message.relicweave.gate.inactive"), true);
        } else if (!GateShape.isComplete(level, pos)) {
            serverPlayer.displayClientMessage(Component.translatable("message.relicweave.gate.incomplete"), true);
        } else {
            GateTravel.travel(serverPlayer);
        }
        return InteractionResult.CONSUME;
    }
}
