package com.yourname.vox.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper; // Added import

import java.util.function.Consumer;
import java.util.function.Supplier;

public class VoxColorButton extends VoxButton {
    private final Supplier<Integer> colorGetter;
    private final Consumer<Integer> colorSetter;

    public VoxColorButton(VoxTheme theme, String label, Supplier<Integer> colorGetter, Consumer<Integer> colorSetter) {
        super(theme, label);
        this.colorGetter = colorGetter;
        this.colorSetter = colorSetter;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean isHovered = mouseX >= x && mouseX <= x + getWidth() && mouseY >= y && mouseY <= y + getHeight();
        renderWidget(context, mouseX, mouseY, y, delta, isHovered, false);
    }

    @Override
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
        context.fill(renderX, renderY, renderX + scaledWidth, renderY + scaledHeight, bgColor);
        context.fill(renderX + scaledWidth - 20, renderY, renderX + scaledWidth, renderY + scaledHeight, colorGetter.get());
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(), renderX + 5, renderY + (scaledHeight - 8) / 2, theme.textColor);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        // Placeholder for color picker logic
        colorSetter.accept(0xFFFF0000); // Example: Set to red
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }
}