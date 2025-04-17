package com.yourname.vox;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

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
        addonSettings.put("AutoRespond_trigger_hi", "true");
        addonSettings.put("AutoRespond_response_hi", "Hello!");
        addonSettings.put("ServerScan_test", "example_value");
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
                    // Load all settings dynamically
                    addonConfig.entrySet().forEach(entry -> {
                        String key = entry.getKey();
                        if (!key.equals("enabled")) {
                            Object value;
                            if (entry.getValue().isJsonPrimitive()) {
                                if (entry.getValue().getAsJsonPrimitive().isBoolean()) {
                                    value = entry.getValue().getAsBoolean();
                                } else if (entry.getValue().getAsJsonPrimitive().isNumber()) {
                                    value = entry.getValue().getAsFloat();
                                } else {
                                    value = entry.getValue().getAsString();
                                }
                            } else {
                                value = entry.getValue().toString();
                            }
                            addonSettings.put(addon.getName() + "_" + key, value);
                        }
                    });
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
                // Save all settings for this addon
                addonSettings.entrySet().stream()
                        .filter(entry -> entry.getKey().startsWith(addon.getName() + "_"))
                        .forEach(entry -> {
                            String key = entry.getKey().substring(addon.getName().length() + 1);
                            Object value = entry.getValue();
                            if (value instanceof Boolean) {
                                addonConfig.addProperty(key, (Boolean) value);
                            } else if (value instanceof Number) {
                                addonConfig.addProperty(key, (Number) value);
                            } else {
                                addonConfig.addProperty(key, value.toString());
                            }
                        });
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