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
    private final int width = 80;
    private final int height = 400;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private boolean dragging = false;
    private int dragOffsetX;
    private int dragOffsetY;
    private final int titleBarHeight = 16;

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
        context.fill(x, y, x + width, y + titleBarHeight, 0xFF1C2526);
        String displayText = category.length() > 10 ? category.substring(0, 10) + "..." : category;
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, displayText, x + width / 2, y + 4, 0xFFFFFFFF);

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
}