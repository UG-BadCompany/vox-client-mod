package com.yourname.vox.features;

import com.yourname.vox.AddonLoader;
import com.yourname.vox.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class CommandHandler {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public void handleCommand(String command) {
        if (mc.player != null && command.startsWith("/vox")) {
            String[] parts = command.split(" ");
            if (parts.length > 1) {
                String addonName = parts[1];
                AddonLoader.getAddons().stream()
                        .filter(addon -> addon.getName().equalsIgnoreCase(addonName))
                        .findFirst()
                        .ifPresent(addon -> {
                            addon.toggle();
                            mc.player.sendMessage(Text.literal("Toggled " + addonName), false);
                        });
            }
        }
    }
}