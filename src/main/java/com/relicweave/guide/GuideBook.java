package com.relicweave.guide;

import com.relicweave.RelicWeave;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Gives every player a written "RelicWeave Guide" once, the first time they join a world. Pages are translatable
 * components ({@code book.relicweave.guide.page<N>}), so the text comes from the lang file on the reader's client.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class GuideBook {
    public static final int PAGE_COUNT = 24;
    public static final String TITLE = "RelicWeave Guide";
    public static final String AUTHOR = "Dutch and Claude";
    /** Stored in Forge's persisted player data, which survives death and respawn. */
    private static final String RECEIVED_TAG = RelicWeave.MOD_ID + ":received_guide";
    /** Guide edition last given to the player. Bumped when the guide gains pages, so upgrading players get it again. */
    private static final String EDITION_TAG = RelicWeave.MOD_ID + ":guide_edition";
    /** 1 = 0.0.1/0.0.2 guide (13 pages; players who have it carry no edition tag), 2 = 0.0.3 Attunement guide. */
    public static final int EDITION = 2;

    private GuideBook() {}

    public static ItemStack create() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        tag.putString(WrittenBookItem.TAG_TITLE, TITLE);
        tag.putString(WrittenBookItem.TAG_AUTHOR, AUTHOR);
        ListTag pages = new ListTag();
        for (int page = 1; page <= PAGE_COUNT; page++) {
            pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable("book.relicweave.guide.page" + page))));
        }
        tag.put(WrittenBookItem.TAG_PAGES, pages);
        return book;
    }

    /**
     * Gives the guide if this player has never received it, or has only received an older edition (then the new
     * edition is given once, with a chat note). Returns true if a book was given.
     */
    public static boolean giveIfFirstJoin(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        boolean hadOlder = persisted.getBoolean(RECEIVED_TAG);
        int edition = persisted.contains(EDITION_TAG) ? persisted.getInt(EDITION_TAG) : (hadOlder ? 1 : 0);
        if (edition >= EDITION) {
            return false;
        }
        persisted.putBoolean(RECEIVED_TAG, true);
        persisted.putInt(EDITION_TAG, EDITION);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);

        ItemStack book = create();
        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
        if (hadOlder) {
            player.sendSystemMessage(Component.translatable("message.relicweave.guide_updated"));
        }
        return true;
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            giveIfFirstJoin(player);
        }
    }
}
