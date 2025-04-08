package com.yourname.vox.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class VoxWindow {
    private final VoxTheme theme;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final String title;

    public VoxWindow(VoxTheme theme, int x, int y, int width, int height, String title) {
        this.theme = theme;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.title = title;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        theme.renderRoundedRect(context, x, y, width, height, theme.getPanelBgStart(), theme.getPanelBgEnd());
        theme.renderShadow(context, x + 4, y + 4, width, height);
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, title, x + width / 2, y + 20, theme.getTextColor());
    }
}