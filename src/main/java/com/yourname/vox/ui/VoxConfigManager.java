package com.yourname.vox.ui;

import com.yourname.vox.ConfigManager;
import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class VoxConfigManager {
    private static final String CONFIG_DIR = MinecraftClient.getInstance().runDirectory.getPath() + "/vox_configs/";
    private static final String UI_CONFIG_FILE = "ui_config.txt";
    private static final String MODULE_CONFIG_FILE = "module_config.txt";

    public static void saveUIConfig(VoxTheme theme) {
        File dir = new File(CONFIG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(CONFIG_DIR + UI_CONFIG_FILE)))) {
            writer.write("backgroundColor=" + theme.backgroundColor + "\n");
            writer.write("panelBgStart=" + theme.panelBgStart + "\n");
            writer.write("panelBgEnd=" + theme.panelBgEnd + "\n");
            writer.write("tabActive=" + theme.tabActive + "\n");
            writer.write("tabInactive=" + theme.tabInactive + "\n");
            writer.write("buttonBg=" + theme.buttonBg + "\n");
            writer.write("buttonHover=" + theme.buttonHover + "\n");
            writer.write("activeTextColor=" + theme.activeTextColor + "\n");
            writer.write("textColor=" + theme.textColor + "\n");
            writer.write("scrollBarColor=" + theme.scrollBarColor + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadUIConfig(VoxTheme theme) {
        File file = new File(CONFIG_DIR + UI_CONFIG_FILE);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=");
                if (parts.length != 2) continue;
                String key = parts[0];
                int value = Integer.parseInt(parts[1]);
                switch (key) {
                    case "backgroundColor":
                        theme.backgroundColor = value;
                        break;
                    case "panelBgStart":
                        theme.panelBgStart = value;
                        break;
                    case "panelBgEnd":
                        theme.panelBgEnd = value;
                        break;
                    case "tabActive":
                        theme.tabActive = value;
                        break;
                    case "tabInactive":
                        theme.tabInactive = value;
                        break;
                    case "buttonBg":
                        theme.buttonBg = value;
                        break;
                    case "buttonHover":
                        theme.buttonHover = value;
                        break;
                    case "activeTextColor":
                        theme.activeTextColor = value;
                        break;
                    case "textColor":
                        theme.textColor = value;
                        break;
                    case "scrollBarColor":
                        theme.scrollBarColor = value;
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveModuleConfig() {
        File dir = new File(CONFIG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(CONFIG_DIR + MODULE_CONFIG_FILE)))) {
            for (Map.Entry<String, Boolean> entry : ConfigManager.addonToggles.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadModuleConfig() {
        File file = new File(CONFIG_DIR + MODULE_CONFIG_FILE);
        if (!file.exists()) return;

        Map<String, Boolean> toggles = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=");
                if (parts.length != 2) continue;
                toggles.put(parts[0], Boolean.parseBoolean(parts[1]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        ConfigManager.addonToggles.clear();
        ConfigManager.addonToggles.putAll(toggles);
    }
}