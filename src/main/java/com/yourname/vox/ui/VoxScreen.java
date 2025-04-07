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
    private final int controlWidth = 100; // Adjusted for logo size
    private final int controlHeight = 50; // Adjusted for logo size
    private final int controlX; // Fixed position
    private final int controlY; // Fixed position
    private Identifier logoTexture;

    public VoxScreen() {
        super(Text.literal("Vox Client"));
        // Load the logo texture from assets
        logoTexture = new Identifier("vox", "images/logo.png");
        // Fixed position for the logo (top center)
        controlX = (width - controlWidth) / 2;
        controlY = 10; // Near the top of the screen
    }

    @Override
    protected void init() {
        // Search field as a separate window
        searchField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, 0, 0, 120, 20, Text.literal("Search..."));
        searchField.setMaxLength(32);
        searchField.setChangedListener(this::updateSearch);
        addDrawableChild(searchField);
        searchWindow = new SearchWindow(theme, (width - 130) / 2, controlY + controlHeight + 10, searchField);

        // Category windows
        categoryWindows.clear();
        List<IVoxAddon> allAddons = AddonLoader.getAddons();
        String[] categories = {"All", "Combat", "Utility", "Movement", "Render", "World", "Client"};

        // Calculate total grid size
        int windowsPerRow = (int) Math.ceil(width / 90.0); // 90 = window width (80) + spacing (10)
        int totalRows = (int) Math.ceil((double) categories.length / windowsPerRow);
        int totalGridWidth = windowsPerRow * 90 - 10; // Total width of the grid
        int totalGridHeight = totalRows * 310 - 10; // Total height of the grid

        // Center the grid on the screen, shifted left
        int windowX = (width - totalGridWidth) / 2 - 50; // Shift left by 50 pixels
        int windowY = (height - totalGridHeight) / 2;
        int row = 0;
        int col = 0;

        for (String category : categories) {
            List<IVoxAddon> addons;
            if (category.equals("All")) {
                addons = allAddons;
            } else {
                addons = allAddons.stream()
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

            categoryWindows.add(new CategoryWindow(theme, category, addons, windowX + col * 90, windowY + row * 310));
            col++;
            if (col >= windowsPerRow) {
                col = 0;
                row++;
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

        // Control window (logo only, fixed position)
        try {
            context.drawTexture(logoTexture, controlX, controlY, 0, 0, controlWidth, controlHeight, controlWidth, controlHeight);
        } catch (Exception e) {
            System.err.println("Failed to render logo texture: " + e.getMessage());
        }

        // Search window
        searchWindow.render(context, mouseX, mouseY, delta);

        // Category windows
        for (CategoryWindow window : categoryWindows) {
            window.render(context, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Clicking search window
        if (searchWindow.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // Clicking category windows
        for (CategoryWindow window : categoryWindows) {
            if (window.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        searchWindow.mouseReleased(mouseX, mouseY, button);
        for (CategoryWindow window : categoryWindows) {
            window.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        searchWindow.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        for (CategoryWindow window : categoryWindows) {
            window.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (CategoryWindow window : categoryWindows) {
            if (window.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}