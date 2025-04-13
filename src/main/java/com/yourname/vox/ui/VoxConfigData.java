package com.yourname.vox.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VoxConfigData {
    private static final String CONFIG_FILE = "config/vox_presets.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class Preset {
        public Map<String, int[]> logo = new HashMap<>();
        public Map<String, int[]> search = new HashMap<>();
        public Map<String, int[]> categoryBodyColors = new HashMap<>();
        public Map<String, int[]> categoryTitleColors = new HashMap<>();
        public Map<String, int[]> categoryBorderColors = new HashMap<>();
        public Map<String, Boolean> categoryBorderStates = new HashMap<>();
        public Map<String, int[]> categoryPositions = new HashMap<>();
        public Map<String, int[]> categorySizes = new HashMap<>();
    }

    public static void savePreset(String name, VoxScreen screen) {
        Preset preset = new Preset();
        preset.logo.put("color", screen.getLogoColor());
        preset.logo.put("position", new int[]{screen.getLogoX(), screen.getLogoY()});
        preset.logo.put("size", new int[]{screen.getLogoWidth(), screen.getLogoHeight()});
        preset.search.put("color", screen.getSearchColor());
        preset.search.put("position", new int[]{screen.getSearchField().getX(), screen.getSearchField().getY()});
        preset.search.put("size", new int[]{screen.getSearchField().getWidth(), screen.getSearchField().getHeight()});
        preset.categoryBodyColors.putAll(screen.getCategoryBodyColors());
        preset.categoryTitleColors.putAll(screen.getCategoryTitleColors());
        preset.categoryBorderColors.putAll(screen.getCategoryBorderColors());
        preset.categoryBorderStates.putAll(screen.getCategoryBorderStates());
        for (CategoryWindow window : screen.getCategoryWindows()) {
            String id = window.getCategory();
            preset.categoryPositions.put(id, new int[]{window.getX(), window.getY()});
            preset.categorySizes.put(id, new int[]{window.getWidth(), window.getHeight()});
        }

        Map<String, Preset> presets = loadPresets();
        presets.put(name, preset);
        File file = new File(CONFIG_FILE);
        file.getParentFile().mkdirs();
        try (Writer writer = new FileWriter(file)) {
            GSON.toJson(presets, writer);
        } catch (IOException e) {
            System.err.println("Failed to save preset: " + e.getMessage());
        }
    }

    public static void savePreset(int number, VoxScreen screen) {
        savePreset("preset_" + number, screen);
    }

    public static void loadPreset(String name, VoxScreen screen) {
        Map<String, Preset> presets = loadPresets();
        Preset preset = presets.get(name);
        if (preset == null) return;

        if (preset.logo.containsKey("color")) {
            int[] color = preset.logo.get("color");
            screen.updateLogoColor(color[0], color[1], color[2], color[3]);
        }
        if (preset.logo.containsKey("position")) {
            int[] pos = preset.logo.get("position");
            screen.updateLogoPosition(pos[0], pos[1]);
        }
        if (preset.logo.containsKey("size")) {
            int[] size = preset.logo.get("size");
            screen.updateLogoSize(size[0], size[1]);
        }
        if (preset.search.containsKey("color")) {
            int[] color = preset.search.get("color");
            screen.updateSearchColor(color[0], color[1], color[2], color[3]);
        }
        if (preset.search.containsKey("position")) {
            int[] pos = preset.search.get("position");
            screen.updateSearchPosition(pos[0], pos[1]);
        }
        if (preset.search.containsKey("size")) {
            int[] size = preset.search.get("size");
            screen.updateSearchSize(size[0], size[1]);
        }
        for (Map.Entry<String, int[]> entry : preset.categoryBodyColors.entrySet()) {
            String id = entry.getKey();
            int[] body = entry.getValue();
            int[] title = preset.categoryTitleColors.getOrDefault(id, new int[]{255, 255, 255});
            screen.updateCategoryColor(id, body[0], body[1], body[2], title[0], title[1], title[2]);
        }
        for (Map.Entry<String, int[]> entry : preset.categoryBorderColors.entrySet()) {
            String id = entry.getKey();
            int[] border = entry.getValue();
            boolean enabled = preset.categoryBorderStates.getOrDefault(id, false);
            screen.updateCategoryBorder(id, border[0], border[1], border[2], enabled);
        }
        for (Map.Entry<String, int[]> entry : preset.categoryPositions.entrySet()) {
            String id = entry.getKey();
            int[] pos = entry.getValue();
            screen.updateCategoryPosition(id, pos[0], pos[1]);
        }
        for (Map.Entry<String, int[]> entry : preset.categorySizes.entrySet()) {
            String id = entry.getKey();
            int[] size = entry.getValue();
            screen.updateCategorySize(id, size[0], size[1]);
        }
    }

    public static void loadPreset(int number, VoxScreen screen) {
        loadPreset("preset_" + number, screen);
    }

    public static Map<String, Preset> loadPresets() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            return new HashMap<>();
        }
        try (Reader reader = new FileReader(file)) {
            return GSON.fromJson(reader, new TypeToken<Map<String, Preset>>(){}.getType());
        } catch (IOException e) {
            System.err.println("Failed to load presets: " + e.getMessage());
            return new HashMap<>();
        }
    }

    public static List<String> getPresetNames() {
        return new ArrayList<>(loadPresets().keySet());
    }
}