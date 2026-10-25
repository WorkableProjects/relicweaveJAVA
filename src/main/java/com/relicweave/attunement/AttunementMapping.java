package com.relicweave.attunement;

import com.relicweave.RelicWeave;
import com.relicweave.infusion.InfusionType;
import com.relicweave.registry.ModEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.RegistryObject;

/**
 * The canonical 0.0.3 Attunement source-item matrix. Every row has <b>both</b> a Transmutation Table enchantment and a
 * Celestial Infuser infusion; the two are separate recipes and never applied together.
 * <p>
 * Source items are not hard-coded: each row reads the item tag {@code relicweave:attunement_sources/<row>}, and both
 * of the row's recipes use that tag as their ingredient. A datapack can swap the source item by overriding the tag,
 * without touching recipes or code. The guidebook, JEI info pages and {@link AttunementValidator} all read this enum,
 * so they cannot disagree.
 */
public enum AttunementMapping {
    /** Default source: Prismarine Shard. */
    UNDERTOW("undertow", ModEnchantments.UNDERTOW, InfusionType.TIDAL),
    /** Default source: Echo Shard. */
    RESONANCE("resonance", ModEnchantments.RESONANCE, InfusionType.WARDING),
    /** Default source: Phantom Membrane. */
    STEADY_DRAW("steady_draw", ModEnchantments.STEADY_DRAW, InfusionType.PURSUIT),
    /** Default source: Copper Ingot. */
    REPRISAL("reprisal", ModEnchantments.REPRISAL, InfusionType.ANCHORED),
    /** Default source: Ghast Tear. */
    SOULGUARD("soulguard", ModEnchantments.SOULGUARD, InfusionType.WARDING),
    /** Default source: Oak Sapling. */
    REAPING("reaping", ModEnchantments.REAPING, InfusionType.ANCHORED);

    private final String key;
    private final RegistryObject<Enchantment> enchantment;
    private final InfusionType infusion;
    private final TagKey<Item> sourceTag;

    AttunementMapping(String key, RegistryObject<Enchantment> enchantment, InfusionType infusion) {
        this.key = key;
        this.enchantment = enchantment;
        this.infusion = infusion;
        this.sourceTag = TagKey.create(Registries.ITEM, RelicWeave.id("attunement_sources/" + key));
    }

    /** Row name, also the tag path suffix and the recipe file names ({@code transmutation/<key>_1}, {@code infusion/<infusion>_from_<key>_source}). */
    public String key() { return key; }
    public Enchantment enchantment() { return enchantment.get(); }
    public ResourceLocation enchantmentId() { return enchantment.getId(); }
    public InfusionType infusion() { return infusion; }
    /** {@code relicweave:attunement_sources/<key>} */
    public TagKey<Item> sourceTag() { return sourceTag; }
}
