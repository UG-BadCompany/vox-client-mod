package com.yourname.vox.ui;

import com.yourname.vox.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

public class VoxTheme {
    // Layout constants
    public final int tabWidth = 80;
    public final int tabHeight = 30;
    public final int buttonWidth = 70;
    public final int buttonHeight = 14; // Slightly smaller to match the reference
    public final int spacing = 4; // Reduced for a more compact look
    public final int columns = 4;
    public final int scrollBarWidth = 4;
    public final int cornerRadius = 3;
    public final int contentHeight = 310;

    // Colors
    public int backgroundColor = 0xC01C2526;
    public int panelBgStart = 0xFF1C2526;
    public int panelBgEnd = 0xFF2A2F32; // Slightly lighter for a subtle gradient
    public int tabActive = 0xFF66B0FF;
    public int tabInactive = 0xFF666666;
    public int buttonBg = 0x00000000; // Transparent background for buttons
    public int buttonHover = 0x20FFFFFF; // Slight white tint on hover
    public int activeTextColor = 0xFF66B0FF;
    public int textColor = 0xFFFFFFFF;
    public int shadowColor = 0x20000000;
    public int scrollBarColor = 0xFF666666;

    public VoxTheme() {
        loadColors();
    }

    private void loadColors() {
        backgroundColor = (Integer) ConfigManager.addonSettings.getOrDefault("ui_background_color", backgroundColor);
        panelBgStart = (Integer) ConfigManager.addonSettings.getOrDefault("ui_panel_start", panelBgStart);
        panelBgEnd = (Integer) ConfigManager.addonSettings.getOrDefault("ui_panel_end", panelBgEnd);
        tabActive = (Integer) ConfigManager.addonSettings.getOrDefault("ui_tab_active", tabActive);
    }

    public void renderRoundedRect(DrawContext context, int x, int y, int width, int height, int colorStart, int colorEnd) {
        int x2 = x + width;
        int y2 = y + height;
        context.fill(x + cornerRadius, y, x2 - cornerRadius, y2, colorStart);
        context.fillGradient(x, y + cornerRadius, x + cornerRadius, y2 - cornerRadius, colorStart, colorEnd);
        context.fillGradient(x2 - cornerRadius, y + cornerRadius, x2, y2 - cornerRadius, colorStart, colorEnd);
        for (int i = 0; i < cornerRadius; i++) {
            int alpha = (int) (255 * (1.0 - (double) i / cornerRadius));
            int blendedStart = (colorStart & 0x00FFFFFF) | (alpha << 24);
            int blendedEnd = (colorEnd & 0x00FFFFFF) | (alpha << 24);
            context.fill(x + cornerRadius - i, y + i, x + cornerRadius - i + 1, y + i + 1, blendedStart);
            context.fill(x2 - cornerRadius + i, y + i, x2 - cornerRadius + i + 1, y + i + 1, blendedStart);
            context.fill(x + cornerRadius - i, y2 - i - 1, x + cornerRadius - i + 1, y2 - i, blendedEnd);
            context.fill(x2 - cornerRadius + i, y2 - i - 1, x2 - cornerRadius + i + 1, y2 - i, blendedEnd);
        }
    }

    public void renderShadow(DrawContext context, int x, int y, int width, int height) {
        renderRoundedRect(context, x, y, width, height, shadowColor, shadowColor);
    }

    public void renderScrollBar(DrawContext context, int x, int y, int height) {
        renderRoundedRect(context, x, y, scrollBarWidth, height, scrollBarColor, scrollBarColor);
    }

    public void renderTooltip(DrawContext context, String text, int mouseX, int mouseY) {
        int tooltipWidth = MinecraftClient.getInstance().textRenderer.getWidth(text) + 16;
        int tooltipHeight = MinecraftClient.getInstance().textRenderer.fontHeight + 12;
        int tooltipX = MathHelper.clamp(mouseX + 12, 0, MinecraftClient.getInstance().getWindow().getScaledWidth() - tooltipWidth);
        int tooltipY = MathHelper.clamp(mouseY - tooltipHeight - 4, 0, MinecraftClient.getInstance().getWindow().getScaledHeight() - tooltipHeight);

        renderRoundedRect(context, tooltipX, tooltipY, tooltipWidth, tooltipHeight, 0xD0333333, 0xD0555555);
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, text, tooltipX + 8, tooltipY + 6, textColor);
    }
}