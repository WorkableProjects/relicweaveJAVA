package com.relicweave.item;

import com.relicweave.client.RelicClient;
import com.relicweave.compat.curios.CuriosCompat;
import com.relicweave.config.RelicWeaveConfig;
import com.relicweave.relic.RelicType;
import com.relicweave.relic.WayfarerChannel;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.List;

/**
 * Tooltip lines for relics: effect, valid locations, activation mode, cooldown, and duplicate rule. Config values are
 * read when loaded (the synced server value on a client in a world) and fall back to the defaults on the title screen.
 */
public final class RelicTooltips {
    private RelicTooltips() {}

    public static void append(RelicType type, ItemStack stack, List<Component> tooltip) {
        tooltip.add(Component.translatable("item." + com.relicweave.RelicWeave.MOD_ID + "." + type.itemPath() + ".desc")
                .withStyle(ChatFormatting.BLUE));
        tooltip.add(locationLine());
        if (type.activatable()) {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                tooltip.add(RelicClient.activateHint().withStyle(ChatFormatting.GRAY));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.relicweave.relic.passive").withStyle(ChatFormatting.GRAY));
        }
        if (type == RelicType.WAYFARER) {
            if (WayfarerChannel.hasBinding(stack)) {
                CompoundTag tag = WayfarerChannel.binding(stack);
                tooltip.add(Component.translatable("tooltip.relicweave.wayfarer.bound",
                        tag.getInt("x"), tag.getInt("y"), tag.getInt("z"), tag.getString("dimension"))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            } else {
                tooltip.add(Component.translatable("tooltip.relicweave.wayfarer.unbound").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        if (type == RelicType.ECHO) {
            tooltip.add(Component.translatable("tooltip.relicweave.echo.glowing").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (type.hasCooldown()) {
            tooltip.add(Component.translatable("tooltip.relicweave.relic.cooldown", cooldownSeconds(type)).withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.translatable("tooltip.relicweave.relic.no_stack").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** "Works anywhere in your inventory..." or "Works from your offhand..." depending on {@code charmActivation}. */
    public static Component locationLine() {
        boolean equippedOnly = RelicWeaveConfig.SPEC.isLoaded()
                && RelicWeaveConfig.CHARM_ACTIVATION.get() == RelicWeaveConfig.CharmActivation.EQUIPPED_ONLY;
        return Component.translatable(equippedOnly ? "tooltip.relicweave.relic.location.equipped"
                : "tooltip.relicweave.relic.location.inventory").withStyle(ChatFormatting.GRAY);
    }

    /** True when RelicWeave totems may also be used from a Curios slot. */
    public static boolean totemsInCharmSlot() {
        return RelicWeaveConfig.SPEC.isLoaded() && RelicWeaveConfig.CURIOS_TOTEMS.get() && CuriosCompat.isLoaded();
    }

    private static int cooldownSeconds(RelicType type) {
        if (RelicWeaveConfig.SPEC.isLoaded()) {
            return type.cooldownTicks() / 20;
        }
        return switch (type) {
            case FEATHERWOVEN -> 10;
            case WAYFARER -> 300;
            case ECHO -> 45;
            case GUARDIAN -> 120;
            default -> 0;
        };
    }
}
