package com.relicweave.gametest;

import com.mojang.authlib.GameProfile;
import com.relicweave.RelicWeave;
import com.relicweave.guide.GuideBook;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GuideGameTests {
    private GuideGameTests() {}

    @GameTest(template = "platform")
    public static void guideBookHasAllPages(GameTestHelper helper) {
        ItemStack book = GuideBook.create();
        CompoundTag tag = book.getTag();
        helper.assertTrue(book.is(Items.WRITTEN_BOOK) && tag != null, "Guide should be a written book");
        helper.assertTrue(GuideBook.TITLE.equals(tag.getString(WrittenBookItem.TAG_TITLE)), "Wrong title");
        helper.assertTrue(tag.getList(WrittenBookItem.TAG_PAGES, Tag.TAG_STRING).size() == GuideBook.PAGE_COUNT, "Wrong page count");
        helper.assertTrue(tag.getList(WrittenBookItem.TAG_PAGES, Tag.TAG_STRING).getString(0).contains("book.relicweave.guide.page1"),
                "Pages should be translatable components");
        helper.succeed();
    }

    @GameTest(template = "platform")
    public static void guideBookIsGivenOnlyOnce(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "relicweave-test"));
        helper.assertTrue(GuideBook.giveIfFirstJoin(player), "First join should give the guide");
        helper.assertFalse(GuideBook.giveIfFirstJoin(player), "Second join must not give another guide");
        helper.assertTrue(player.getInventory().countItem(Items.WRITTEN_BOOK) == 1, "Exactly one guide expected");
        helper.succeed();
    }

    /** A player who got the 0.0.2 guide (flag only, no edition) receives the 0.0.3 edition exactly once. */
    @GameTest(template = "platform")
    public static void upgradedPlayerGetsNewEditionOnce(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "relicweave-upgrade"));
        CompoundTag persisted = new CompoundTag();
        persisted.putBoolean(RelicWeave.MOD_ID + ":received_guide", true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        helper.assertTrue(GuideBook.giveIfFirstJoin(player), "0.0.2 players should get the updated guide");
        helper.assertFalse(GuideBook.giveIfFirstJoin(player), "The updated guide must only be given once");
        helper.assertTrue(player.getInventory().countItem(Items.WRITTEN_BOOK) == 1, "Exactly one new guide expected");
        helper.succeed();
    }
}
