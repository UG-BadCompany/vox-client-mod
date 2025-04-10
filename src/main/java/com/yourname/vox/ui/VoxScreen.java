package com.yourname.vox.ui;

import com.yourname.vox.AddonLoader;
import com.yourname.vox.IVoxAddon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VoxScreen extends Screen {
    private final VoxTheme theme = new VoxTheme();
    private final List<CategoryWindow> categoryWindows = new ArrayList<>();
    private SearchWindow searchWindow;
    private TextFieldWidget searchField;
    private final int controlWidth = 100;
    private final int controlHeight = 50;
    private int controlX;
    private final int controlY = 10;
    private Identifier logoTexture = new Identifier("vox", "images/logo.png");
    private boolean draggingLogo = false;
    private int dragOffsetX;
    private int previousWidth = 0;
    private int previousHeight = 0;

    public VoxScreen() {
        super(Text.literal("Vox Client"));
        updateControlPosition();
    }

    private void updateControlPosition() {
        // Center the logo horizontally
        controlX = (width - controlWidth) / 2;
    }

    @Override
    protected void init() {
        // Check if window size has changed
        if (width != previousWidth || height != previousHeight) {
            previousWidth = width;
            previousHeight = height;
            updateControlPosition();
            categoryWindows.clear(); // Clear and re-add windows to reposition them
        }

        // Position search bar on the right
        searchField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, width - 130, controlY + controlHeight + 10, 120, 20, Text.literal("Search..."));
        searchField.setMaxLength(32);
        searchField.setChangedListener(this::updateSearch);
        addDrawableChild(searchField);
        searchWindow = new SearchWindow(theme, width - 130, controlY + controlHeight + 10, searchField);

        if (categoryWindows.isEmpty()) {
            List<IVoxAddon> allAddons = AddonLoader.getAddons();
            String[] categories = {"Chat", "Combat", "Miscellaneous", "Movement", "Player", "Render", "World"};

            int windowsPerRow = (int) Math.ceil(width / 55.0); // 50px width + 5px spacing
            int totalRows = (int) Math.ceil((double) categories.length / windowsPerRow);
            int totalGridWidth = windowsPerRow * 55 - 5;
            int totalGridHeight = totalRows * 400 - 5;
            int windowX = (width - totalGridWidth) / 2; // Center the grid horizontally
            int windowY = (height - totalGridHeight) / 2; // Center the grid vertically
            int row = 0;
            int col = 0;

            // Adjust windowX to ensure the grid is centered even with fewer columns
            int actualColumns = Math.min(categories.length, windowsPerRow);
            int actualGridWidth = actualColumns * 55 - 5;
            windowX = (width - actualGridWidth) / 2; // Recalculate to center the actual grid width

            for (String category : categories) {
                List<IVoxAddon> addons = category.equals("All") ? allAddons : allAddons.stream()
                        .filter(a -> {
                            if (category.equals("Combat")) return List.of("KillAura", "BowAimbot").contains(a.getName());
                            if (category.equals("Player")) return List.of("AntiAFK", "AutoRespond").contains(a.getName());
                            if (category.equals("Visuals")) return List.of("ChunkLoaderESP", "StashFinder").contains(a.getName());
                            if (category.equals("Movement")) return List.of("Speed", "PhaseClip", "Teleport").contains(a.getName());
                            if (category.equals("Miscellaneous")) return List.of("ServerScan").contains(a.getName());
                            if (category.equals("Core")) return List.of("ClickGUI", "HUD").contains(a.getName());
                            return false;
                        })
                        .collect(Collectors.toList());

                categoryWindows.add(new CategoryWindow(theme, category, addons, windowX + col * 55, windowY + row * 400));
                col++;
                if (col >= windowsPerRow) {
                    col = 0;
                    row++;
                }
            }
        }
    }

    private void updateSearch(String query) {
        for (CategoryWindow window : categoryWindows) {
            window.filterButtons(query);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        try {
            context.drawTexture(logoTexture, controlX, controlY, 0, 0, controlWidth, controlHeight, controlWidth, controlHeight);
        } catch (Exception e) {
            System.err.println("Failed to render logo texture: " + e.getMessage());
        }

        searchWindow.render(context, mouseX, mouseY, delta);
        for (CategoryWindow window : categoryWindows) {
            window.render(context, mouseX, mouseY, delta);
        }
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        // Force re-initialization to adjust positions
        this.init();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (mouseX >= controlX && mouseX <= controlX + controlWidth && mouseY >= controlY && mouseY <= controlY + controlHeight) {
                draggingLogo = true;
                dragOffsetX = (int) (mouseX - controlX);
                return true;
            }
            if (searchWindow.mouseClicked(mouseX, mouseY, button)) return true;
            for (CategoryWindow window : categoryWindows) {
                if (window.mouseClicked(mouseX, mouseY, button)) return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingLogo = false;
        searchWindow.mouseReleased(mouseX, mouseY, button);
        for (CategoryWindow window : categoryWindows) {
            window.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingLogo) {
            controlX = (int) (mouseX - dragOffsetX);
            controlX = MathHelper.clamp(controlX, 0, width - controlWidth);
            return true;
        }
        searchWindow.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        for (CategoryWindow window : categoryWindows) {
            window.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (CategoryWindow window : categoryWindows) {
            if (window.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}