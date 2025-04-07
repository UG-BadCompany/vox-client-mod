package com.yourname.vox;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    public static final Map<String, Object> addonSettings = new HashMap<>();
    public static final Map<String, Boolean> addonToggles = new HashMap<>();
    public static boolean highLoad = false;

    static {
        // Example settings
        addonSettings.put("AutoRespond_trigger_hi", "true");
        addonSettings.put("AutoRespond_response_hi", "Hello!");
        addonSettings.put("ServerScan_test", "example_value");

        // Example toggles
        addonToggles.put("AntiAFK", false);
        addonToggles.put("AntiAnticheat", false);
        addonToggles.put("AutoRespond", false);
    }

    public static void registerAddon(String name) {
        addonToggles.putIfAbsent(name, false);
    }

    public static void loadConfig() {
        System.out.println("Loading Vox config...");
    }
}