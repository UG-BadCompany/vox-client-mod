package com.yourname.vox.ui;

import com.yourname.vox.IVoxAddon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class VoxButton extends ButtonWidget {
    private final VoxTheme theme;
    private final IVoxAddon addon;
    private final boolean isActive;

    public VoxButton(int x, int y, VoxTheme theme, IVoxAddon addon) {
        super(x, y, theme.getButtonWidth(), theme.getButtonHeight(), Text.literal(addon != null ? addon.getName() : ""), btn -> {
            if (addon != null) addon.toggle();
        }, btn -> Text.literal(addon != null ? addon.getDescription() : ""));
        this.theme = theme;
        this.addon = addon;
        this.isActive = false; // Placeholder
    }

    public VoxButton(int x, int y, VoxTheme theme, String label) {
        super(x, y, theme.getButtonWidth(), theme.getButtonHeight(), Text.literal(label), btn -> {}, btn -> Text.empty());
        this.theme = theme;
        this.addon = null;
        this.isActive = false;
    }

    public VoxButton(VoxTheme theme, String label) {
        super(0, 0, theme.getButtonWidth(), theme.getButtonHeight(), Text.literal(label), btn -> {}, btn -> Text.empty());
        this.theme = theme;
        this.addon = null;
        this.isActive = false;
    }

    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        // Only draw background if active or hovered
        if (isHovered()) {
            int bgColor = theme.getButtonHover();
            context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bgColor);
        }
        int textColor = isActive ? theme.getActiveTextColor() : theme.getTextColor();
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(), getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, textColor);
        if (isActive) {
            context.fill(getX(), getY(), getX() + 2, getY() + getHeight(), theme.getActiveTextColor());
        }
    }
}