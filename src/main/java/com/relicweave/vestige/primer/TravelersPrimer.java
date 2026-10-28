package com.relicweave.vestige.primer;

import com.relicweave.RelicWeave;
import net.minecraft.advancements.Advancement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * "The Vestige: A Traveler's Primer" — a short, spoiler-free written book on how to reach The Vestige and get home.
 * Given once when a player earns {@code minecraft:end/kill_dragon} (or on login if they already have it), and craftable
 * from a Book, an Eye of Ender and an Amethyst Shard. Pages are translatable ({@code book.relicweave.primer.page<N>}).
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class TravelersPrimer {
    public static final int PAGE_COUNT = 11;
    public static final String TITLE = "The Vestige: A Traveler's Primer";
    public static final String AUTHOR = "An Unknown Archivist";
    public static final ResourceLocation DRAGON_ADVANCEMENT = new ResourceLocation("minecraft", "end/kill_dragon");
    /** Persisted player flag, so the primer is only handed out once. */
    private static final String RECEIVED_TAG = RelicWeave.MOD_ID + ":received_primer";

    /** Recipes unlocked by defeating the Ender Dragon (also granted to players who did so before 0.0.4). */
    public static final ResourceLocation[] GATE_RECIPES = {
            RelicWeave.id("vestige/gate/threshold_sigil"), RelicWeave.id("vestige/gate/concordance_frame"),
            RelicWeave.id("vestige/gate/gate_keystone"), RelicWeave.id("vestige/travelers_primer")};

    private TravelersPrimer() {}

    /** Puts the gate recipes in the player's recipe book (no-op for recipes already known). */
    public static void grantGateRecipes(ServerPlayer player) {
        player.awardRecipesByKey(GATE_RECIPES);
    }

    public static ItemStack create() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        tag.putString(WrittenBookItem.TAG_TITLE, TITLE);
        tag.putString(WrittenBookItem.TAG_AUTHOR, AUTHOR);
        ListTag pages = new ListTag();
        for (int page = 1; page <= PAGE_COUNT; page++) {
            pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable("book.relicweave.primer.page" + page))));
        }
        tag.put(WrittenBookItem.TAG_PAGES, pages);
        return book;
    }

    /** Gives the primer if this player has never received it. Returns true if a book was given. */
    public static boolean giveOnce(ServerPlayer player) {
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
        player.sendSystemMessage(Component.translatable("message.relicweave.primer.received"));
        return true;
    }

    private static boolean hasKilledDragon(ServerPlayer player) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(DRAGON_ADVANCEMENT);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAdvancement().getId().equals(DRAGON_ADVANCEMENT)) {
            giveOnce(player);
            grantGateRecipes(player);
        }
    }

    /** Players who beat the dragon before 0.0.4 get the primer on their next login. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && hasKilledDragon(player)) {
            giveOnce(player);
            grantGateRecipes(player);
        }
    }
}
