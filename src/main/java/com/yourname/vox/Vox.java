package com.yourname.vox;

import com.yourname.vox.ui.VoxScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class Vox implements ClientModInitializer {
    private static final KeyBinding OPEN_VOX_SCREEN = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                    "key.vox.open_screen",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_H,
                    "category.vox"
            )
    );

    @Override
    public void onInitializeClient() {
        ConfigManager.loadConfig();
        AddonLoader.loadAddons();

        // Example high load check
        ConfigManager.highLoad = Runtime.getRuntime().freeMemory() < 50_000_000;

        // Register key binding handler
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && OPEN_VOX_SCREEN.wasPressed()) {
                client.setScreen(new VoxScreen());
            }
        });
    }
}