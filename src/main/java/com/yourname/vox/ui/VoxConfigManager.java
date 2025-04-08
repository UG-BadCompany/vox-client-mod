package com.yourname.vox.ui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class VoxConfigManager {
    private final VoxTheme theme;

    public VoxConfigManager(VoxTheme theme) {
        this.theme = theme;
    }

    public void saveConfig(String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("backgroundColor=" + Integer.toHexString(theme.getBackgroundColor()) + "\n");
            writer.write("panelBgStart=" + Integer.toHexString(theme.getPanelBgStart()) + "\n");
            writer.write("panelBgEnd=" + Integer.toHexString(theme.getPanelBgEnd()) + "\n");
            writer.write("tabActive=" + Integer.toHexString(theme.getTabActive()) + "\n");
            writer.write("tabInactive=" + Integer.toHexString(theme.getTabInactive()) + "\n");
            writer.write("buttonBg=" + Integer.toHexString(theme.getButtonBg()) + "\n");
            writer.write("buttonHover=" + Integer.toHexString(theme.getButtonHover()) + "\n");
            writer.write("activeTextColor=" + Integer.toHexString(theme.getActiveTextColor()) + "\n");
            writer.write("textColor=" + Integer.toHexString(theme.getTextColor()) + "\n");
            writer.write("scrollBarColor=" + Integer.toHexString(theme.getScrollBarColor()) + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadConfig(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=");
                if (parts.length != 2) continue;
                String key = parts[0];
                int value = Integer.parseInt(parts[1], 16);
                switch (key) {
                    case "backgroundColor": theme.setBackgroundColor(value); break;
                    case "panelBgStart": theme.setPanelBgStart(value); break;
                    case "panelBgEnd": theme.setPanelBgEnd(value); break;
                    case "tabActive": theme.setTabActive(value); break;
                    case "tabInactive": theme.setTabInactive(value); break;
                    case "buttonBg": theme.setButtonBg(value); break;
                    case "buttonHover": theme.setButtonHover(value); break;
                    case "activeTextColor": theme.setActiveTextColor(value); break;
                    case "textColor": theme.setTextColor(value); break;
                    case "scrollBarColor": theme.setScrollBarColor(value); break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}