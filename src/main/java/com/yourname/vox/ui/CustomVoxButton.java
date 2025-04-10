package com.yourname.vox.ui;

import com.yourname.vox.ConfigManager;
import com.yourname.vox.IVoxAddon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper; // Added import
import org.joml.Matrix4f;

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

        // Auto-size text to fit within button width
        String text = getMessage().getString();
        int maxWidth = getWidth() - 8; // Account for padding
        float scale = 1.0f;
        int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(text);
        if (textWidth > maxWidth) {
            scale = (float) maxWidth / textWidth;
            scale = MathHelper.clamp(scale, 0.4f, 1.0f); // Allow smaller scale for better fitting
            // If scale is too small, truncate text instead
            if (scale < 0.4f) {
                scale = 0.4f;
                text = MinecraftClient.getInstance().textRenderer.trimToWidth(text, maxWidth) + "...";
            }
        }

        // Draw scaled text
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        context.getMatrices().push();
        context.getMatrices().scale(scale, scale, 1.0f);
        float scaledX = (getX() + 4) / scale;
        float scaledY = (getY() + (getHeight() - 8) / 2) / scale;
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, text, (int) scaledX, (int) scaledY, isActive ? 0xFF66B2FF : 0xFFFFFFFF);
        context.getMatrices().pop();
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