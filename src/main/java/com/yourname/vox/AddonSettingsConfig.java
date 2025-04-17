package com.yourname.vox;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddonSettingsConfig {
    private static final Map<String, List<String>> settingsOptions = new HashMap<>();

    static {
        // HighwayNav settings
        settingsOptions.put("HighwayNav", Arrays.asList(
                "Axis: x+, x-, z+, z-, x+z+, x-z+, x+z-, x-z-",
                "Sprint: true, false",
                "AutoJump: true, false"
        ));

        // FireOverlayToggle settings
        settingsOptions.put("FireOverlayToggle", Arrays.asList(
                "Mode: auto, always, off"
        ));

        // Add other addons with adjustable settings here
        // Example:
        // settingsOptions.put("AutoRespond", Arrays.asList(
        //     "Trigger: hi, hello",
        //     "Response: Hello!, Hi there!"
        // ));
    }

    public static Map<String, List<String>> getSettingsOptions() {
        return settingsOptions;
    }
}