package com.yourname.vox.features.addons;

import com.yourname.vox.IVoxAddon;
import com.yourname.vox.ConfigManager;
import net.minecraft.client.MinecraftClient;

public class AutoTotem implements IVoxAddon {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public static boolean enabled = false;

    @Override
    public String getName() {
        return "AutoTotem";
    }

    @Override
    public void onEnable() {
        enabled = true;
    }

    @Override
    public void onTick() {
        if (mc.player != null && enabled) {
            // Placeholder for totem logic
        }
    }

    @Override
    public void onChat(String msg) {}

    @Override
    public void onRenderWorldLast(float partialTicks) {}

    @Override
    public void toggle() {
        enabled = !enabled;
        ConfigManager.addonToggles.put(getName(), enabled);
    }

    @Override
    public String getDescription() {
        return "Automatically equips a totem of undying.";
    }
}