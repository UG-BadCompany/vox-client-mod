package com.yourname.vox.ui;

import com.yourname.vox.ConfigManager;
import com.yourname.vox.IVoxAddon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class VoxButton extends ClickableWidget {
    protected final VoxTheme theme;
    public final IVoxAddon addon;
    public int x, y;
    protected float hoverScale = 1.0f; // Made protected for subclass access
    protected String label;

    public VoxButton(VoxTheme theme, IVoxAddon addon) {
        super(0, 0, theme.buttonWidth, theme.buttonHeight, Text.literal(addon != null ? addon.getName() : ""));
        this.theme = theme;
        this.addon = addon;
        this.label = addon != null ? addon.getName() : "";
    }

    public VoxButton(VoxTheme theme, String label) {
        super(0, 0, theme.buttonWidth, theme.buttonHeight, Text.literal(label));
        this.theme = theme;
        this.addon = null;
        this.label = label;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // This method is required to override the abstract method in ClickableWidget
        // We'll delegate to our custom render method for consistency
        boolean isHovered = mouseX >= x && mouseX <= x + getWidth() && mouseY >= y && mouseY <= y + getHeight();
        boolean isActive = addon != null && ConfigManager.addonToggles.getOrDefault(addon.getName(), false);
        renderWidget(context, mouseX, mouseY, y, delta, isHovered, isActive);
    }

    public void renderWidget(DrawContext context, int mouseX, int mouseY, int yPos, float delta, boolean isHovered, boolean isActive) {
        this.y = yPos;
        hoverScale = MathHelper.lerp(0.2f * delta, hoverScale, isHovered ? 1.05f : 1.0f);
        int scaledWidth = (int) (getWidth() * hoverScale);
        int scaledHeight = (int) (getHeight() * hoverScale);
        int offsetX = (scaledWidth - getWidth()) / 2;
        int offsetY = (scaledHeight - getHeight()) / 2;
        int renderX = x - offsetX;
        int renderY = y - offsetY;

        int bgColor = isHovered ? theme.buttonHover : theme.buttonBg;
        int textColor = isActive ? theme.activeTextColor : theme.textColor;

        // Transparent background with a slight hover tint
        context.fill(renderX, renderY, renderX + scaledWidth, renderY + scaledHeight, bgColor);
        if (isActive) {
            context.fill(renderX, renderY, renderX + 2, renderY + scaledHeight, theme.activeTextColor);
        }
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, label, renderX + 5, renderY + (scaledHeight - 8) / 2, textColor);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (addon != null) {
            addon.toggle();
        }
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    @Override
    public void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}