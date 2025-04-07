package com.yourname.vox.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class VoxWindow {
    private final VoxTheme theme;
    private final VoxScreen screen;
    private int x, y, width, height;
    private final String title;

    public VoxWindow(VoxTheme theme, VoxScreen screen, int width, int height, String title) {
        this.theme = theme;
        this.screen = screen;
        this.width = width;
        this.height = height;
        this.title = title;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        theme.renderRoundedRect(context, x, y, width, height, theme.panelBgStart, theme.panelBgEnd);
        theme.renderShadow(context, x + 4, y + 4, width, height);
        context.fillGradient(x, y, x + width, y + 50, 0xFF1A1A1A, 0xFF333333);
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, title, x + width / 2, y + 20, theme.textColor);
    }
}