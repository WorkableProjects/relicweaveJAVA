package com.relicweave.vestige.puzzle;

import com.relicweave.RelicWeave;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * Exploration puzzle blocks placed by Vestige structures and used by 0.0.4 relics and enchantments:
 * <ul>
 *   <li>{@code false_wall}: looks like Vestige Bricks, breaks instantly, drops nothing — a secret passage.
 *   Tagged {@code #relicweave:hidden_glyphs} so the Surveyor's Lens and Glyphfinder outline it.</li>
 *   <li>{@code hidden_glyph}: a faint inscription in brickwork that marks a nearby secret or clue. Same tag.</li>
 *   <li>{@code glyph_seal}: a low-tier seal (very slow to break; Sealbreaker speeds it up, the Sealkeeper's Signet opens
 *   it). Tagged {@code #relicweave:breakable_seals}. Boss altars and Seal Gates are never in that tag.</li>
 *   <li>{@code ancient_terminal}: decoded by the Mnemonic Spindle; on its own it only shows a hint.</li>
 * </ul>
 */
public final class PuzzleBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RelicWeave.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RelicWeave.MOD_ID);

    public static final RegistryObject<Block> FALSE_WALL = block("false_wall", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.3F).noLootTable().sound(SoundType.STONE)));
    public static final RegistryObject<Block> HIDDEN_GLYPH = block("hidden_glyph", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(2.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final RegistryObject<Block> GLYPH_SEAL = block("glyph_seal", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(40.0F, 1200.0F).requiresCorrectToolForDrops()
                    .noLootTable().lightLevel(state -> 5).sound(SoundType.AMETHYST)));
    public static final RegistryObject<Block> ANCIENT_TERMINAL = block("ancient_terminal", () -> new AncientTerminalBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(-1.0F, 3600000.0F).noLootTable()
                    .lightLevel(state -> 7).sound(SoundType.LODESTONE)));

    private PuzzleBlocks() {}

    private static <T extends Block> RegistryObject<T> block(String name, Supplier<T> block) {
        RegistryObject<T> registered = BLOCKS.register(name, block);
        ITEMS.register(name, () -> new BlockItem(registered.get(), new Item.Properties()));
        return registered;
    }

    /** Shows a hint; the Mnemonic Spindle relic (0.0.4) does the decoding. */
    public static class AncientTerminalBlock extends Block {
        public AncientTerminalBlock(Properties properties) {
            super(properties);
        }

        @Override
        @SuppressWarnings("deprecation")
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.relicweave.terminal.unreadable"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }
}
