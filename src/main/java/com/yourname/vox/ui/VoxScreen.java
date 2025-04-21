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
import org.lwjgl.opengl.GL11;
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
    private int logoR = 255, logoG = 255, logoB = 255, logoA = 128; // Default to semi-transparent
    private int searchR = 255, searchG = 255, searchB = 255, searchA = 255;
    private final Map<String, int[]> categoryBodyColors = new HashMap<>();
    private final Map<String, int[]> categoryTitleColors = new HashMap<>();
    private final Map<String, int[]> categoryBorderColors = new HashMap<>();
    private final Map<String, Boolean> categoryBorderStates = new HashMap<>();

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
                            if (category.equals("Chat")) return List.of("Temp1").contains(a.getName());
                            if (category.equals("Combat")) return List.of("KillAura", "BowAimbot").contains(a.getName());
                            if (category.equals("Player")) return List.of("FireOverlayToggle", "AntiAFK", "AutoRespond").contains(a.getName());
                            if (category.equals("Render")) return List.of("ChunkLoaderESP", "StashFinder").contains(a.getName());
                            if (category.equals("Movement")) return List.of("Speed", "PhaseClip", "HighwayNav").contains(a.getName());
                            if (category.equals("World")) return List.of("ServerScan").contains(a.getName());
                            if (category.equals("Miscellaneous")) return List.of("AutoNav").contains(a.getName());
                            if (category.equals("Core")) return List.of("ClickGUI", "HUD").contains(a.getName());
                            return false;
                        })
                        .collect(Collectors.toList());

                CategoryWindow window = new CategoryWindow(theme, category, addons, windowX + col * 85, windowY + row * 400);
                categoryWindows.add(window);
                int[] bodyColors = categoryBodyColors.getOrDefault(category, new int[]{255, 255, 255});
                int[] titleColors = categoryTitleColors.getOrDefault(category, new int[]{255, 255, 255});
                int[] borderColors = categoryBorderColors.getOrDefault(category, new int[]{255, 255, 255});
                boolean borderState = categoryBorderStates.getOrDefault(category, false);
                window.setColor(bodyColors[0], bodyColors[1], bodyColors[2], titleColors[0], titleColors[1], titleColors[2]);
                window.setBorderColor(borderColors[0], borderColors[1], borderColors[2]);
                window.setBorderEnabled(borderState);
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
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            System.out.println("Rendering logo with alpha: " + logoA);
            context.setShaderColor(logoR / 255.0f, logoG / 255.0f, logoB / 255.0f, logoA / 255.0f);
            context.drawTexture(logoTexture, controlX, controlY, 0, 0, controlWidth, controlHeight, controlWidth, controlHeight);
            context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            context.fill(searchField.getX(), searchField.getY(), searchField.getX() + searchField.getWidth(), searchField.getY() + searchField.getHeight(),
                    (searchA << 24) | (searchR << 16) | (searchG << 8) | searchB);
            searchField.render(context, mouseX, mouseY, delta);
            searchWindow.render(context, mouseX, mouseY, delta);

            for (CategoryWindow window : categoryWindows) {
                window.render(context, mouseX, mouseY, delta);
            }

            configButton.render(context, mouseX, mouseY, delta);

            GL11.glDisable(GL11.GL_BLEND);
        } catch (Exception e) {
            System.err.println("Failed to render VoxScreen: " + e.getMessage());
        }
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        this.init();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        System.out.println("[Vox] VoxScreen mouseClicked: button=" + button + ", mouseX=" + mouseX + ", mouseY=" + mouseY);
        if (button == 0 || button == 1) {
            if (searchField.mouseClicked(mouseX, mouseY, button)) {
                setFocused(searchField);
                System.out.println("[Vox] SearchField clicked");
                return true;
            }
            if (configButton.mouseClicked(mouseX, mouseY, button)) {
                MinecraftClient.getInstance().setScreen(new VoxConfigScreen(this, theme, new VoxConfigManager(theme)));
                System.out.println("[Vox] ConfigButton clicked");
                return true;
            }
            if (button == 0 && mouseX >= controlX && mouseX <= controlX + controlWidth && mouseY >= controlY && mouseY <= controlY + controlHeight) {
                draggingLogo = true;
                dragOffsetX = (int) (mouseX - controlX);
                System.out.println("[Vox] Logo dragging started");
                return true;
            }
            if (searchWindow.mouseClicked(mouseX, mouseY, button)) {
                System.out.println("[Vox] SearchWindow handled click");
                return true;
            }
            for (int i = categoryWindows.size() - 1; i >= 0; i--) {
                CategoryWindow window = categoryWindows.get(i);
                if (mouseX >= window.getX() && mouseX <= window.getX() + window.getWidth() &&
                        mouseY >= window.getY() && mouseY <= window.getY() + window.getHeight()) {
                    System.out.println("[Vox] Click within CategoryWindow: category=" + window.getCategory() + ", button=" + button);
                    if (window.mouseClicked(mouseX, mouseY, button)) {
                        System.out.println("[Vox] CategoryWindow handled click: category=" + window.getCategory() + ", button=" + button);
                        categoryWindows.remove(i);
                        categoryWindows.add(window);
                        return true;
                    }
                }
            }
            System.out.println("[Vox] No actionable click detected at x=" + mouseX + ", y=" + mouseY);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        System.out.println("[Vox] VoxScreen mouseReleased: button=" + button + ", mouseX=" + mouseX + ", mouseY=" + mouseY);
        draggingLogo = false;
        searchWindow.mouseReleased(mouseX, mouseY, button);
        for (CategoryWindow window : categoryWindows) {
            window.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        System.out.println("[Vox] VoxScreen mouseDragged: button=" + button + ", mouseX=" + mouseX + ", mouseY=" + mouseY);
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
        System.out.println("[Vox] VoxScreen mouseScrolled: mouseX=" + mouseX + ", mouseY=" + mouseY + ", verticalAmount=" + verticalAmount);
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

    public void updateLogoColor(int r, int g, int b, int a) {
        this.logoR = MathHelper.clamp(r, 0, 255);
        this.logoG = MathHelper.clamp(g, 0, 255);
        this.logoB = MathHelper.clamp(b, 0, 255);
        this.logoA = MathHelper.clamp(a, 0, 255);
        System.out.println("Updated logo color: Red=" + logoR + ", Green=" + logoG + ", Blue=" + logoB + ", Alpha=" + logoA);
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

    public void updateSearchColor(int r, int g, int b, int a) {
        this.searchR = MathHelper.clamp(r, 0, 255);
        this.searchG = MathHelper.clamp(g, 0, 255);
        this.searchB = MathHelper.clamp(b, 0, 255);
        this.searchA = MathHelper.clamp(a, 0, 255);
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

    public void updateCategoryColor(String id, int bodyR, int bodyG, int bodyB, int titleR, int titleG, int titleB) {
        categoryBodyColors.put(id, new int[]{MathHelper.clamp(bodyR, 0, 255), MathHelper.clamp(bodyG, 0, 255), MathHelper.clamp(bodyB, 0, 255)});
        categoryTitleColors.put(id, new int[]{MathHelper.clamp(titleR, 0, 255), MathHelper.clamp(titleG, 0, 255), MathHelper.clamp(titleB, 0, 255)});
        for (CategoryWindow window : categoryWindows) {
            if (window.getCategory().equals(id)) {
                window.setColor(bodyR, bodyG, bodyB, titleR, titleG, titleB);
                break;
            }
        }
    }

    public void updateCategoryBorder(String id, int r, int g, int b, boolean enabled) {
        categoryBorderColors.put(id, new int[]{MathHelper.clamp(r, 0, 255), MathHelper.clamp(g, 0, 255), MathHelper.clamp(b, 0, 255)});
        categoryBorderStates.put(id, enabled);
        for (CategoryWindow window : categoryWindows) {
            if (window.getCategory().equals(id)) {
                window.setBorderColor(r, g, b);
                window.setBorderEnabled(enabled);
                break;
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

    public int[] getLogoColor() {
        return new int[]{logoR, logoG, logoB, logoA};
    }

    public int[] getSearchColor() {
        return new int[]{searchR, searchG, searchB, searchA};
    }

    public Map<String, int[]> getCategoryBodyColors() {
        return categoryBodyColors;
    }

    public Map<String, int[]> getCategoryTitleColors() {
        return categoryTitleColors;
    }

    public Map<String, int[]> getCategoryBorderColors() {
        return categoryBorderColors;
    }

    public Map<String, Boolean> getCategoryBorderStates() {
        return categoryBorderStates;
    }
}