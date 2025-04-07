package com.yourname.vox.features.addons;

import com.yourname.vox.IVoxAddon;
import com.yourname.vox.ConfigManager;
import net.minecraft.client.MinecraftClient;

public class KillAura implements IVoxAddon {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    @Override
    public String getName() {
        return "KillAura";
    }

    @Override
    public void onEnable() {}

    @Override
    public void onTick() {
        if (mc.player != null && ConfigManager.addonToggles.getOrDefault(getName(), false)) {
            // Add kill aura logic (e.g., attack nearby entities)
        }
    }

    @Override
    public void onChat(String msg) {}

    @Override
    public void onRenderWorldLast(float partialTicks) {}

    @Override
    public void toggle() {
        ConfigManager.addonToggles.put(getName(), !ConfigManager.addonToggles.getOrDefault(getName(), false));
    }

    @Override
    public String getDescription() {
        return "Automatically attacks nearby entities.";
    }
}