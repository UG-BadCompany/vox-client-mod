package com.yourname.vox.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.math.MathHelper;

public class SearchWindow {
    private final VoxTheme theme;
    private final TextFieldWidget searchField;
    private int x, y;
    private final int width = 130;
    private final int height = 30;
    private boolean isDragging = false;
    private int dragOffsetX, dragOffsetY;
    private float dragSmoothX, dragSmoothY;

    public SearchWindow(VoxTheme theme, int x, int y, TextFieldWidget searchField) {
        this.theme = theme;
        this.x = x;
        this.y = y;
        this.dragSmoothX = x;
        this.dragSmoothY = y;
        this.searchField = searchField;
        this.searchField.setX(x + 5);
        this.searchField.setY(y + 5);
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Instant movement (no smoothing)
        dragSmoothX = x;
        dragSmoothY = y;
        int renderX = (int) dragSmoothX;
        int renderY = (int) dragSmoothY;

        // Transparent background (no border, no rounded corners for performance)
        context.fill(renderX, renderY, renderX + width, renderY + height, 0x40000000);

        // Update search field position
        searchField.setX(renderX + 5);
        searchField.setY(renderY + 5);
        searchField.render(context, mouseX, mouseY, delta);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Dragging the window
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            isDragging = true;
            dragOffsetX = (int) (mouseX - x);
            dragOffsetY = (int) (mouseY - y);
            return true;
        }
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        isDragging = false;
    }

    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDragging) {
            x = (int) (mouseX - dragOffsetX);
            y = (int) (mouseY - dragOffsetY);
        }
    }
}