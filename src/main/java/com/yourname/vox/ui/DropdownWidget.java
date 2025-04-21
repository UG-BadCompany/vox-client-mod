package com.yourname.vox.ui;

import com.yourname.vox.IVoxAddon;
import com.yourname.vox.AddonSettingsConfig;
import com.yourname.vox.ConfigManager;
import com.yourname.vox.features.addons.HighwayNav;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DropdownWidget extends ClickableWidget {
    private final IVoxAddon addon;
    private final VoxTheme theme;
    private final CategoryWindow parentWindow; // Reference to parent CategoryWindow for color inheritance
    private boolean isVisible = false;
    private final List<String> settings;
    private int bgR, bgG, bgB; // Background (inherits from CategoryWindow)
    private int borderR, borderG, borderB; // Border (inherits from CategoryWindow)
    private int titleR, titleG, titleB; // Title bar (inherits from CategoryWindow)
    private boolean borderEnabled; // Matches CategoryWindow
    private final int itemHeight = 14; // Match CustomVoxButton height
    private final int buttonColor = 0xFF555555; // Gray, matching CustomVoxButton
    private final int buttonHoverColor = 0xFF777777; // Lighter gray for hover

    public DropdownWidget(int x, int y, int width, int height, IVoxAddon addon, VoxTheme theme, CategoryWindow parentWindow) {
        super(x, y, 200, height, Text.literal(addon.getName())); // Increased width to 200px
        this.addon = addon;
        this.theme = theme;
        this.parentWindow = parentWindow;
        this.settings = new ArrayList<>();
        List<String> settingsOptions = AddonSettingsConfig.getSettingsOptions().get(addon.getName());
        if (settingsOptions == null || settingsOptions.isEmpty()) {
            settingsOptions = Arrays.asList(
                    "Axis: x+, x-, z+, z-, x+z+, x-z+, x+z-, x-z-",
                    "Sprint: true, false",
                    "AutoJump: true, false",
                    "Speed: 0.05, 1.0",
                    "PathWidth: 0.5, 2.0"
            );
            System.out.println("[Vox] Using fallback settings for " + addon.getName());
        }
        this.settings.addAll(settingsOptions);
        this.height = settings.size() * 16; // 14px height + 2px gap per setting

        // Inherit colors from parent CategoryWindow
        int[] bodyColor = parentWindow.getBodyColor();
        int[] borderColor = parentWindow.getBorderColor();
        int[] titleColor = parentWindow.getTitleColor();
        this.bgR = bodyColor[0];
        this.bgG = bodyColor[1];
        this.bgB = bodyColor[2];
        this.borderR = borderColor[0];
        this.borderG = borderColor[1];
        this.borderB = borderColor[2];
        this.titleR = titleColor[0];
        this.titleG = titleColor[1];
        this.titleB = titleColor[2];
        this.borderEnabled = parentWindow.isBorderEnabled();

        System.out.println("[Vox] DropdownWidget initialized: addon=" + addon.getName() + ", x=" + x + ", y=" + y + ", width=" + width + ", height=" + this.height +
                ", bgR=" + bgR + ", bgG=" + bgG + ", bgB=" + bgB + ", borderR=" + borderR + ", borderG=" + borderG + ", borderB=" + borderB +
                ", titleR=" + titleR + ", titleG=" + titleG + ", titleB=" + titleB + ", borderEnabled=" + borderEnabled);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!isVisible) {
            System.out.println("[Vox] DropdownWidget skipped rendering: addon=" + addon.getName());
            return;
        }

        System.out.println("[Vox] Rendering DropdownWidget: addon=" + addon.getName() + ", x=" + getX() + ", y=" + getY() +
                ", bgR=" + bgR + ", bgG=" + bgG + ", bgB=" + bgB + ", borderR=" + borderR + ", borderG=" + borderG + ", borderB=" + borderB +
                ", titleR=" + titleR + ", titleG=" + titleG + ", titleB=" + titleB);

        // Draw border (match CategoryWindow's border state)
        if (borderEnabled) {
            context.fill(getX() - 2, getY() - 2, getX() + getWidth() + 2, getY(), (0xFF << 24) | (borderR << 16) | (borderG << 8) | borderB);
            context.fill(getX() - 2, getY() + height, getX() + getWidth() + 2, getY() + height + 2, (0xFF << 24) | (borderR << 16) | (borderG << 8) | borderB);
            context.fill(getX() - 2, getY(), getX(), getY() + height, (0xFF << 24) | (borderR << 16) | (borderG << 8) | borderB);
            context.fill(getX() + getWidth(), getY(), getX() + getWidth() + 2, getY() + height, (0xFF << 24) | (borderR << 16) | (borderG << 8) | borderB);
        }

        // Draw background (match CategoryWindow's white body)
        context.fill(getX(), getY(), getX() + getWidth(), getY() + height, (0xFF << 24) | (bgR << 16) | (bgG << 8) | bgB);

        int yOffset = getY();
        if (addon instanceof HighwayNav nav) {
            for (String setting : settings) {
                String[] parts = setting.split(": ");
                String settingName = parts[0];
                String[] options = parts[1].split(", ");
                String currentValue = getCurrentValue(settingName, nav);
                String displayText = settingName + ": " + currentValue;

                // Draw setting background (dark title bar, match CategoryWindow)
                int titleBarColor = (titleR != 255 || titleG != 255 || titleB != 255) ?
                        (0x99 << 24) | (titleR << 16) | (titleG << 8) | titleB :
                        (0x99 << 24) | (0x1C << 16) | (0x25 << 8) | 0x26;
                context.fill(getX(), yOffset, getX() + getWidth(), yOffset + itemHeight, titleBarColor);
                // Draw setting text (left-aligned like CustomVoxButton)
                context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, displayText, getX() + 5, yOffset + 3, 0xFFFFFF);

                // Render controls (match CustomVoxButton style)
                if (settingName.equals("Axis")) {
                    boolean isHovered = mouseX >= getX() + getWidth() - 30 && mouseX <= getX() + getWidth() - 10 && mouseY >= yOffset + 1 && mouseY <= yOffset + 11;
                    context.fill(getX() + getWidth() - 30, yOffset + 1, getX() + getWidth() - 10, yOffset + 11, isHovered ? buttonHoverColor : buttonColor);
                    context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, ">", getX() + getWidth() - 20, yOffset + 3, 0xFFFFFF);
                    System.out.println("[Vox] Rendered Axis control: x=" + (getX() + getWidth() - 30) + ", y=" + yOffset + ", text=" + displayText);
                } else if (settingName.equals("Sprint") || settingName.equals("AutoJump")) {
                    boolean isHovered = mouseX >= getX() + getWidth() - 20 && mouseX <= getX() + getWidth() && mouseY >= yOffset + 1 && mouseY <= yOffset + 11;
                    context.fill(getX() + getWidth() - 20, yOffset + 1, getX() + getWidth(), yOffset + 11,
                            isHovered ? (currentValue.equals("true") ? 0xFF33FF33 : 0xFFFF3333) : (currentValue.equals("true") ? 0xFF00FF00 : 0xFFFF0000));
                    System.out.println("[Vox] Rendered " + settingName + " control: x=" + (getX() + getWidth() - 20) + ", y=" + yOffset + ", text=" + displayText);
                } else if (settingName.equals("Speed") || settingName.equals("PathWidth")) {
                    float value = settingName.equals("Speed") ? nav.speed : nav.pathWidth;
                    float min = settingName.equals("Speed") ? 0.05f : 0.5f;
                    float max = settingName.equals("Speed") ? 1.0f : 2.0f;
                    float normalized = (value - min) / (max - min);
                    int sliderWidth = 50;
                    int sliderX = getX() + getWidth() - sliderWidth - 5;
                    boolean isHovered = mouseX >= sliderX && mouseX <= sliderX + sliderWidth && mouseY >= yOffset + 3 && mouseY <= yOffset + 7;
                    context.fill(sliderX, yOffset + 3, sliderX + sliderWidth, yOffset + 7, isHovered ? buttonHoverColor : buttonColor);
                    context.fill(sliderX, yOffset + 3, sliderX + (int) (sliderWidth * normalized), yOffset + 7, isHovered ? 0xFF33FF33 : 0xFF00FF00);
                    System.out.println("[Vox] Rendered " + settingName + " control: x=" + sliderX + ", y=" + yOffset + ", text=" + displayText);
                }
                yOffset += 16; // 14px height + 2px gap
            }
        } else {
            for (String setting : settings) {
                String[] parts = setting.split(": ");
                String settingName = parts[0];
                String[] options = parts[1].split(", ");
                String currentValue = ConfigManager.addonSettings.getOrDefault(addon.getName() + "_" + settingName.toLowerCase(), options[0]).toString();
                String displayText = settingName + ": " + currentValue;

                // Draw setting background (dark title bar)
                int titleBarColor = (titleR != 255 || titleG != 255 || titleB != 255) ?
                        (0x99 << 24) | (titleR << 16) | (titleG << 8) | titleB :
                        (0x99 << 24) | (0x1C << 16) | (0x25 << 8) | 0x26;
                context.fill(getX(), yOffset, getX() + getWidth(), yOffset + itemHeight, titleBarColor);
                // Draw setting text
                context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, displayText, getX() + 5, yOffset + 3, 0xFFFFFF);

                // Draw arrow button
                boolean isHovered = mouseX >= getX() + getWidth() - 30 && mouseX <= getX() + getWidth() - 10 && mouseY >= yOffset + 1 && mouseY <= yOffset + 11;
                context.fill(getX() + getWidth() - 30, yOffset + 1, getX() + getWidth() - 10, yOffset + 11, isHovered ? buttonHoverColor : buttonColor);
                context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, ">", getX() + getWidth() - 20, yOffset + 3, 0xFFFFFF);
                System.out.println("[Vox] Rendered " + settingName + " control: x=" + (getX() + getWidth() - 30) + ", y=" + yOffset + ", text=" + displayText);
                yOffset += 16;
            }
        }
    }

    private String getCurrentValue(String settingName, HighwayNav nav) {
        return switch (settingName) {
            case "Axis" -> nav.axis;
            case "Sprint" -> String.valueOf(nav.sprint);
            case "AutoJump" -> String.valueOf(nav.autoJump);
            case "Speed" -> String.format("%.2f", nav.speed);
            case "PathWidth" -> String.format("%.2f", nav.pathWidth);
            default -> "";
        };
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (!isVisible || !isMouseOver(mouseX, mouseY)) {
            System.out.println("[Vox] DropdownWidget onClick skipped: addon=" + addon.getName() + ", not visible or not over");
            return;
        }

        System.out.println("[Vox] DropdownWidget onClick: addon=" + addon.getName() + ", mouseX=" + mouseX + ", mouseY=" + mouseY);
        int yOffset = getY();
        if (addon instanceof HighwayNav nav) {
            for (String setting : settings) {
                String[] parts = setting.split(": ");
                String settingName = parts[0];
                String[] options = parts[1].split(", ");
                if (mouseY >= yOffset && mouseY < yOffset + itemHeight) {
                    if (settingName.equals("Axis") && mouseX >= getX() + getWidth() - 30 && mouseX <= getX() + getWidth() - 10) {
                        int currentIndex = Arrays.asList(options).indexOf(nav.axis);
                        String nextAxis = options[(currentIndex + 1) % options.length];
                        nav.setAxis(nextAxis);
                        System.out.println("[Vox] Updated Axis to " + nextAxis);
                        MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.literal("HighwayNav Axis: " + nextAxis), false);
                        ConfigManager.saveConfig();
                    } else if (settingName.equals("Sprint") && mouseX >= getX() + getWidth() - 20 && mouseX <= getX() + getWidth()) {
                        nav.setSprint(!nav.sprint);
                        System.out.println("[Vox] Updated Sprint to " + nav.sprint);
                        MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.literal("HighwayNav Sprint: " + nav.sprint), false);
                        ConfigManager.saveConfig();
                    } else if (settingName.equals("AutoJump") && mouseX >= getX() + getWidth() - 20 && mouseX <= getX() + getWidth()) {
                        nav.setAutoJump(!nav.autoJump);
                        System.out.println("[Vox] Updated AutoJump to " + nav.autoJump);
                        MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.literal("HighwayNav AutoJump: " + nav.autoJump), false);
                        ConfigManager.saveConfig();
                    } else if (settingName.equals("Speed") || settingName.equals("PathWidth")) {
                        float min = settingName.equals("Speed") ? 0.05f : 0.5f;
                        float max = settingName.equals("Speed") ? 1.0f : 2.0f;
                        int sliderWidth = 50;
                        int sliderX = getX() + getWidth() - sliderWidth - 5;
                        if (mouseX >= sliderX && mouseX <= sliderX + sliderWidth) {
                            float normalized = (float) (mouseX - sliderX) / sliderWidth;
                            float newValue = min + normalized * (max - min);
                            if (settingName.equals("Speed")) {
                                nav.setSpeed(newValue);
                                System.out.println("[Vox] Updated Speed to " + newValue);
                                MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.literal("HighwayNav Speed: " + String.format("%.2f", newValue)), false);
                            } else {
                                nav.setPathWidth(newValue);
                                System.out.println("[Vox] Updated PathWidth to " + String.format("%.2f", newValue));
                                MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.literal("HighwayNav PathWidth: " + String.format("%.2f", newValue)), false);
                            }
                            ConfigManager.saveConfig();
                        }
                    }
                }
                yOffset += 16;
            }
        } else {
            for (String setting : settings) {
                String[] parts = setting.split(": ");
                String settingName = parts[0];
                String[] options = parts[1].split(", ");
                if (mouseY >= yOffset && mouseY < yOffset + itemHeight && mouseX >= getX() + getWidth() - 30 && mouseX <= getX() + getWidth() - 10) {
                    String currentValue = ConfigManager.addonSettings.getOrDefault(addon.getName() + "_" + settingName.toLowerCase(), options[0]).toString();
                    int currentIndex = Arrays.asList(options).indexOf(currentValue);
                    String nextValue = options[(currentIndex + 1) % options.length];
                    ConfigManager.addonSettings.put(addon.getName() + "_" + settingName.toLowerCase(), nextValue);
                    System.out.println("[Vox] Updated " + settingName + " to " + nextValue);
                    MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.literal(addon.getName() + " " + settingName + ": " + nextValue), false);
                    ConfigManager.saveConfig();
                }
                yOffset += 16;
            }
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!isVisible || !(addon instanceof HighwayNav nav)) {
            System.out.println("[Vox] DropdownWidget mouseDragged skipped: addon=" + addon.getName() + ", not visible or not HighwayNav");
            return false;
        }

        System.out.println("[Vox] DropdownWidget mouseDragged: addon=" + addon.getName() + ", button=" + button + ", mouseX=" + mouseX + ", mouseY=" + mouseY);
        int yOffset = getY();
        for (String setting : settings) {
            String[] parts = setting.split(": ");
            String settingName = parts[0];
            if (mouseY >= yOffset && mouseY < yOffset + itemHeight && (settingName.equals("Speed") || settingName.equals("PathWidth"))) {
                float min = settingName.equals("Speed") ? 0.05f : 0.5f;
                float max = settingName.equals("Speed") ? 1.0f : 2.0f;
                int sliderWidth = 50;
                int sliderX = getX() + getWidth() - sliderWidth - 5;
                if (mouseX >= sliderX && mouseX <= sliderX + sliderWidth) {
                    float normalized = (float) (mouseX - sliderX) / sliderWidth;
                    float newValue = min + normalized * (max - min);
                    if (settingName.equals("Speed")) {
                        nav.setSpeed(newValue);
                        System.out.println("[Vox] Dragged Speed to " + newValue);
                        MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.literal("HighwayNav Speed: " + String.format("%.2f", newValue)), false);
                    } else {
                        nav.setPathWidth(newValue);
                        System.out.println("[Vox] Dragged PathWidth to " + String.format("%.2f", newValue));
                        MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.literal("HighwayNav PathWidth: " + String.format("%.2f", newValue)), false);
                    }
                    ConfigManager.saveConfig();
                    return true;
                }
            }
            yOffset += 16;
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!isVisible) return false;
        boolean over = mouseX >= getX() && mouseX <= getX() + getWidth() && mouseY >= getY() && mouseY <= getY() + height;
        System.out.println("[Vox] DropdownWidget isMouseOver: addon=" + addon.getName() + ", mouseX=" + mouseX + ", mouseY=" + mouseY + ", x=" + getX() + ", y=" + getY() + ", width=" + getWidth() + ", height=" + height + ", over=" + over);
        return over;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, "Settings dropdown for " + addon.getName());
    }

    public void setVisible(boolean visible) {
        this.isVisible = visible;
        System.out.println("[Vox] DropdownWidget setVisible: addon=" + addon.getName() + ", visible=" + visible);
    }

    public boolean isVisible() {
        return isVisible;
    }

    public IVoxAddon getAddon() {
        return addon;
    }
}