package com.relicweave.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class ModKeyMappings {
    public static final String CATEGORY = "key.categories.relicweave";

    public static final KeyMapping PURLING = new KeyMapping("key.relicweave.purling",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);

    /** Activates the selected relic (see {@link #CYCLE_RELIC}). Saved in options.txt as key_key.relicweave.activate_relic. */
    public static final KeyMapping ACTIVATE_RELIC = new KeyMapping("key.relicweave.activate_relic",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    /** Selects the next activatable relic the player carries. Saved as key_key.relicweave.cycle_relic. */
    public static final KeyMapping CYCLE_RELIC = new KeyMapping("key.relicweave.cycle_relic",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);

    private ModKeyMappings() {}
}
