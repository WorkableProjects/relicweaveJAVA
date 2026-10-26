package com.relicweave.gametest;

import com.mojang.authlib.GameProfile;
import com.relicweave.RelicWeave;
import com.relicweave.quest.QuestEvents;
import com.relicweave.vestige.primer.TravelersPrimer;
import net.minecraft.advancements.Advancement;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/** The quest line (two advancement tabs) and the Traveler's Primer. */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class QuestGameTests {
    private static final String TEMPLATE = "platform";

    private static final List<String> RELICWEAVE_TAB = List.of("root", "transmutation_table", "first_book", "bookworm",
            "attuned", "attunement_master", "celestial_shard", "celestial_ingot", "celestial_gear", "celestial_set",
            "celestial_infuser", "first_infusion", "effect_infusion", "first_charm", "relic_hunter", "relic_activated",
            "relic_collector", "totem", "fine_dining", "beyond_the_dragon", "threshold_sigil", "gate_woken");
    private static final List<String> VESTIGE_TAB = List.of("root", "there_and_back", "witnessed_resonance",
            "outlasted_convergence", "warded", "rift_sickness", "aetherite", "aetherite_ingot", "riftglass_lens",
            "first_ruin", "ruin_scholar", "cartographer", "first_blood", "elite_slayer", "mimic", "bestiary",
            "vestige_relic", "decoded", "loom", "augmented", "vestige_relics_all", "vestige_book", "vestige_infusion",
            "altar_call", "thorned_regent", "choir_unheard", "gravity_maw", "starforged_colossus", "warden_of_seals",
            "concordant_alloy", "upgrade_template", "aetherforged", "aetherforged_arsenal", "seal_gate", "archivist_prime");

    private QuestGameTests() {}

    /** Every quest parses (a malformed criterion would make the advancement disappear) and has a display. */
    @GameTest(template = TEMPLATE)
    public static void everyQuestLoads(GameTestHelper helper) {
        ServerAdvancementManager advancements = helper.getLevel().getServer().getAdvancements();
        StringBuilder missing = new StringBuilder();
        for (String name : RELICWEAVE_TAB) {
            check(advancements, RelicWeave.id("relicweave/" + name), missing);
        }
        for (String name : VESTIGE_TAB) {
            check(advancements, RelicWeave.id("vestige/" + name), missing);
        }
        helper.assertTrue(missing.length() == 0, "Missing or display-less quests:" + missing);
        helper.succeed();
    }

    private static void check(ServerAdvancementManager advancements, ResourceLocation id, StringBuilder missing) {
        Advancement advancement = advancements.getAdvancement(id);
        if (advancement == null || advancement.getDisplay() == null) {
            missing.append(' ').append(id);
        }
    }

    /** The quest chain reads in order: gate → Vestige; bosses in sequence; the Seal Gate leads to the final boss. */
    @GameTest(template = TEMPLATE)
    public static void questChainOrder(GameTestHelper helper) {
        ServerAdvancementManager advancements = helper.getLevel().getServer().getAdvancements();
        helper.assertTrue(parentOf(advancements, "relicweave/gate_woken").equals("relicweave/threshold_sigil"), "gate after sigil");
        helper.assertTrue(parentOf(advancements, "relicweave/threshold_sigil").equals("relicweave/beyond_the_dragon"), "sigil after dragon");
        helper.assertTrue(parentOf(advancements, "vestige/choir_unheard").equals("vestige/thorned_regent"), "choir after regent");
        helper.assertTrue(parentOf(advancements, "vestige/warden_of_seals").equals("vestige/starforged_colossus"), "warden after colossus");
        helper.assertTrue(parentOf(advancements, "vestige/seal_gate").equals("vestige/warden_of_seals"), "seal gate after warden");
        helper.assertTrue(parentOf(advancements, "vestige/archivist_prime").equals("vestige/seal_gate"), "prime after seal gate");
        Advancement boss = advancements.getAdvancement(RelicWeave.id("vestige/thorned_regent"));
        helper.assertTrue(boss != null && boss.getCriteria().containsKey("defeated"), "boss quests keep the 'defeated' criterion BossRewards awards");
        helper.succeed();
    }

    private static String parentOf(ServerAdvancementManager advancements, String path) {
        Advancement advancement = advancements.getAdvancement(RelicWeave.id(path));
        if (advancement == null || advancement.getParent() == null) {
            return "<none>";
        }
        return advancement.getParent().getId().getPath();
    }

    /** Quest steps match only their own event name. */
    @GameTest(template = TEMPLATE)
    public static void questTriggerMatchesEventName(GameTestHelper helper) {
        var instance = new com.relicweave.quest.QuestTrigger.Instance(
                net.minecraft.advancements.critereon.ContextAwarePredicate.ANY, QuestEvents.GATE_ACTIVATED);
        helper.assertTrue(instance.event().equals("gate_activated"), "event name");
        helper.assertTrue(instance.getCriterion().equals(RelicWeave.id("quest")), "trigger id relicweave:quest");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void primerHasAllPagesAndIsGivenOnce(GameTestHelper helper) {
        ItemStack book = TravelersPrimer.create();
        CompoundTag tag = book.getTag();
        helper.assertTrue(book.is(Items.WRITTEN_BOOK) && tag != null, "primer is a written book");
        helper.assertTrue(tag.getString(WrittenBookItem.TAG_TITLE).equals(TravelersPrimer.TITLE), "title");
        helper.assertTrue(tag.getList(WrittenBookItem.TAG_PAGES, Tag.TAG_STRING).size() == TravelersPrimer.PAGE_COUNT, "page count");
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "relicweave-primer"));
        helper.assertTrue(TravelersPrimer.giveOnce(player), "first time gives the primer");
        helper.assertFalse(TravelersPrimer.giveOnce(player), "never twice");
        helper.assertTrue(player.getInventory().countItem(Items.WRITTEN_BOOK) == 1, "exactly one primer");
        helper.succeed();
    }

    /** The craftable copy is identical in title and pages to the one handed out. */
    @GameTest(template = TEMPLATE)
    public static void primerRecipeMakesTheSameBook(GameTestHelper helper) {
        Recipe<?> recipe = helper.getLevel().getRecipeManager().byKey(RelicWeave.id("vestige/travelers_primer")).orElse(null);
        helper.assertTrue(recipe != null, "primer recipe loads");
        ItemStack result = recipe.getResultItem(helper.getLevel().registryAccess());
        ItemStack given = TravelersPrimer.create();
        helper.assertTrue(result.is(Items.WRITTEN_BOOK), "recipe makes a written book");
        helper.assertTrue(result.getTag() != null
                && result.getTag().getString(WrittenBookItem.TAG_TITLE).equals(given.getTag().getString(WrittenBookItem.TAG_TITLE))
                && result.getTag().getList(WrittenBookItem.TAG_PAGES, Tag.TAG_STRING).size() == TravelersPrimer.PAGE_COUNT,
                "recipe book matches the given primer");
        helper.succeed();
    }
}
