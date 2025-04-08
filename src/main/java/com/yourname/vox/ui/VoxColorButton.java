package com.yourname.vox.ui;

import com.yourname.vox.ConfigManager;
import com.yourname.vox.IVoxAddon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class VoxColorButton extends ButtonWidget {
    private final IVoxAddon addon;
    private final VoxTheme theme;
    private boolean visible = true;

    public VoxColorButton(int x, int y, int width, int height, IVoxAddon addon) {
        super(x, y, width, height, Text.literal(addon.getName()), btn -> addon.toggle(), btn -> Text.literal(addon.getDescription()));
        this.addon = addon;
        this.theme = new VoxTheme();
    }

    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        boolean isActive = ConfigManager.addonToggles.getOrDefault(addon.getName(), false);
        // Only draw a background if active or hovered
        if (isActive || isHovered()) {
            int bgStart = isActive ? theme.getButtonActiveStart() : theme.getButtonHoverStart();
            int bgEnd = isActive ? theme.getButtonActiveEnd() : theme.getButtonHoverEnd();
            context.fillGradient(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bgStart, bgEnd);
        }

        // Draw text without any default background
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(), getX() + 4, getY() + (getHeight() - 8) / 2, isActive ? theme.getActiveTextColor() : theme.getTextColor());

        int toggleColor = isActive ? theme.getToggleOnColor() : theme.getToggleOffColor();
        context.fill(getX() + getWidth() - 8, getY() + getHeight() / 2 - 2, getX() + getWidth() - 4, getY() + getHeight() / 2 + 2, toggleColor);
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }
}