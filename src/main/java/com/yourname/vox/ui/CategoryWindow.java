package com.yourname.vox.ui;

import com.yourname.vox.IVoxAddon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class CategoryWindow {
    private final VoxTheme theme;
    private final String category;
    private final List<IVoxAddon> addons;
    private final List<VoxColorButton> buttons;
    private int x; // Movable
    private int y; // Movable
    private final int width = 90;
    private final int height = 350;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private boolean dragging = false;
    private int dragOffsetX;
    private int dragOffsetY;
    private final int titleBarHeight = 12;

    public CategoryWindow(VoxTheme theme, String category, List<IVoxAddon> addons, int x, int y) {
        this.theme = theme;
        this.category = category;
        this.addons = addons;
        this.x = x;
        this.y = y;
        this.buttons = new ArrayList<>();

        int buttonY = titleBarHeight; // Relative to window's top
        for (IVoxAddon addon : addons) {
            buttons.add(new VoxColorButton(x + 5, y + buttonY, 80, 14, addon));
            buttonY += 16;
        }
        maxScroll = Math.max(0, (buttons.size() * 16) - (height - titleBarHeight));
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Only draw background for the title bar area
        context.fill(x, y, x + width, y + titleBarHeight, 0xFF333333); // Darker title bar
        String displayText = category.length() > 10 ? category.substring(0, 10) + "..." : category;
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, displayText, x + width / 2, y + 2, 0xFF66B2FF);

        // No background fill for the addon area
        context.enableScissor(x, y + titleBarHeight, x + width, y + height);
        for (VoxColorButton button : buttons) {
            // Update Y position with scroll offset
            button.setY(button.getY() - scrollOffset);
            if (button.getY() + button.getHeight() >= y + titleBarHeight && button.getY() <= y + height) {
                button.render(context, mouseX, mouseY, delta);
            }
        }
        context.disableScissor();

        if (maxScroll > 0) {
            int scrollBarX = x + width - 4;
            int scrollBarHeight = (int) (((float) (height - titleBarHeight) / (height - titleBarHeight + maxScroll)) * (height - titleBarHeight));
            int scrollBarY = y + titleBarHeight + (int) (((float) scrollOffset / maxScroll) * (height - titleBarHeight - scrollBarHeight));
            context.fill(scrollBarX, scrollBarY, scrollBarX + 3, scrollBarY + scrollBarHeight, theme.getScrollBarColor());
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + titleBarHeight) {
                dragging = true;
                dragOffsetX = (int) (mouseX - x);
                dragOffsetY = (int) (mouseY - y);
                return true;
            }
            for (VoxColorButton btn : buttons) {
                if (btn.mouseClicked(mouseX, mouseY, button)) return true;
            }
        }
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        for (VoxColorButton btn : buttons) {
            btn.mouseReleased(mouseX, mouseY, button);
        }
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            int newX = (int) (mouseX - dragOffsetX);
            int newY = (int) (mouseY - dragOffsetY);
            newX = MathHelper.clamp(newX, 0, MinecraftClient.getInstance().getWindow().getScaledWidth() - width);
            newY = MathHelper.clamp(newY, 0, MinecraftClient.getInstance().getWindow().getScaledHeight() - height);

            // Calculate the delta movement
            int deltaXMove = newX - x;
            int deltaYMove = newY - y;

            // Update window position
            x = newX;
            y = newY;

            // Move all buttons with the window
            for (VoxColorButton btn : buttons) {
                btn.setX(btn.getX() + deltaXMove);
                btn.setY(btn.getY() + deltaYMove - scrollOffset);
            }
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height && maxScroll > 0) {
            int previousScrollOffset = scrollOffset;
            scrollOffset = MathHelper.clamp(scrollOffset - (int) (verticalAmount * 20), 0, maxScroll);
            int scrollDelta = scrollOffset - previousScrollOffset;
            // Update button positions based on scroll delta
            for (VoxColorButton btn : buttons) {
                btn.setY(btn.getY() - scrollDelta);
            }
            return true;
        }
        return false;
    }

    public void filterButtons(String query) {
        for (VoxColorButton button : buttons) {
            button.setVisible(button.getMessage().getString().toLowerCase().contains(query.toLowerCase()));
        }
        updateScroll();
    }

    private void updateScroll() {
        int visibleButtons = (int) buttons.stream().filter(VoxColorButton::isVisible).count();
        maxScroll = Math.max(0, (visibleButtons * 16) - (height - titleBarHeight));
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);
    }
}