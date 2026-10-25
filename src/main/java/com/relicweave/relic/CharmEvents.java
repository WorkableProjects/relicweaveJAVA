package com.relicweave.relic;

import com.relicweave.RelicWeave;
import com.relicweave.item.CharmItem;
import com.relicweave.item.CharmType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side charm passives. Charms are polled on a coarse interval instead of every tick: attribute modifiers last
 * until removed, so a 10 tick delay in picking up or dropping a charm is imperceptible and much cheaper.
 * <p>
 * Modifiers are transient (never saved with the player), so a charm that is lost while offline cannot leave a stale
 * bonus behind; they are simply re-added on the next check after login.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class CharmEvents {
    public static final int CHARM_CHECK_INTERVAL = 10;

    private static final UUID COPPER_ID = UUID.fromString("5b0c1c3e-7a41-4d52-9a0e-0c6a1f2b3001");
    private static final UUID IRON_ID = UUID.fromString("5b0c1c3e-7a41-4d52-9a0e-0c6a1f2b3002");
    private static final UUID GOLD_ID = UUID.fromString("5b0c1c3e-7a41-4d52-9a0e-0c6a1f2b3003");
    private static final UUID DIAMOND_ID = UUID.fromString("5b0c1c3e-7a41-4d52-9a0e-0c6a1f2b3004");
    private static final UUID NETHERITE_ID = UUID.fromString("5b0c1c3e-7a41-4d52-9a0e-0c6a1f2b3005");

    private CharmEvents() {}

    public static UUID modifierId(CharmType type) {
        return switch (type) {
            case COPPER -> COPPER_ID;
            case IRON -> IRON_ID;
            case GOLD -> GOLD_ID;
            case DIAMOND -> DIAMOND_ID;
            case NETHERITE -> NETHERITE_ID;
        };
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % CHARM_CHECK_INTERVAL == 0) {
            updateCharms(player);
        }
    }

    /** Applies or removes every charm modifier according to what the player currently carries. */
    public static void updateCharms(ServerPlayer player) {
        Set<CharmType> carried = carriedCharms(player);
        for (CharmType type : CharmType.values()) {
            boolean present = carried.contains(type);
            switch (type) {
                case COPPER -> sync(player, Attributes.MOVEMENT_SPEED, type, present, 0.05, AttributeModifier.Operation.MULTIPLY_BASE);
                case IRON -> sync(player, Attributes.ARMOR, type, present, 2.0, AttributeModifier.Operation.ADDITION);
                case GOLD -> sync(player, Attributes.LUCK, type, present, 2.0, AttributeModifier.Operation.ADDITION);
                case DIAMOND -> sync(player, Attributes.MAX_HEALTH, type, present, 2.0, AttributeModifier.Operation.ADDITION);
                case NETHERITE -> {
                    sync(player, Attributes.KNOCKBACK_RESISTANCE, type, present, 0.1, AttributeModifier.Operation.ADDITION);
                    if (present) {
                        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, true, false, true));
                    }
                }
            }
        }
        RelicAttributes.clampHealth(player);
    }

    private static Set<CharmType> carriedCharms(Player player) {
        Set<CharmType> found = EnumSet.noneOf(CharmType.class);
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof CharmItem charm) {
                found.add(charm.getCharmType());
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof CharmItem charm) {
                found.add(charm.getCharmType());
            }
        }
        return found;
    }

    private static void sync(Player player, Attribute attribute, CharmType type, boolean present, double amount,
                             AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        UUID id = modifierId(type);
        AttributeModifier existing = instance.getModifier(id);
        if (present && existing == null) {
            instance.addTransientModifier(new AttributeModifier(id, "relicweave:charm/" + type.name().toLowerCase(Locale.ROOT), amount, operation));
        } else if (!present && existing != null) {
            instance.removeModifier(id);
        }
    }
}
