package com.yourname.vox.ui;

import com.yourname.vox.IVoxAddon;
import com.yourname.vox.AddonSettingsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Arrays;
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
    private DropdownWidget activeDropdown = null;
    private int dropdownOffset = 0; // Offset for buttons below dropdown
    private final List<Integer> originalButtonYs = new ArrayList<>(); // Store original y positions

    public CategoryWindow(VoxTheme theme, String category, List<IVoxAddon> addons, int x, int y) {
        this.theme = theme;
        this.category = category;
        this.addons = addons;
        this.x = x;
        this.y = y;
        this.buttons = new ArrayList<>();

        int buttonY = y + titleBarHeight;
        for (IVoxAddon addon : addons) {
            CustomVoxButton button = new CustomVoxButton(x + 5, buttonY, 70, 14, addon) {
                @Override
                public boolean mouseClicked(double mouseX, double mouseY, int button) {
                    System.out.println("[Vox] CustomVoxButton mouseClicked: addon=" + addon.getName() + ", button=" + button + ", mouseX=" + mouseX + ", mouseY=" + mouseY + ", isMouseOver=" + isMouseOver(mouseX, mouseY));
                    if (button == 1 && isMouseOver(mouseX, mouseY)) {
                        System.out.println("[Vox] Right-click detected on " + addon.getName());
                        if (activeDropdown != null && activeDropdown.getAddon() == addon) {
                            activeDropdown.setVisible(false);
                            activeDropdown = null;
                            dropdownOffset = 0; // Reset offset when closing
                            System.out.println("[Vox] Closed dropdown for " + addon.getName());
                        } else {
                            if (activeDropdown != null) {
                                activeDropdown.setVisible(false);
                                System.out.println("[Vox] Closed previous dropdown for " + activeDropdown.getAddon().getName());
                            }
                            showDropdown(addon, this);
                        }
                        return true;
                    }
                    return super.mouseClicked(mouseX, mouseY, button);
                }

                @Override
                public boolean isMouseOver(double mouseX, double mouseY) {
                    int adjustedY = getY() - scrollOffset;
                    boolean over = mouseX >= getX() && mouseX < getX() + getWidth() &&
                            mouseY >= adjustedY && mouseY < adjustedY + getHeight();
                    System.out.println("[Vox] CustomVoxButton isMouseOver: addon=" + addon.getName() + ", mouseX=" + mouseX + ", mouseY=" + mouseY + ", x=" + getX() + ", y=" + adjustedY + ", width=" + getWidth() + ", height=" + getHeight() + ", scrollOffset=" + scrollOffset + ", over=" + over);
                    return over;
                }
            };
            buttons.add(button);
            originalButtonYs.add(buttonY);
            buttonY += 16;
        }
        maxScroll = Math.max(0, (buttons.size() * 16) - (height - titleBarHeight));
        System.out.println("[Vox] CategoryWindow initialized: category=" + category + ", addons=" + addons.size() + ", maxScroll=" + maxScroll);
    }

    private void showDropdown(IVoxAddon addon, CustomVoxButton button) {
        System.out.println("[Vox] showDropdown: addon=" + addon.getName() + ", buttonX=" + button.getX() + ", buttonY=" + button.getY() + ", scrollOffset=" + scrollOffset);
        List<String> settings = AddonSettingsConfig.getSettingsOptions().get(addon.getName());
        System.out.println("[Vox] Settings from AddonSettingsConfig: " + (settings != null ? settings : "null"));
        if (settings == null || settings.isEmpty()) {
            settings = Arrays.asList(
                    "Axis: x+, x-, z+, z-, x+z+, x-z+, x+z-, x-z-",
                    "Sprint: true, false",
                    "AutoJump: true, false",
                    "Speed: 0.05, 1.0",
                    "PathWidth: 0.5, 2.0"
            );
            System.out.println("[Vox] Using fallback settings for " + addon.getName() + ": " + settings);
        }
        int dropdownX = button.getX();
        int dropdownY = button.getY() + button.getHeight() + 2 - scrollOffset;
        dropdownX = MathHelper.clamp(dropdownX, x, x + width - 150);
        dropdownY = MathHelper.clamp(dropdownY, y + titleBarHeight, y + height - (settings.size() * 14 + 10));
        dropdownX = MathHelper.clamp(dropdownX, 0, MinecraftClient.getInstance().getWindow().getScaledWidth() - 150);
        dropdownY = MathHelper.clamp(dropdownY, 0, MinecraftClient.getInstance().getWindow().getScaledHeight() - (settings.size() * 14 + 10));
        activeDropdown = new DropdownWidget(dropdownX, dropdownY, 150, 20, addon, theme, this);
        activeDropdown.setVisible(true);
        dropdownOffset = activeDropdown.getHeight() + 2; // Set offset for buttons below
        System.out.println("[Vox] Created dropdown for " + addon.getName() + " at x=" + dropdownX + ", y=" + dropdownY + ", settings=" + settings.size() + ", dropdownOffset=" + dropdownOffset);
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (borderEnabled && (borderR != 255 || borderG != 255 || borderB != 255)) {
            context.fill(x - 2, y - 2, x + width + 2, y, (0xFF << 24) | (borderR << 16) | (borderG << 8) | borderB);
            context.fill(x - 2, y + height, x + width + 2, y + height + 2, (0xFF << 24) | (borderR << 16) | (borderG << 8) | borderB);
            context.fill(x - 2, y, x, y + height, (0xFF << 24) | (borderR << 16) | (borderG << 8) | borderB);
            context.fill(x + width, y, x + width + 2, y + height, (0xFF << 24) | (borderR << 16) | (borderG << 8) | borderB);
        }

        context.fill(x, y, x + width, y + titleBarHeight,
                (titleR != 255 || titleG != 255 || titleB != 255) ?
                        (0xFF << 24) | (titleR << 16) | (titleG << 8) | titleB :
                        (0x99 << 24) | (0x1C << 16) | (0x25 << 8) | 0x26);
        String displayText = category.length() > 10 ? category.substring(0, 10) + "..." : category;
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, displayText, x + width / 2, y + 4, 0xFFFFFFFF);

        if (r != 255 || g != 255 || b != 255) {
            context.fill(x, y + titleBarHeight, x + width, y + height, (0xFF << 24) | (r << 16) | (g << 8) | b);
        }

        context.enableScissor(x, y + titleBarHeight, x + width, y + height);
        int buttonIndex = 0;
        for (CustomVoxButton button : buttons) {
            int baseY = originalButtonYs.get(buttonIndex); // Use original y position
            int adjustedY = baseY - scrollOffset;
            // Apply dropdown offset for buttons below HighwayNav
            if (activeDropdown != null && activeDropdown.isVisible() && buttonIndex > buttons.indexOf(findHighwayNavButton())) {
                adjustedY = baseY + dropdownOffset - scrollOffset;
            }
            button.setY(adjustedY);
            if (adjustedY + button.getHeight() >= y + titleBarHeight && adjustedY <= y + height) {
                button.render(context, mouseX, mouseY, delta);
            }
            buttonIndex++;
        }
        context.disableScissor();

        if (maxScroll > 0) {
            int scrollBarX = x + width - 3;
            int scrollBarHeight = (int) (((float) (height - titleBarHeight) / (height - titleBarHeight + maxScroll)) * (height - titleBarHeight));
            int scrollBarY = y + titleBarHeight + (int) (((float) scrollOffset / maxScroll) * (height - titleBarHeight - scrollBarHeight));
            context.fill(scrollBarX, scrollBarY, scrollBarX + 3, scrollBarY + scrollBarHeight, 0xFF666666);
        }

        if (activeDropdown != null && activeDropdown.isVisible()) {
            System.out.println("[Vox] Rendering dropdown for " + activeDropdown.getAddon().getName() + " at x=" + activeDropdown.getX() + ", y=" + activeDropdown.getY());
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 200);
            context.enableScissor(x, y + titleBarHeight, x + width, y + height);
            activeDropdown.render(context, mouseX, mouseY, delta);
            context.disableScissor();
            context.getMatrices().pop();
        }
    }

    private CustomVoxButton findHighwayNavButton() {
        for (CustomVoxButton button : buttons) {
            if (button.getMessage().getString().equals("HighwayNav")) {
                return button;
            }
        }
        return null;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        System.out.println("[Vox] CategoryWindow mouseClicked: category=" + category + ", button=" + button + ", mouseX=" + mouseX + ", mouseY=" + mouseY);
        if (activeDropdown != null && activeDropdown.isVisible()) {
            if (activeDropdown.isMouseOver(mouseX, mouseY)) {
                if (activeDropdown.mouseClicked(mouseX, mouseY, button)) {
                    System.out.println("[Vox] Dropdown handled click for " + activeDropdown.getAddon().getName());
                    return true;
                }
            } else if (button == 0 || button == 1) {
                activeDropdown.setVisible(false);
                dropdownOffset = 0; // Reset offset when closing
                System.out.println("[Vox] Closed dropdown for " + activeDropdown.getAddon().getName() + " due to click outside");
                activeDropdown = null;
                return true;
            }
        }
        if (button == 0 || button == 1) {
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + titleBarHeight) {
                dragging = true;
                dragOffsetX = (int) (mouseX - x);
                dragOffsetY = (int) (mouseY - y);
                System.out.println("[Vox] CategoryWindow dragging started: category=" + category);
                return true;
            }
            int buttonIndex = 0;
            for (CustomVoxButton btn : buttons) {
                int baseY = originalButtonYs.get(buttonIndex);
                int adjustedY = baseY - scrollOffset;
                if (activeDropdown != null && activeDropdown.isVisible() && buttonIndex > buttons.indexOf(findHighwayNavButton())) {
                    adjustedY = baseY + dropdownOffset - scrollOffset;
                }
                btn.setY(adjustedY);
                if (adjustedY + btn.getHeight() >= y + titleBarHeight && adjustedY <= y + height) {
                    if (btn.mouseClicked(mouseX, mouseY, button)) {
                        System.out.println("[Vox] Button handled click: addon=" + btn.getMessage().getString());
                        return true;
                    }
                }
                buttonIndex++;
            }
        }
        System.out.println("[Vox] CategoryWindow no actionable click: category=" + category);
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        System.out.println("[Vox] CategoryWindow mouseReleased: category=" + category + ", button=" + button + ", mouseX=" + mouseX + ", mouseY=" + mouseY);
        dragging = false;
        for (CustomVoxButton btn : buttons) {
            btn.mouseReleased(mouseX, mouseY, button);
        }
        if (activeDropdown != null && activeDropdown.isVisible()) {
            activeDropdown.mouseReleased(mouseX, mouseY, button);
        }
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        System.out.println("[Vox] CategoryWindow mouseDragged: category=" + category + ", button=" + button + ", mouseX=" + mouseX + ", mouseY=" + mouseY);
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
                btn.setY(btn.getY() + deltaYMove);
            }
            if (activeDropdown != null && activeDropdown.isVisible()) {
                activeDropdown.setX(activeDropdown.getX() + deltaXMove);
                activeDropdown.setY(activeDropdown.getY() + deltaYMove);
            }
            return true;
        }
        if (activeDropdown != null && activeDropdown.isVisible()) {
            return activeDropdown.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        System.out.println("[Vox] CategoryWindow mouseScrolled: category=" + category + ", mouseX=" + mouseX + ", mouseY=" + mouseY + ", verticalAmount=" + verticalAmount);
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height && maxScroll > 0) {
            int previousScrollOffset = scrollOffset;
            scrollOffset = MathHelper.clamp(scrollOffset - (int) (verticalAmount * 20), 0, maxScroll);
            int scrollDelta = scrollOffset - previousScrollOffset;
            for (CustomVoxButton btn : buttons) {
                btn.setY(btn.getY() - scrollDelta);
            }
            if (activeDropdown != null && activeDropdown.isVisible()) {
                activeDropdown.setY(activeDropdown.getY() - scrollDelta);
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
            btn.setY(btn.getY() + deltaY);
        }
        if (activeDropdown != null && activeDropdown.isVisible()) {
            activeDropdown.setX(activeDropdown.getX() + deltaX);
            activeDropdown.setY(activeDropdown.getY() + deltaY);
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
        // Update dropdown colors if open
        if (activeDropdown != null && activeDropdown.isVisible()) {
            activeDropdown = null; // Force recreate to apply new colors
        }
    }

    public void setBorderColor(int r, int g, int b) {
        this.borderR = MathHelper.clamp(r, 0, 255);
        this.borderG = MathHelper.clamp(g, 0, 255);
        this.borderB = MathHelper.clamp(b, 0, 255);
        // Update dropdown colors if open
        if (activeDropdown != null && activeDropdown.isVisible()) {
            activeDropdown = null; // Force recreate to apply new colors
        }
    }

    public void setBorderEnabled(boolean enabled) {
        this.borderEnabled = enabled;
        // Update dropdown colors if open
        if (activeDropdown != null && activeDropdown.isVisible()) {
            activeDropdown = null; // Force recreate to apply new border state
        }
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