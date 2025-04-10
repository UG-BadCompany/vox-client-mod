package com.yourname.vox.ui;

import com.yourname.vox.ConfigManager;
import com.yourname.vox.IVoxAddon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public class CustomVoxButton extends ClickableWidget {
    private final IVoxAddon addon;
    private final VoxTheme theme;
    private boolean visible = true;
    private final PressAction onPress;
    private final TooltipSupplier tooltipSupplier;

    public CustomVoxButton(int x, int y, int width, int height, IVoxAddon addon) {
        super(x, y, width, height, Text.literal(addon.getName()));
        this.addon = addon;
        this.theme = new VoxTheme();
        this.onPress = btn -> addon.toggle();
        this.tooltipSupplier = btn -> Text.literal(addon.getDescription());
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        boolean isActive = ConfigManager.addonToggles.getOrDefault(addon.getName(), false);
        boolean isHovered = mouseX >= getX() && mouseX < getX() + getWidth() && mouseY >= getY() && mouseY < getY() + getHeight();
        // Draw background if active or hovered
        if (isActive || isHovered) {
            int bgColor = isActive ? 0xFF1C3A5E : 0xFF4A4A4A; // Blue when active, light gray when hovered
            context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bgColor);
        }

        // Draw text
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(), getX() + 4, getY() + (getHeight() - 8) / 2, isActive ? 0xFF66B2FF : 0xFFFFFFFF);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (onPress != null) {
            onPress.onPress(this);
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public Text getMessage() {
        return super.getMessage();
    }

    public void setX(int x) {
        super.setX(x);
    }

    public void setY(int y) {
        super.setY(y);
    }

    public int getY() {
        return super.getY();
    }

    public int getHeight() {
        return super.getHeight();
    }

    @FunctionalInterface
    public interface PressAction {
        void onPress(CustomVoxButton button);
    }

    @FunctionalInterface
    public interface TooltipSupplier {
        Text getTooltip(CustomVoxButton button);
    }
}