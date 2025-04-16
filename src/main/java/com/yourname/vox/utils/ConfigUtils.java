package com.yourname.vox.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.yourname.vox.AddonLoader;
import com.yourname.vox.IVoxAddon;
import com.yourname.vox.features.addons.AutoTotem;
import com.yourname.vox.features.addons.FireOverlayToggle;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class ConfigUtils {
    private static final File CONFIG_FILE = new File("config/vox/config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

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
                    if (addon instanceof FireOverlayToggle) {
                        FireOverlayToggle.enabled = addonConfig.get("enabled").getAsBoolean();
                        FireOverlayToggle.mode = addonConfig.get("mode").getAsString();
                        addon.onTick();
                        System.out.println("[Vox] Loaded FireOverlayToggle: enabled=" + FireOverlayToggle.enabled + ", mode=" + FireOverlayToggle.mode);
                    } else if (addon instanceof AutoTotem) {
                        AutoTotem.enabled = addonConfig.get("enabled").getAsBoolean();
                        System.out.println("[Vox] Loaded AutoTotem: enabled=" + AutoTotem.enabled);
                    }
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
                if (addon instanceof FireOverlayToggle) {
                    addonConfig.addProperty("enabled", FireOverlayToggle.enabled);
                    addonConfig.addProperty("mode", FireOverlayToggle.mode);
                } else if (addon instanceof AutoTotem) {
                    addonConfig.addProperty("enabled", AutoTotem.enabled);
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