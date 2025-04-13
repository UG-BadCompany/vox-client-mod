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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VoxScreen extends Screen {
    private final VoxTheme theme = new VoxTheme();
    private final List<CategoryWindow> categoryWindows = new ArrayList<>();
    private SearchWindow searchWindow;
    private TextFieldWidget searchField;
    private int controlWidth = 100;
    private int controlHeight = 50;
    private int controlX;
    private int controlY = 10;
    private Identifier logoTexture = new Identifier("vox", "images/logo.png");
    private boolean draggingLogo = false;
    private int dragOffsetX;
    private int previousWidth = 0;
    private int previousHeight = 0;
    private VoxButton configButton;
    private int logoR = 255, logoG = 255, logoB = 255;
    private int searchR = 255, searchG = 255, searchB = 255;
    private final Map<String, int[]> categoryColors = new HashMap<>();

    public VoxScreen() {
        super(Text.literal("Vox Client"));
        updateControlPosition();
    }

    private void updateControlPosition() {
        controlX = (width - controlWidth) / 2;
    }

    @Override
    protected void init() {
        if (width != previousWidth || height != previousHeight) {
            previousWidth = width;
            previousHeight = height;
            updateControlPosition();
            categoryWindows.clear();
            categoryColors.clear();
        }

        searchField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, width - 130, controlY + controlHeight + 10, 120, 20, Text.literal("Search..."));
        searchField.setMaxLength(32);
        searchField.setEditable(true);
        searchField.setVisible(true);
        searchField.setFocusUnlocked(true);
        searchField.setChangedListener(this::updateSearch);
        addDrawableChild(searchField);
        searchWindow = new SearchWindow(theme, width - 130, controlY + controlHeight + 10, searchField);

        configButton = new VoxButton(width - 60, height - 30, theme, "Config");
        configButton.setMessage(Text.literal("Config"));
        addDrawableChild(configButton);

        if (categoryWindows.isEmpty()) {
            List<IVoxAddon> allAddons = AddonLoader.getAddons();
            String[] categories = {"Chat", "Combat", "Miscellaneous", "Movement", "Player", "Render", "World"};

            int windowsPerRow = (int) Math.ceil(width / 85.0);
            int totalRows = (int) Math.ceil((double) categories.length / windowsPerRow);
            int totalGridWidth = windowsPerRow * 85 - 5;
            int totalGridHeight = totalRows * 400 - 5;
            int windowX = (width - totalGridWidth) / 2;
            int windowY = (height - totalGridHeight) / 2;
            int row = 0, col = 0;

            int actualColumns = Math.min(categories.length, windowsPerRow);
            int actualGridWidth = actualColumns * 85 - 5;
            windowX = (width - actualGridWidth) / 2;

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

                CategoryWindow window = new CategoryWindow(theme, category, addons, windowX + col * 85, windowY + row * 400);
                categoryWindows.add(window);
                int[] colors = categoryColors.getOrDefault(category, new int[]{255, 255, 255});
                window.setColor(colors[0], colors[1], colors[2]);
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
            if (logoR != 255 || logoG != 255 || logoB != 255) {
                context.fill(controlX, controlY, controlX + controlWidth, controlY + controlHeight, (0xFF << 24) | (logoR << 16) | (logoG << 8) | logoB);
            }
            context.setShaderColor(logoR / 255.0f, logoG / 255.0f, logoB / 255.0f, 1.0f);
            context.drawTexture(logoTexture, controlX, controlY, 0, 0, controlWidth, controlHeight, controlWidth, controlHeight);
            context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        } catch (Exception e) {
            System.err.println("Failed to render logo texture: " + e.getMessage());
        }

        if (searchR != 255 || searchG != 255 || searchB != 255) {
            context.fill(searchField.getX() - 2, searchField.getY() - 2, searchField.getX() + searchField.getWidth() + 2, searchField.getY(), (0xFF << 24) | (searchR << 16) | (searchG << 8) | searchB);
            context.fill(searchField.getX() - 2, searchField.getY() + searchField.getHeight(), searchField.getX() + searchField.getWidth() + 2, searchField.getY() + searchField.getHeight() + 2, (0xFF << 24) | (searchR << 16) | (searchG << 8) | searchB);
            context.fill(searchField.getX() - 2, searchField.getY(), searchField.getX(), searchField.getY() + searchField.getHeight(), (0xFF << 24) | (searchR << 16) | (searchG << 8) | searchB);
            context.fill(searchField.getX() + searchField.getWidth(), searchField.getY(), searchField.getX() + searchField.getWidth() + 2, searchField.getY() + searchField.getHeight(), (0xFF << 24) | (searchR << 16) | (searchG << 8) | searchB);
        }
        searchField.render(context, mouseX, mouseY, delta);
        searchWindow.render(context, mouseX, mouseY, delta);
        for (CategoryWindow window : categoryWindows) {
            window.render(context, mouseX, mouseY, delta);
        }
        configButton.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        this.init();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (searchField.mouseClicked(mouseX, mouseY, button)) {
            setFocused(searchField);
            return true;
        }
        if (configButton.mouseClicked(mouseX, mouseY, button)) {
            MinecraftClient.getInstance().setScreen(new VoxConfigScreen(this, theme, new VoxConfigManager(theme)));
            return true;
        }
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

    public void updateLogoPosition(int x, int y) {
        this.controlX = MathHelper.clamp(x, 0, width - controlWidth);
        this.controlY = MathHelper.clamp(y, 0, height - controlHeight);
    }

    public void updateLogoSize(int width, int height) {
        this.controlWidth = Math.max(50, Math.min(500, width));
        this.controlHeight = Math.max(20, Math.min(600, height));
        updateControlPosition();
    }

    public void updateSearchPosition(int x, int y) {
        this.searchField.setX(MathHelper.clamp(x, 0, width - searchField.getWidth()));
        this.searchField.setY(MathHelper.clamp(y, 0, height - searchField.getHeight()));
        this.searchWindow = new SearchWindow(theme, searchField.getX(), searchField.getY(), searchField);
    }

    public void updateSearchSize(int width, int height) {
        this.searchField.setWidth(Math.max(20, Math.min(500, width)));
        this.searchField.setHeight(Math.max(20, Math.min(600, height)));
        this.searchWindow = new SearchWindow(theme, searchField.getX(), searchField.getY(), searchField);
    }

    public void updateCategoryPosition(String category, int x, int y) {
        for (CategoryWindow window : categoryWindows) {
            if (window.getCategory().equals(category)) {
                window.setPosition(x, y);
                break;
            }
        }
    }

    public void updateCategorySize(String category, int width, int height) {
        for (CategoryWindow window : categoryWindows) {
            if (window.getCategory().equals(category)) {
                window.setSize(Math.max(50, Math.min(500, width)), Math.max(50, Math.min(600, height)));
                break;
            }
        }
    }

    public void updateElementColor(String id, int r, int g, int b) {
        System.out.println("VoxScreen.updateElementColor called for id=" + id + " with RGB(" + r + "," + g + "," + b + ")");
        if (id.equals("logo")) {
            this.logoR = MathHelper.clamp(r, 0, 255);
            this.logoG = MathHelper.clamp(g, 0, 255);
            this.logoB = MathHelper.clamp(b, 0, 255);
        } else if (id.equals("search")) {
            this.searchR = MathHelper.clamp(r, 0, 255);
            this.searchG = MathHelper.clamp(g, 0, 255);
            this.searchB = MathHelper.clamp(b, 0, 255);
        } else {
            categoryColors.put(id, new int[]{MathHelper.clamp(r, 0, 255), MathHelper.clamp(g, 0, 255), MathHelper.clamp(b, 0, 255)});
            for (CategoryWindow window : categoryWindows) {
                if (window.getCategory().equals(id)) {
                    window.setColor(r, g, b);
                    break;
                }
            }
        }
    }

    public List<CategoryWindow> getCategoryWindows() {
        return categoryWindows;
    }

    public TextFieldWidget getSearchField() {
        return searchField;
    }

    public int getLogoX() {
        return controlX;
    }

    public int getLogoY() {
        return controlY;
    }

    public int getLogoWidth() {
        return controlWidth;
    }

    public int getLogoHeight() {
        return controlHeight;
    }
}