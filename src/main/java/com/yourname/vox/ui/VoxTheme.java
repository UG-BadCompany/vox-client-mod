package com.yourname.vox.ui;

import net.minecraft.client.gui.DrawContext;

public class VoxTheme {
    private int backgroundColor = 0xA0333333;
    private int windowBgStart = 0x30222222;
    private int windowBgEnd = 0x30444444;
    private int panelBgStart = 0x30222222;
    private int panelBgEnd = 0x30444444;
    private int tabActive = 0xFF66B2FF;
    private int tabInactive = 0xFF666666;
    private int buttonWidth = 70;
    private int buttonHeight = 20;
    private int buttonBg = 0x80333333;
    private int buttonHover = 0x804A90E2;
    private int buttonBgStart = 0x80333333;
    private int buttonBgEnd = 0x80555555;
    private int buttonHoverStart = 0x804A90E2;
    private int buttonHoverEnd = 0x80357ABD;
    private int buttonActiveStart = 0xFF4A90E2;
    private int buttonActiveEnd = 0xFF357ABD;
    private int textColor = 0xFFFFFF;
    private int activeTextColor = 0xFFFFFF;
    private int toggleOnColor = 0xFF4CAF50;
    private int toggleOffColor = 0xFFE57373;
    private int scrollBarColor = 0xFF666666;

    public int getBackgroundColor() { return backgroundColor; }
    public int getWindowBgStart() { return windowBgStart; }
    public int getWindowBgEnd() { return windowBgEnd; }
    public int getPanelBgStart() { return panelBgStart; }
    public int getPanelBgEnd() { return panelBgEnd; }
    public int getTabActive() { return tabActive; }
    public int getTabInactive() { return tabInactive; }
    public int getButtonWidth() { return buttonWidth; }
    public int getButtonHeight() { return buttonHeight; }
    public int getButtonBg() { return buttonBg; }
    public int getButtonHover() { return buttonHover; }
    public int getButtonBgStart() { return buttonBgStart; }
    public int getButtonBgEnd() { return buttonBgEnd; }
    public int getButtonHoverStart() { return buttonHoverStart; }
    public int getButtonHoverEnd() { return buttonHoverEnd; }
    public int getButtonActiveStart() { return buttonActiveStart; }
    public int getButtonActiveEnd() { return buttonActiveEnd; }
    public int getTextColor() { return textColor; }
    public int getActiveTextColor() { return activeTextColor; }
    public int getToggleOnColor() { return toggleOnColor; }
    public int getToggleOffColor() { return toggleOffColor; }
    public int getScrollBarColor() { return scrollBarColor; }

    public void setBackgroundColor(int value) { this.backgroundColor = value; }
    public void setPanelBgStart(int value) { this.panelBgStart = value; }
    public void setPanelBgEnd(int value) { this.panelBgEnd = value; }
    public void setTabActive(int value) { this.tabActive = value; }
    public void setTabInactive(int value) { this.tabInactive = value; }
    public void setButtonBg(int value) { this.buttonBg = value; }
    public void setButtonHover(int value) { this.buttonHover = value; }
    public void setActiveTextColor(int value) { this.activeTextColor = value; }
    public void setTextColor(int value) { this.textColor = value; }
    public void setScrollBarColor(int value) { this.scrollBarColor = value; }

    public void renderRoundedRect(DrawContext context, int x, int y, int width, int height, int colorStart, int colorEnd) {
        context.fillGradient(x, y, x + width, y + height, colorStart, colorEnd);
    }

    public void renderShadow(DrawContext context, int x, int y, int width, int height) {
        context.fillGradient(x, y, x + width, y + height, 0x40000000, 0x40000000);
    }
}