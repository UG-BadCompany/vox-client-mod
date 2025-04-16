package com.yourname.vox;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.yourname.vox.features.addons.FireOverlayToggle;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    public static final Map<String, Object> addonSettings = new HashMap<>();
    public static final Map<String, Boolean> addonToggles = new HashMap<>();
    public static boolean highLoad = false;
    private static final File CONFIG_FILE = new File("config/vox/config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static {
        // Example settings
        addonSettings.put("AutoRespond_trigger_hi", "true");
        addonSettings.put("AutoRespond_response_hi", "Hello!");
        addonSettings.put("ServerScan_test", "example_value");

        // Initialize toggles
        AddonLoader.getAddons().forEach(addon -> addonToggles.putIfAbsent(addon.getName(), false));
    }

    public static void registerAddon(String name) {
        addonToggles.putIfAbsent(name, false);
    }

    public static void loadConfig() {
        try {
            if (!CONFIG_FILE.exists()) {
                saveConfig();
                return;
            }
            JsonObject config = GSON.fromJson(new FileReader(CONFIG_FILE), JsonObject.class);
            for (IVoxAddon addon : AddonLoader.getAddons()) {
                String name = addon.getName().toLowerCase();
                JsonObject addonConfig = config.getAsJsonObject(name);
                if (addonConfig != null) {
                    boolean enabled = addonConfig.get("enabled").getAsBoolean();
                    addonToggles.put(addon.getName(), enabled);
                    if (addon instanceof FireOverlayToggle && addonConfig.has("mode")) {
                        String mode = addonConfig.get("mode").getAsString();
                        ((FireOverlayToggle) addon).setMode(mode);
                        addonSettings.put(addon.getName() + "_mode", mode);
                    }
                    System.out.println("[Vox] Loaded " + addon.getName() + ": enabled=" + enabled);
                }
            }
        } catch (Exception e) {
            System.err.println("[Vox] Failed to load config: " + e.getMessage());
        }
    }

    public static void saveConfig() {
        try {
            JsonObject config = new JsonObject();
            for (IVoxAddon addon : AddonLoader.getAddons()) {
                String name = addon.getName().toLowerCase();
                JsonObject addonConfig = new JsonObject();
                boolean enabled = addonToggles.getOrDefault(addon.getName(), false);
                addonConfig.addProperty("enabled", enabled);
                if (addon instanceof FireOverlayToggle) {
                    String mode = addonSettings.getOrDefault(addon.getName() + "_mode", FireOverlayToggle.mode).toString();
                    addonConfig.addProperty("mode", mode);
                }
                config.add(name, addonConfig);
            }
            CONFIG_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(config, writer);
            }
            System.out.println("[Vox] Saved config");
        } catch (Exception e) {
            System.err.println("[Vox] Failed to save config: " + e.getMessage());
        }
    }
}