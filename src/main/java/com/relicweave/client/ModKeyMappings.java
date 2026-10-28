package com.relicweave.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import com.relicweave.config.RelicWeaveClientConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

public final class ModKeyMappings {
    public static final String CATEGORY = "key.categories.relicweave";

    public static final KeyMapping PURLING = new KeyMapping("key.relicweave.purling",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);

    /** Activates the selected relic (see {@link #CYCLE_RELIC}). Saved in options.txt as key_key.relicweave.activate_relic. */
    public static final KeyMapping ACTIVATE_RELIC = new KeyMapping("key.relicweave.activate_relic",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    /**
     * Selects the next activatable relic the player carries. Saved as key_key.relicweave.cycle_relic. Defaults to Z
     * since 0.0.4: the 0.0.3 default G is Curios' "Open Curios Inventory" key, so cycling never fired with Curios.
     */
    public static final KeyMapping CYCLE_RELIC = new KeyMapping("key.relicweave.cycle_relic",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY);

    /** The 0.0.3 Cycle Relic default, used only to migrate clashing bindings (see {@link #migrateCycleKey}). */
    public static final InputConstants.Key LEGACY_CYCLE_KEY = InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_G);

    private static final Logger LOGGER = LogUtils.getLogger();

    private ModKeyMappings() {}

    /**
     * Runs once per installation (tracked in the client config). If Cycle Relic is still on the old default G and
     * another key (Curios' Open Curios Inventory, or anything else) is also on G in a clashing context, moves Cycle
     * Relic to its new default and saves options.txt. A later deliberate rebind is never touched again.
     */
    public static void migrateCycleKey(Minecraft minecraft) {
        if (!RelicWeaveClientConfig.SPEC.isLoaded() || RelicWeaveClientConfig.CYCLE_KEY_CHECKED.get()) {
            return;
        }
        RelicWeaveClientConfig.CYCLE_KEY_CHECKED.set(true);
        RelicWeaveClientConfig.SPEC.save();
        if (!CYCLE_RELIC.getKey().equals(LEGACY_CYCLE_KEY)) {
            return;
        }
        for (KeyMapping other : minecraft.options.keyMappings) {
            if (other != CYCLE_RELIC && other.getKey().equals(CYCLE_RELIC.getKey())
                    && other.getKeyModifier() == CYCLE_RELIC.getKeyModifier()
                    && other.getKeyConflictContext().conflicts(CYCLE_RELIC.getKeyConflictContext())) {
                CYCLE_RELIC.setKey(CYCLE_RELIC.getDefaultKey());
                KeyMapping.resetMapping();
                minecraft.options.save();
                LOGGER.info("RelicWeave: Cycle Relic was on G together with '{}'; moved it to {}. Rebind it under Controls.",
                        other.getName(), CYCLE_RELIC.getTranslatedKeyMessage().getString());
                return;
            }
        }
    }
}
