package com.yourname.vox.features;

import com.yourname.vox.AddonLoader;
import com.yourname.vox.ConfigManager;
import com.yourname.vox.features.addons.FireOverlayToggle;
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
                            if (addon instanceof FireOverlayToggle && parts.length > 2 && parts[2].equalsIgnoreCase("mode")) {
                                if (parts.length > 3) {
                                    ((FireOverlayToggle) addon).setMode(parts[3].toLowerCase());
                                }
                            } else {
                                addon.toggle();
                                boolean enabled = ConfigManager.addonToggles.getOrDefault(addon.getName(), false);
                                mc.player.sendMessage(Text.literal("Toggled " + addon.getName() + " " + (enabled ? "on" : "off")), false);
                            }
                        });
            }
        }
    }
}