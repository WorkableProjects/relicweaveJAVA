package com.relicweave.block.furnace;

import com.relicweave.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** One Super Furnace tier. Behaves like a Furnace; see {@link SuperFurnaceBlockEntity}. */
public class SuperFurnaceBlock extends AbstractFurnaceBlock {
    private final SuperFurnaceTier tier;

    public SuperFurnaceBlock(SuperFurnaceTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public SuperFurnaceTier tier() {
        return tier;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SuperFurnaceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, ModBlockEntities.SUPER_FURNACE.get(), SuperFurnaceBlockEntity::serverTick);
    }

    @Override
    protected void openContainer(Level level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof SuperFurnaceBlockEntity furnace) {
            player.openMenu((MenuProvider) furnace);
            player.awardStat(Stats.INTERACT_WITH_FURNACE);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        for (Component line : tier.perkLines()) {
            tooltip.add(line.copy().withStyle(ChatFormatting.GRAY));
        }
    }

    /** Furnace flames and crackle, with a few extra sparks on higher tiers. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;
        if (random.nextDouble() < 0.1) {
            level.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
        }
        Direction facing = state.getValue(FACING);
        Direction.Axis axis = facing.getAxis();
        for (int i = 0; i < Math.max(1, tier.level() / 2); i++) {
            double along = random.nextDouble() * 0.6 - 0.3;
            double dx = axis == Direction.Axis.X ? facing.getStepX() * 0.52 : along;
            double dy = random.nextDouble() * 6.0 / 16.0;
            double dz = axis == Direction.Axis.Z ? facing.getStepZ() * 0.52 : along;
            level.addParticle(ParticleTypes.SMOKE, x + dx, y + dy, z + dz, 0.0, 0.0, 0.0);
            level.addParticle(tier == SuperFurnaceTier.CELESTIAL ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME,
                    x + dx, y + dy, z + dz, 0.0, 0.0, 0.0);
        }
    }
}
