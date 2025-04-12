package com.yourname.vox.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.function.Consumer;

public class SliderWidget extends ButtonWidget {
    private final double minValue;
    private final double maxValue;
    private double value;
    private final Consumer<Double> onChange;
    private boolean dragging;
    private final VoxTheme theme;

    public SliderWidget(VoxTheme theme, int x, int y, int width, int height, double minValue, double maxValue, double initialValue, Consumer<Double> onChange) {
        super(x, y, width, height, Text.literal(String.format("%.2f", initialValue)), btn -> {},
                button -> Text.literal(String.format("%.2f", initialValue)));
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.value = initialValue;
        this.onChange = onChange;
        this.dragging = false;
        this.theme = theme;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(getX(), getY() + getHeight() / 2 - 2, getX() + getWidth(), getY() + getHeight() / 2 + 2, theme.getScrollBarColor());
        int handlePos = getX() + (int) ((value - minValue) / (maxValue - minValue) * (getWidth() - 8));
        context.fill(handlePos, getY(), handlePos + 8, getY() + getHeight(), theme.getButtonActiveStart());
        String displayText = String.format("%.2f", value);
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, displayText,
                getX() + (getWidth() - MinecraftClient.getInstance().textRenderer.getWidth(displayText)) / 2,
                getY() + (getHeight() - 8) / 2, theme.getTextColor());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            dragging = true;
            updateValue(mouseX);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && button == 0) {
            updateValue(mouseX);
            return true;
        }
        return false;
    }

    private void updateValue(double mouseX) {
        double relativePos = (mouseX - getX()) / (getWidth() - 8);
        relativePos = Math.max(0, Math.min(1, relativePos));
        value = MathHelper.clamp(minValue + relativePos * (maxValue - minValue), minValue, maxValue);
        onChange.accept(value);
        setMessage(Text.literal(String.format("%.2f", value)));
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= getX() && mouseX <= getX() + getWidth() && mouseY >= getY() && mouseY <= getY() + getHeight();
    }

    public void setValue(double newValue) {
        value = MathHelper.clamp(newValue, minValue, maxValue);
        setMessage(Text.literal(String.format("%.2f", value)));
        onChange.accept(value);
    }
}