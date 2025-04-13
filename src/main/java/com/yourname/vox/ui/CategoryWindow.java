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
    private final List<CustomVoxButton> buttons;
    private int x;
    private int y;
    private int width = 80;
    private int height = 400;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private boolean dragging = false;
    private int dragOffsetX;
    private int dragOffsetY;
    private final int titleBarHeight = 16;
    private int r = 255, g = 255, b = 255; // Body RGB
    private int titleR = 255, titleG = 255, titleB = 255; // Title bar RGB
    private int borderR = 255, borderG = 255, borderB = 255; // Border RGB
    private boolean borderEnabled = false; // Border toggle

    public CategoryWindow(VoxTheme theme, String category, List<IVoxAddon> addons, int x, int y) {
        this.theme = theme;
        this.category = category;
        this.addons = addons;
        this.x = x;
        this.y = y;
        this.buttons = new ArrayList<>();

        int buttonY = titleBarHeight;
        for (IVoxAddon addon : addons) {
            buttons.add(new CustomVoxButton(x + 5, y + buttonY, 70, 14, addon));
            buttonY += 16;
        }
        maxScroll = Math.max(0, (buttons.size() * 16) - (height - titleBarHeight));
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Border (2px thick)
        if (borderEnabled && (borderR != 255 || borderG != 255 || borderB != 255)) {
            context.fill(x - 2, y - 2, x + width + 2, y, (0xFF << 24) | (borderR << 16) | (borderG << 8) | borderB); // Top
            context.fill(x - 2, y + height, x + width + 2, y + height + 2, (0xFF << 24) | (borderR << 16) | (borderG << 8) | borderB); // Bottom
            context.fill(x - 2, y, x, y + height, (0xFF << 24) | (borderR << 16) | (borderG << 8) | borderB); // Left
            context.fill(x + width, y, x + width + 2, y + height, (0xFF << 24) | (borderR << 16) | (borderG << 8) | borderB); // Right
        }

        // Title bar
        context.fill(x, y, x + width, y + titleBarHeight,
                (titleR != 255 || titleG != 255 || titleB != 255) ?
                        (0xFF << 24) | (titleR << 16) | (titleG << 8) | titleB :
                        (0x99 << 24) | (0x1C << 16) | (0x25 << 8) | 0x26);
        String displayText = category.length() > 10 ? category.substring(0, 10) + "..." : category;
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, displayText, x + width / 2, y + 4, 0xFFFFFFFF);

        // Body RGB (optional)
        if (r != 255 || g != 255 || b != 255) {
            context.fill(x, y + titleBarHeight, x + width, y + height, (0xFF << 24) | (r << 16) | (g << 8) | b);
        }

        context.enableScissor(x, y + titleBarHeight, x + width, y + height);
        for (CustomVoxButton button : buttons) {
            button.setY(button.getY() - scrollOffset);
            if (button.getY() + button.getHeight() >= y + titleBarHeight && button.getY() <= y + height) {
                button.render(context, mouseX, mouseY, delta);
            }
        }
        context.disableScissor();

        if (maxScroll > 0) {
            int scrollBarX = x + width - 3;
            int scrollBarHeight = (int) (((float) (height - titleBarHeight) / (height - titleBarHeight + maxScroll)) * (height - titleBarHeight));
            int scrollBarY = y + titleBarHeight + (int) (((float) scrollOffset / maxScroll) * (height - titleBarHeight - scrollBarHeight));
            context.fill(scrollBarX, scrollBarY, scrollBarX + 3, scrollBarY + scrollBarHeight, 0xFF666666);
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
            for (CustomVoxButton btn : buttons) {
                if (btn.mouseClicked(mouseX, mouseY, button)) return true;
            }
        }
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        for (CustomVoxButton btn : buttons) {
            btn.mouseReleased(mouseX, mouseY, button);
        }
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            int newX = (int) (mouseX - dragOffsetX);
            int newY = (int) (mouseY - dragOffsetY);
            newX = MathHelper.clamp(newX, 0, MinecraftClient.getInstance().getWindow().getScaledWidth() - width);
            newY = MathHelper.clamp(newY, 0, MinecraftClient.getInstance().getWindow().getScaledHeight() - height);

            int deltaXMove = newX - x;
            int deltaYMove = newY - y;

            x = newX;
            y = newY;

            for (CustomVoxButton btn : buttons) {
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
            for (CustomVoxButton btn : buttons) {
                btn.setY(btn.getY() - scrollDelta);
            }
            return true;
        }
        return false;
    }

    public void filterButtons(String query) {
        for (CustomVoxButton button : buttons) {
            button.setVisible(button.getMessage().getString().toLowerCase().contains(query.toLowerCase()));
        }
        updateScroll();
    }

    private void updateScroll() {
        int visibleButtons = (int) buttons.stream().filter(CustomVoxButton::isVisible).count();
        maxScroll = Math.max(0, (visibleButtons * 16) - (height - titleBarHeight));
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);
    }

    public String getCategory() {
        return category;
    }

    public void setPosition(int x, int y) {
        int deltaX = x - this.x;
        int deltaY = y - this.y;
        this.x = MathHelper.clamp(x, 0, MinecraftClient.getInstance().getWindow().getScaledWidth() - width);
        this.y = MathHelper.clamp(y, 0, MinecraftClient.getInstance().getWindow().getScaledHeight() - height);
        for (CustomVoxButton btn : buttons) {
            btn.setX(btn.getX() + deltaX);
            btn.setY(btn.getY() + deltaY - scrollOffset);
        }
    }

    public void setSize(int width, int height) {
        this.width = Math.max(50, Math.min(500, width));
        this.height = Math.max(50, Math.min(600, height));
        maxScroll = Math.max(0, (buttons.size() * 16) - (height - titleBarHeight));
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);
    }

    public void setColor(int r, int g, int b, int titleR, int titleG, int titleB) {
        this.r = MathHelper.clamp(r, 0, 255);
        this.g = MathHelper.clamp(g, 0, 255);
        this.b = MathHelper.clamp(b, 0, 255);
        this.titleR = MathHelper.clamp(titleR, 0, 255);
        this.titleG = MathHelper.clamp(titleG, 0, 255);
        this.titleB = MathHelper.clamp(titleB, 0, 255);
    }

    public void setBorderColor(int r, int g, int b) {
        this.borderR = MathHelper.clamp(r, 0, 255);
        this.borderG = MathHelper.clamp(g, 0, 255);
        this.borderB = MathHelper.clamp(b, 0, 255);
    }

    public void setBorderEnabled(boolean enabled) {
        this.borderEnabled = enabled;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int[] getBodyColor() {
        return new int[]{r, g, b};
    }

    public int[] getTitleColor() {
        return new int[]{titleR, titleG, titleB};
    }

    public int[] getBorderColor() {
        return new int[]{borderR, borderG, borderB};
    }

    public boolean isBorderEnabled() {
        return borderEnabled;
    }
}