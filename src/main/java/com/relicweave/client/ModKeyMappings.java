package com.relicweave.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class ModKeyMappings {
    public static final String CATEGORY = "key.categories.relicweave";

    public static final KeyMapping PURLING = new KeyMapping("key.relicweave.purling",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);

    private ModKeyMappings() {}
}
