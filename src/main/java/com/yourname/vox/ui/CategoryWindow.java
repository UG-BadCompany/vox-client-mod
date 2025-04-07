package com.yourname.vox.ui;

import com.yourname.vox.AddonLoader;
import com.yourname.vox.ConfigManager;
import com.yourname.vox.IVoxAddon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CategoryWindow {
    private final VoxTheme theme;
    private final String category;
    private final List<VoxButton> buttons;
    private int x, y;
    private final int width = 80;
    private final int height = 300;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private boolean isDragging = false;
    private int dragOffsetX, dragOffsetY;
    private float dragSmoothX, dragSmoothY;
    private int[] buttonXs, buttonYs; // Cache button positions
    private boolean[] buttonVisible; // Cache visibility state
    private boolean[] buttonHovered; // Cache hover state
    private boolean[] buttonActive; // Cache active state

    public CategoryWindow(VoxTheme theme, String category, List<IVoxAddon> addons, int x, int y) {
        this.theme = theme;
        this.category = category;
        this.buttons = new ArrayList<>();
        this.x = x;
        this.y = y;
        this.dragSmoothX = x;
        this.dragSmoothY = y;

        for (IVoxAddon addon : addons) {
            buttons.add(new VoxButton(theme, addon));
        }

        updateButtons();
    }

    private void updateButtons() {
        int contentX = x + 5;
        int contentY = y + 30; // Increased space for category name
        int index = 0;
        buttonXs = new int[buttons.size()];
        buttonYs = new int[buttons.size()];
        buttonVisible = new boolean[buttons.size()];
        buttonHovered = new boolean[buttons.size()];
        buttonActive = new boolean[buttons.size()];
        for (VoxButton button : buttons) {
            buttonXs[index] = contentX;
            buttonYs[index] = contentY + index * (theme.buttonHeight + theme.spacing);
            button.x = buttonXs[index];
            button.y = buttonYs[index];
            button.setWidth(width - 10);
            buttonActive[index] = button.addon != null && ConfigManager.addonToggles.getOrDefault(button.addon.getName(), false);
            index++;
        }

        int contentHeight = buttons.size() * (theme.buttonHeight + theme.spacing) - theme.spacing;
        maxScroll = Math.max(0, contentHeight - (height - 35)); // Adjusted for category name
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Instant movement (no smoothing to eliminate "loggy" feeling)
        dragSmoothX = x;
        dragSmoothY = y;
        int renderX = (int) dragSmoothX;
        int renderY = (int) dragSmoothY;

        // Transparent background (no border, no rounded corners for performance)
        context.fill(renderX, renderY, renderX + width, renderY + height, 0x40000000);

        // Category name (no header background, distinct color, thicker shadow)
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, category, renderX + 5, renderY + 5, theme.activeTextColor);
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, category, renderX + 6, renderY + 6, 0xFF000000); // Thicker shadow

        // Buttons
        context.enableScissor(renderX + 5, renderY + 30, renderX + width - 5, renderY + height - 5);
        for (int i = 0; i < buttons.size(); i++) {
            VoxButton button = buttons.get(i);
            int buttonX = buttonXs[i];
            int buttonY = buttonYs[i] - scrollOffset;
            buttonVisible[i] = buttonY + theme.buttonHeight >= renderY + 30 && buttonY <= renderY + height - 5;
            if (buttonVisible[i]) {
                button.x = buttonX;
                button.y = buttonY;
                buttonHovered[i] = mouseX >= buttonX && mouseX <= buttonX + button.getWidth() && mouseY >= buttonY && mouseY <= buttonY + theme.buttonHeight;
                button.renderWidget(context, mouseX, mouseY, buttonY, delta, buttonHovered[i], buttonActive[i]);
            }
        }
        context.disableScissor();

        // Scrollbar
        if (maxScroll > 0) {
            int scrollBarX = renderX + width - theme.scrollBarWidth - 3;
            int scrollBarHeight = (int) (((float) (height - 35) / (height - 35 + maxScroll)) * (height - 35));
            int scrollBarY = renderY + 35 + (int) (((float) scrollOffset / maxScroll) * ((height - 35) - scrollBarHeight));
            theme.renderScrollBar(context, scrollBarX, scrollBarY, scrollBarHeight);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Dragging the window
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            isDragging = true;
            dragOffsetX = (int) (mouseX - x);
            dragOffsetY = (int) (mouseY - y);
            return true;
        }

        // Clicking buttons
        for (int i = 0; i < buttons.size(); i++) {
            if (!buttonVisible[i]) continue; // Skip invisible buttons
            VoxButton b = buttons.get(i);
            int buttonX = buttonXs[i];
            int buttonY = buttonYs[i] - scrollOffset;
            if (mouseX >= buttonX && mouseX <= buttonX + b.getWidth() && mouseY >= buttonY && mouseY <= buttonY + theme.buttonHeight) {
                b.onClick(mouseX, mouseY);
                return true;
            }
        }

        // Scrollbar
        if (maxScroll > 0 && button == 0) {
            int scrollBarX = x + width - theme.scrollBarWidth - 3;
            int scrollBarHeight = (int) (((float) (height - 35) / (height - 35 + maxScroll)) * (height - 35));
            int scrollBarY = y + 35 + (int) (((float) scrollOffset / maxScroll) * ((height - 35) - scrollBarHeight));
            if (mouseX >= scrollBarX && mouseX <= scrollBarX + theme.scrollBarWidth && mouseY >= scrollBarY && mouseY <= scrollBarY + scrollBarHeight) {
                isDragging = true;
                dragOffsetY = (int) (mouseY - scrollBarY);
                return true;
            }
        }

        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (isDragging) {
            isDragging = false;
            updateButtons(); // Update button positions only when dragging ends
        }
    }

    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDragging) {
            if (mouseY >= y && mouseY <= y + height) {
                x = (int) (mouseX - dragOffsetX);
                y = (int) (mouseY - dragOffsetY);
                // Update cached positions
                for (int i = 0; i < buttons.size(); i++) {
                    buttonXs[i] = x + 5;
                    buttonYs[i] = y + 30 + i * (theme.buttonHeight + theme.spacing);
                    buttons.get(i).x = buttonXs[i];
                    buttons.get(i).y = buttonYs[i];
                }
            } else {
                double scrollAreaHeight = (height - 35) - ((height - 35) / (height - 35 + maxScroll));
                double scrollPerPixel = (double) maxScroll / scrollAreaHeight;
                scrollOffset = MathHelper.clamp((int) ((mouseY - (y + 35) - dragOffsetY) * scrollPerPixel), 0, maxScroll);
            }
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll > 0 && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            scrollOffset = MathHelper.clamp(scrollOffset - (int) (verticalAmount * 20), 0, maxScroll);
            return true;
        }
        return false;
    }

    public void filterButtons(String searchQuery) {
        buttons.clear();
        List<IVoxAddon> addons = AddonLoader.getAddons();
        List<IVoxAddon> filteredAddons;
        if (category.equals("All")) {
            filteredAddons = addons;
        } else {
            filteredAddons = addons.stream()
                    .filter(a -> {
                        if (category.equals("Combat")) return List.of("KillAura", "BowAimbot").contains(a.getName());
                        if (category.equals("Utility")) return List.of("AntiAFK", "AutoRespond", "ServerScan").contains(a.getName());
                        if (category.equals("Movement")) return List.of("Speed", "PhaseClip", "Teleport").contains(a.getName());
                        if (category.equals("Render")) return List.of("ChunkLoaderESP", "StashFinder").contains(a.getName());
                        if (category.equals("World")) return List.of("AutoMine", "Nuker").contains(a.getName());
                        if (category.equals("Client")) return List.of("ClickGUI", "HUD").contains(a.getName());
                        return false;
                    })
                    .collect(Collectors.toList());
        }

        for (IVoxAddon addon : filteredAddons) {
            if (addon.getName().toLowerCase().contains(searchQuery.toLowerCase())) {
                buttons.add(new VoxButton(theme, addon));
            }
        }

        updateButtons();
    }
}