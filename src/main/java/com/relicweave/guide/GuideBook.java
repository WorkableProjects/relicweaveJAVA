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
    public static final int PAGE_COUNT = 13;
    public static final String TITLE = "RelicWeave Guide";
    public static final String AUTHOR = "Dutch and Claude";
    /** Stored in Forge's persisted player data, which survives death and respawn. */
    private static final String RECEIVED_TAG = RelicWeave.MOD_ID + ":received_guide";

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

    /** Gives the guide if this player has never received it. Returns true if a book was given. */
    public static boolean giveIfFirstJoin(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(RECEIVED_TAG)) {
            return false;
        }
        persisted.putBoolean(RECEIVED_TAG, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);

        ItemStack book = create();
        if (!player.getInventory().add(book)) {
            player.drop(book, false);
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
