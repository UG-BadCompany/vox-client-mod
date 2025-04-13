package com.yourname.vox.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VoxConfigScreen extends Screen {
    private final VoxScreen parent;
    private final VoxTheme theme;
    private final VoxConfigManager configManager;
    private boolean editorMode = false;
    private List<UIElement> elements;
    private UIElement selectedElement = null;
    private int elementListOffset = 0;
    private int maxElementListOffset;
    private int propertiesOffset = 0;
    private int maxPropertiesOffset;
    private List<VoxButton> controlButtons;
    private List<SliderWidget> sliders;
    private List<VoxButton> propertyButtons;
    private List<WidgetPosition> sliderPositions;
    private List<WidgetPosition> buttonPositions;
    private Deque<EditAction> undoStack;
    private Deque<EditAction> redoStack;
    private boolean previewMode = false;
    private boolean dragging = false;
    private double dragOffsetX, dragOffsetY;
    private boolean showColorPicker = false;
    private boolean showCategories = true;
    private boolean showProperties = false;
    private boolean showLayers = false;
    private boolean snapToGrid = false;
    private Map<String, List<int[]>> colorPresets;
    private List<List<UIElement>> groups;
    private boolean draggingLayer = false;
    private int draggedLayerIndex = -1;
    private String helpHint = "";

    public VoxConfigScreen(VoxScreen parent, VoxTheme theme, VoxConfigManager configManager) {
        super(Text.literal("Vox UI Editor"));
        this.parent = parent;
        this.theme = theme;
        this.configManager = configManager;
        this.elements = new ArrayList<>();
        this.controlButtons = new ArrayList<>();
        this.sliders = new ArrayList<>();
        this.propertyButtons = new ArrayList<>();
        this.sliderPositions = new ArrayList<>();
        this.buttonPositions = new ArrayList<>();
        this.undoStack = new ArrayDeque<>();
        this.redoStack = new ArrayDeque<>();
        this.colorPresets = new HashMap<>();
        this.groups = new ArrayList<>();
    }

    private static class UIElement {
        String id;
        String type;
        double x, y, width, height;
        int r, g, b;
        float opacity;
        float fontSize;
        String textAlign;
        int spacing;
        boolean border;
        int borderThickness;
        int borderR, borderG, borderB;
        boolean visible;
        int zIndex;
        String easing;

        UIElement(String id, String type, double x, double y, double width, double height) {
            this.id = id;
            this.type = type;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.r = 255;
            this.g = 255;
            this.b = 255;
            this.opacity = 1.0f;
            this.fontSize = 1.0f;
            this.textAlign = "center";
            this.spacing = 2;
            this.border = false;
            this.borderThickness = 1;
            this.borderR = 255;
            this.borderG = 255;
            this.borderB = 255;
            this.visible = true;
            this.zIndex = 0;
            this.easing = "linear";
        }

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("id", id);
            json.addProperty("type", type);
            json.addProperty("x", x);
            json.addProperty("y", y);
            json.addProperty("width", width);
            json.addProperty("height", height);
            json.addProperty("r", r);
            json.addProperty("g", g);
            json.addProperty("b", b);
            json.addProperty("opacity", opacity);
            json.addProperty("fontSize", fontSize);
            json.addProperty("textAlign", textAlign);
            json.addProperty("spacing", spacing);
            json.addProperty("border", border);
            json.addProperty("borderThickness", borderThickness);
            json.addProperty("borderR", borderR);
            json.addProperty("borderG", borderG);
            json.addProperty("borderB", borderB);
            json.addProperty("visible", visible);
            json.addProperty("zIndex", zIndex);
            json.addProperty("easing", easing);
            return json;
        }

        void fromJson(JsonObject json) {
            x = json.get("x").getAsDouble();
            y = json.get("y").getAsDouble();
            width = json.get("width").getAsDouble();
            height = json.get("height").getAsDouble();
            r = json.get("r").getAsInt();
            g = json.get("g").getAsInt();
            b = json.get("b").getAsInt();
            opacity = json.get("opacity").getAsFloat();
            fontSize = json.get("fontSize").getAsFloat();
            textAlign = json.get("textAlign").getAsString();
            spacing = json.get("spacing").getAsInt();
            border = json.get("border").getAsBoolean();
            borderThickness = json.get("borderThickness").getAsInt();
            borderR = json.get("borderR").getAsInt();
            borderG = json.get("borderG").getAsInt();
            borderB = json.get("borderB").getAsInt();
            visible = json.get("visible").getAsBoolean();
            zIndex = json.get("zIndex").getAsInt();
            easing = json.has("easing") ? json.get("easing").getAsString() : "linear";
        }
    }

    private static class EditAction {
        UIElement element;
        JsonObject before;
        JsonObject after;

        EditAction(UIElement element, JsonObject before, JsonObject after) {
            this.element = element;
            this.before = before;
            this.after = after;
        }
    }

    private static class WidgetPosition {
        int x, y, width, height;

        WidgetPosition(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    @Override
    protected void init() {
        elements.clear();
        controlButtons.clear();
        sliders.clear();
        propertyButtons.clear();
        sliderPositions.clear();
        buttonPositions.clear();
        undoStack.clear();
        redoStack.clear();
        showColorPicker = false;
        showLayers = false;
        propertiesOffset = 0;
        if (selectedElement == null) {
            showCategories = true;
            showProperties = false;
        }
        helpHint = showCategories ? "Click element to edit, 'Layers' to reorder." : showLayers ? "Drag to reorder layers, click to return." : "Drag element, Shift + Arrows: Move 0.5px, Ctrl + G: Snap 10px.";

        elements.add(new UIElement("logo", "image", parent.getLogoX(), parent.getLogoY(), parent.getLogoWidth(), parent.getLogoHeight()));
        elements.add(new UIElement("search", "search", parent.getSearchField().getX(), parent.getSearchField().getY(), parent.getSearchField().getWidth(), parent.getSearchField().getHeight()));
        for (CategoryWindow window : parent.getCategoryWindows()) {
            elements.add(new UIElement(window.getCategory(), "window", window.getX(), window.getY(), window.getWidth(), window.getHeight()));
        }

        int buttonX = width - 150, buttonY = height - 150;
        controlButtons.add(new VoxButton(theme, buttonX, buttonY, 100, 20, Text.literal(editorMode ? "Normal Mode" : "Editor Mode"), btn -> {
            editorMode = !editorMode;
            btn.setMessage(Text.literal(editorMode ? "Normal Mode" : "Editor Mode"));
            selectedElement = null;
            showCategories = true;
            showProperties = false;
            showLayers = false;
            helpHint = "Click element to edit, 'Layers' to reorder.";
            init();
        }));
        buttonY += 25;
        controlButtons.add(new VoxButton(theme, buttonX, buttonY, 100, 20, Text.literal(previewMode ? "Edit Mode" : "Preview"), btn -> {
            previewMode = !previewMode;
            btn.setMessage(Text.literal(previewMode ? "Edit Mode" : "Preview"));
        }));
        buttonY += 25;
        controlButtons.add(new VoxButton(theme, buttonX, buttonY, 100, 20, Text.literal("Reset"), btn -> resetToDefaults()));
        buttonY += 25;
        controlButtons.add(new VoxButton(theme, buttonX, buttonY, 100, 20, Text.literal("Undo"), btn -> undo()));
        buttonY += 25;
        controlButtons.add(new VoxButton(theme, buttonX, buttonY, 100, 20, Text.literal("Redo"), btn -> redo()));
        buttonY += 25;
        for (int i = 1; i <= 5; i++) {
            int presetNum = i;
            controlButtons.add(new VoxButton(theme, buttonX - 50, buttonY, 50, 20, Text.literal("Save " + i), btn -> savePreset(presetNum)));
            controlButtons.add(new VoxButton(theme, buttonX, buttonY, 50, 20, Text.literal("Load " + i), btn -> loadPreset(presetNum)));
            buttonY += 25;
        }
        controlButtons.forEach(this::addDrawableChild);

        if (selectedElement != null && showProperties) {
            updatePropertiesPanel();
        }

        maxElementListOffset = Math.max(0, elements.size() * 20 - 180);
        maxPropertiesOffset = Math.max(0, 7 * 30 - 360); // Adjusted for simplified panel
    }

    private void updateVoxScreen() {
        if (selectedElement == null) {
            System.out.println("No element selected, skipping updateVoxScreen");
            return;
        }
        try {
            // Log properties for debugging
            System.out.println("Updating " + selectedElement.id + ": x=" + selectedElement.x + ", y=" + selectedElement.y + ", w=" + selectedElement.width + ", h=" + selectedElement.height);
            System.out.println("Color: R=" + selectedElement.r + ", G=" + selectedElement.g + ", B=" + selectedElement.b);

            // Update position and size
            if (selectedElement.id.equals("logo")) {
                parent.updateLogoPosition((int) selectedElement.x, (int) selectedElement.y);
                parent.updateLogoSize((int) selectedElement.width, (int) selectedElement.height);
            } else if (selectedElement.id.equals("search")) {
                parent.updateSearchPosition((int) selectedElement.x, (int) selectedElement.y);
                parent.updateSearchSize((int) selectedElement.width, (int) selectedElement.height);
            } else {
                parent.updateCategoryPosition(selectedElement.id, (int) selectedElement.x, (int) selectedElement.y);
                parent.updateCategorySize(selectedElement.id, (int) selectedElement.width, (int) selectedElement.height);
            }

            // Update color
            parent.updateElementColor(selectedElement.id, selectedElement.r, selectedElement.g, selectedElement.b);

            // Force render refresh
            MinecraftClient.getInstance().execute(() -> {
                System.out.println("Executing render refresh for " + selectedElement.id);
                try {
                    parent.render(new DrawContext(MinecraftClient.getInstance(), MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers()), 0, 0, 0);
                } catch (Exception e) {
                    System.err.println("Render refresh failed for " + selectedElement.id + ": " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Error in updateVoxScreen for " + selectedElement.id + ": " + e.getMessage());
        }
    }

    private void applyEdit(String property, Object oldValue, Object newValue) {
        if (!oldValue.equals(newValue) && selectedElement != null) {
            JsonObject before = selectedElement.toJson();
            JsonObject after = selectedElement.toJson();
            undoStack.push(new EditAction(selectedElement, before, after));
            redoStack.clear();
        }
    }

    private void updatePropertiesPanel() {
        sliders.clear();
        propertyButtons.clear();
        sliderPositions.clear();
        buttonPositions.clear();
        if (selectedElement == null) return;

        int sliderX = 10, sliderY = 60, buttonX = 100;
        helpHint = "Drag element, Shift + Arrows: Move 0.5px, Ctrl + G: Snap 10px.";
        VoxButton backButton = new VoxButton(theme, sliderX, sliderY, 70, 18, Text.literal("Back"), btn -> {
            showProperties = false;
            showCategories = true;
            selectedElement = null;
            helpHint = "Click element to edit, 'Layers' to reorder.";
            init();
        });
        propertyButtons.add(backButton);
        buttonPositions.add(new WidgetPosition(sliderX, sliderY, 70, 18));
        sliderY += 30;
        SliderWidget xSlider = new SliderWidget(theme, sliderX, sliderY, 70, 18, 0, width, selectedElement.x, v -> {
            double oldX = selectedElement.x;
            selectedElement.x = snapToGrid ? Math.round(v / 10.0) * 10 : v;
            System.out.println("Slider set x=" + selectedElement.x + " for " + selectedElement.id + " (was " + oldX + ")");
            applyEdit("x", oldX, selectedElement.x);
            updateVoxScreen();
        });
        sliders.add(xSlider);
        sliderPositions.add(new WidgetPosition(sliderX, sliderY, 70, 18));
        VoxButton xMinus = new VoxButton(theme, buttonX, sliderY, 18, 18, Text.literal("-0.5"), btn -> {
            double oldX = selectedElement.x;
            selectedElement.x = Math.max(0, selectedElement.x - 0.5);
            System.out.println("Button set x=" + selectedElement.x + " for " + selectedElement.id + " (was " + oldX + ")");
            applyEdit("x", oldX, selectedElement.x);
            updateVoxScreen();
        });
        VoxButton xPlus = new VoxButton(theme, buttonX + 20, sliderY, 18, 18, Text.literal("+0.5"), btn -> {
            double oldX = selectedElement.x;
            selectedElement.x = Math.min(width - selectedElement.width, selectedElement.x + 0.5);
            System.out.println("Button set x=" + selectedElement.x + " for " + selectedElement.id + " (was " + oldX + ")");
            applyEdit("x", oldX, selectedElement.x);
            updateVoxScreen();
        });
        propertyButtons.add(xMinus);
        propertyButtons.add(xPlus);
        buttonPositions.add(new WidgetPosition(buttonX, sliderY, 18, 18));
        buttonPositions.add(new WidgetPosition(buttonX + 20, sliderY, 18, 18));
        sliderY += 30;
        SliderWidget ySlider = new SliderWidget(theme, sliderX, sliderY, 70, 18, 0, height, selectedElement.y, v -> {
            double oldY = selectedElement.y;
            selectedElement.y = snapToGrid ? Math.round(v / 10.0) * 10 : v;
            System.out.println("Slider set y=" + selectedElement.y + " for " + selectedElement.id + " (was " + oldY + ")");
            applyEdit("y", oldY, selectedElement.y);
            updateVoxScreen();
        });
        sliders.add(ySlider);
        sliderPositions.add(new WidgetPosition(sliderX, sliderY, 70, 18));
        VoxButton yMinus = new VoxButton(theme, buttonX, sliderY, 18, 18, Text.literal("-0.5"), btn -> {
            double oldY = selectedElement.y;
            selectedElement.y = Math.max(0, selectedElement.y - 0.5);
            System.out.println("Button set y=" + selectedElement.y + " for " + selectedElement.id + " (was " + oldY + ")");
            applyEdit("y", oldY, selectedElement.y);
            updateVoxScreen();
        });
        VoxButton yPlus = new VoxButton(theme, buttonX + 20, sliderY, 18, 18, Text.literal("+0.5"), btn -> {
            double oldY = selectedElement.y;
            selectedElement.y = Math.min(height - selectedElement.height, selectedElement.y + 0.5);
            System.out.println("Button set y=" + selectedElement.y + " for " + selectedElement.id + " (was " + oldY + ")");
            applyEdit("y", oldY, selectedElement.y);
            updateVoxScreen();
        });
        propertyButtons.add(yMinus);
        propertyButtons.add(yPlus);
        buttonPositions.add(new WidgetPosition(buttonX, sliderY, 18, 18));
        buttonPositions.add(new WidgetPosition(buttonX + 20, sliderY, 18, 18));
        sliderY += 30;
        SliderWidget wSlider = new SliderWidget(theme, sliderX, sliderY, 70, 18, selectedElement.type.equals("window") ? 50 : 20, 500, selectedElement.width, v -> {
            double oldWidth = selectedElement.width;
            selectedElement.width = snapToGrid ? Math.round(v / 10.0) * 10 : v;
            System.out.println("Slider set width=" + selectedElement.width + " for " + selectedElement.id + " (was " + oldWidth + ")");
            applyEdit("width", oldWidth, selectedElement.width);
            updateVoxScreen();
        });
        sliders.add(wSlider);
        sliderPositions.add(new WidgetPosition(sliderX, sliderY, 70, 18));
        VoxButton wMinus = new VoxButton(theme, buttonX, sliderY, 18, 18, Text.literal("-0.5"), btn -> {
            double oldWidth = selectedElement.width;
            selectedElement.width = Math.max(selectedElement.type.equals("window") ? 50 : 20, selectedElement.width - 0.5);
            System.out.println("Button set width=" + selectedElement.width + " for " + selectedElement.id + " (was " + oldWidth + ")");
            applyEdit("width", oldWidth, selectedElement.width);
            updateVoxScreen();
        });
        VoxButton wPlus = new VoxButton(theme, buttonX + 20, sliderY, 18, 18, Text.literal("+0.5"), btn -> {
            double oldWidth = selectedElement.width;
            selectedElement.width = Math.min(500, selectedElement.width + 0.5);
            System.out.println("Button set width=" + selectedElement.width + " for " + selectedElement.id + " (was " + oldWidth + ")");
            applyEdit("width", oldWidth, selectedElement.width);
            updateVoxScreen();
        });
        propertyButtons.add(wMinus);
        propertyButtons.add(wPlus);
        buttonPositions.add(new WidgetPosition(buttonX, sliderY, 18, 18));
        buttonPositions.add(new WidgetPosition(buttonX + 20, sliderY, 18, 18));
        sliderY += 30;
        SliderWidget hSlider = new SliderWidget(theme, sliderX, sliderY, 70, 18, selectedElement.type.equals("window") ? 50 : 20, 600, selectedElement.height, v -> {
            double oldHeight = selectedElement.height;
            selectedElement.height = snapToGrid ? Math.round(v / 10.0) * 10 : v;
            System.out.println("Slider set height=" + selectedElement.height + " for " + selectedElement.id + " (was " + oldHeight + ")");
            applyEdit("height", oldHeight, selectedElement.height);
            updateVoxScreen();
        });
        sliders.add(hSlider);
        sliderPositions.add(new WidgetPosition(sliderX, sliderY, 70, 18));
        VoxButton hMinus = new VoxButton(theme, buttonX, sliderY, 18, 18, Text.literal("-0.5"), btn -> {
            double oldHeight = selectedElement.height;
            selectedElement.height = Math.max(selectedElement.type.equals("window") ? 50 : 20, selectedElement.height - 0.5);
            System.out.println("Button set height=" + selectedElement.height + " for " + selectedElement.id + " (was " + oldHeight + ")");
            applyEdit("height", oldHeight, selectedElement.height);
            updateVoxScreen();
        });
        VoxButton hPlus = new VoxButton(theme, buttonX + 20, sliderY, 18, 18, Text.literal("+0.5"), btn -> {
            double oldHeight = selectedElement.height;
            selectedElement.height = Math.min(600, selectedElement.height + 0.5);
            System.out.println("Button set height=" + selectedElement.height + " for " + selectedElement.id + " (was " + oldHeight + ")");
            applyEdit("height", oldHeight, selectedElement.height);
            updateVoxScreen();
        });
        propertyButtons.add(hMinus);
        propertyButtons.add(hPlus);
        buttonPositions.add(new WidgetPosition(buttonX, sliderY, 18, 18));
        buttonPositions.add(new WidgetPosition(buttonX + 20, sliderY, 18, 18));
        sliderY += 30;
        VoxButton pickColorButton = new VoxButton(theme, sliderX, sliderY, 70, 18, Text.literal("Pick Col"), btn -> {
            showColorPicker = !showColorPicker;
            updatePropertiesPanel();
        });
        propertyButtons.add(pickColorButton);
        buttonPositions.add(new WidgetPosition(sliderX, sliderY, 70, 18));
        VoxButton saveColorButton = new VoxButton(theme, buttonX, sliderY, 38, 18, Text.literal("Save Col"), btn -> {
            List<int[]> presets = colorPresets.computeIfAbsent(selectedElement.id, k -> new ArrayList<>());
            if (presets.size() < 5) {
                presets.add(new int[]{selectedElement.r, selectedElement.g, selectedElement.b});
                System.out.println("Saved color preset for " + selectedElement.id + ": R=" + selectedElement.r + ", G=" + selectedElement.g + ", B=" + selectedElement.b);
            }
        });
        propertyButtons.add(saveColorButton);
        buttonPositions.add(new WidgetPosition(buttonX, sliderY, 38, 18));
        if (showColorPicker) {
            sliderY += 30;
            SliderWidget rSlider = new SliderWidget(theme, sliderX, sliderY, 70, 18, 0, 255, selectedElement.r, v -> {
                int oldR = selectedElement.r;
                selectedElement.r = v.intValue();
                System.out.println("Slider set R=" + selectedElement.r + " for " + selectedElement.id + " (was " + oldR + ")");
                applyEdit("r", oldR, selectedElement.r);
                updateVoxScreen();
            });
            sliders.add(rSlider);
            sliderPositions.add(new WidgetPosition(sliderX, sliderY, 70, 18));
            sliderY += 20;
            SliderWidget gSlider = new SliderWidget(theme, sliderX, sliderY, 70, 18, 0, 255, selectedElement.g, v -> {
                int oldG = selectedElement.g;
                selectedElement.g = v.intValue();
                System.out.println("Slider set G=" + selectedElement.g + " for " + selectedElement.id + " (was " + oldG + ")");
                applyEdit("g", oldG, selectedElement.g);
                updateVoxScreen();
            });
            sliders.add(gSlider);
            sliderPositions.add(new WidgetPosition(sliderX, sliderY, 70, 18));
            sliderY += 20;
            SliderWidget bSlider = new SliderWidget(theme, sliderX, sliderY, 70, 18, 0, 255, selectedElement.b, v -> {
                int oldB = selectedElement.b;
                selectedElement.b = v.intValue();
                System.out.println("Slider set B=" + selectedElement.b + " for " + selectedElement.id + " (was " + oldB + ")");
                applyEdit("b", oldB, selectedElement.b);
                updateVoxScreen();
            });
            sliders.add(bSlider);
            sliderPositions.add(new WidgetPosition(sliderX, sliderY, 70, 18));
            sliderY += 20;
        } else {
            sliderY += 30;
        }
        for (int i = 0; i < 5; i++) {
            int presetIndex = i;
            VoxButton presetButton = new VoxButton(theme, sliderX + i * 22, sliderY, 18, 18, Text.literal(String.valueOf(i + 1)), btn -> {
                List<int[]> presets = colorPresets.getOrDefault(selectedElement.id, new ArrayList<>());
                if (presetIndex < presets.size()) {
                    int[] preset = presets.get(presetIndex);
                    int oldR = selectedElement.r, oldG = selectedElement.g, oldB = selectedElement.b;
                    selectedElement.r = preset[0];
                    selectedElement.g = preset[1];
                    selectedElement.b = preset[2];
                    System.out.println("Loaded preset for " + selectedElement.id + ": R=" + selectedElement.r + ", G=" + selectedElement.g + ", B=" + selectedElement.b);
                    applyEdit("color", oldR + "," + oldG + "," + oldB, selectedElement.r + "," + selectedElement.g + "," + selectedElement.b);
                    updateVoxScreen();
                }
            });
            propertyButtons.add(presetButton);
            buttonPositions.add(new WidgetPosition(sliderX + i * 22, sliderY, 18, 18));
        }
        sliderY += 30;
        VoxButton snapButton = new VoxButton(theme, sliderX, sliderY, 70, 18, Text.literal("Snap: " + (snapToGrid ? "On" : "Off")), btn -> {
            snapToGrid = !snapToGrid;
            btn.setMessage(Text.literal("Snap: " + (snapToGrid ? "On" : "Off")));
            System.out.println("Button set snapToGrid=" + snapToGrid + " for " + selectedElement.id);
        });
        propertyButtons.add(snapButton);
        buttonPositions.add(new WidgetPosition(sliderX, sliderY, 70, 18));

        sliders.forEach(this::addDrawableChild);
        propertyButtons.forEach(this::addDrawableChild);
    }

    private void resetToDefaults() {
        JsonObject before = new JsonObject();
        elements.forEach(e -> before.add(e.id, e.toJson()));
        elements.clear();
        elements.add(new UIElement("logo", "image", width / 2 - 50, 10, 100, 50));
        elements.add(new UIElement("search", "search", width - 130, 70, 120, 20));
        String[] categories = {"Chat", "Combat", "Miscellaneous", "Movement", "Player", "Render", "World"};
        int windowsPerRow = (int) Math.ceil(width / 85.0);
        int windowX = (width - (Math.min(categories.length, windowsPerRow) * 85 - 5)) / 2;
        int windowY = (height - ((int) Math.ceil((double) categories.length / windowsPerRow) * 400 - 5)) / 2;
        int row = 0, col = 0;
        for (String category : categories) {
            elements.add(new UIElement(category, "window", windowX + col * 85, windowY + row * 400, 80, 400));
            col++;
            if (col >= windowsPerRow) {
                col = 0;
                row++;
            }
        }
        JsonObject after = new JsonObject();
        elements.forEach(e -> after.add(e.id, e.toJson()));
        undoStack.push(new EditAction(null, before, after));
        redoStack.clear();
        selectedElement = null;
        showProperties = false;
        showCategories = true;
        showLayers = false;
        helpHint = "Click element to edit, 'Layers' to reorder.";
        init();
        parent.updateLogoPosition(width / 2 - 50, 10);
        parent.updateLogoSize(100, 50);
        parent.updateSearchPosition(width - 130, 70);
        parent.updateSearchSize(120, 20);
        col = 0;
        row = 0;
        for (String category : categories) {
            parent.updateCategoryPosition(category, windowX + col * 85, windowY + row * 400);
            parent.updateCategorySize(category, 80, 400);
            col++;
            if (col >= windowsPerRow) {
                col = 0;
                row++;
            }
        }
    }

    private void undo() {
        if (!undoStack.isEmpty()) {
            EditAction action = undoStack.pop();
            if (action.element != null) {
                action.element.fromJson(action.before);
            } else {
                JsonObject before = action.before;
                elements.clear();
                before.entrySet().forEach(entry -> {
                    UIElement e = new UIElement(entry.getKey(), entry.getValue().getAsJsonObject().get("type").getAsString(), 0, 0, 0, 0);
                    e.fromJson(entry.getValue().getAsJsonObject());
                    elements.add(e);
                });
            }
            redoStack.push(action);
            init();
            updateVoxScreen();
        }
    }

    private void redo() {
        if (!redoStack.isEmpty()) {
            EditAction action = redoStack.pop();
            if (action.element != null) {
                action.element.fromJson(action.after);
            } else {
                JsonObject after = action.after;
                elements.clear();
                after.entrySet().forEach(entry -> {
                    UIElement e = new UIElement(entry.getKey(), entry.getValue().getAsJsonObject().get("type").getAsString(), 0, 0, 0, 0);
                    e.fromJson(entry.getValue().getAsJsonObject());
                    elements.add(e);
                });
            }
            undoStack.push(action);
            init();
            updateVoxScreen();
        }
    }

    private void savePreset(int presetNum) {
        try {
            File dir = new File("config/vox");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "preset_" + presetNum + ".json");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject json = new JsonObject();
            for (UIElement element : elements) {
                json.add(element.id, element.toJson());
            }
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(json, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPreset(int presetNum) {
        try {
            File file = new File("config/vox/preset_" + presetNum + ".json");
            if (!file.exists()) return;
            Gson gson = new Gson();
            JsonObject before = new JsonObject();
            elements.forEach(e -> before.add(e.id, e.toJson()));
            try (FileReader reader = new FileReader(file)) {
                JsonObject json = gson.fromJson(reader, JsonObject.class);
                elements.forEach(e -> {
                    if (json.has(e.id)) {
                        e.fromJson(json.getAsJsonObject(e.id));
                    }
                });
            }
            JsonObject after = new JsonObject();
            elements.forEach(e -> after.add(e.id, e.toJson()));
            undoStack.push(new EditAction(null, before, after));
            redoStack.clear();
            init();
            updateVoxScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        parent.render(context, mouseX, mouseY, delta);

        if (editorMode && !previewMode) {
            if (showCategories) {
                int listY = 30;
                context.fill(10, listY, 150, listY + 200, 0xC0333333);
                context.enableScissor(10, listY, 150, listY + 200);
                for (int i = 0; i < elements.size(); i++) {
                    UIElement element = elements.get(i);
                    int itemY = listY + 10 + i * 20 - elementListOffset;
                    if (itemY >= listY && itemY <= listY + 180) {
                        int textColor = theme.getTextColor();
                        if (mouseX >= 10 && mouseX <= 150 && mouseY >= itemY && mouseY <= itemY + 20) {
                            textColor = theme.getTabActive();
                            context.fill(10, itemY, 150, itemY + 20, theme.getButtonHover());
                        }
                        context.drawTextWithShadow(textRenderer, element.id, 20, itemY, textColor);
                    }
                }
                context.disableScissor();
                context.drawTextWithShadow(textRenderer, "Layers", 20, listY + 205, theme.getTextColor());
            }

            if (showLayers) {
                int listY = 30;
                context.fill(10, listY, 150, listY + 200, 0xC0333333);
                context.enableScissor(10, listY, 150, listY + 200);
                for (int i = 0; i < elements.size(); i++) {
                    UIElement element = elements.get(i);
                    int itemY = listY + 10 + i * 20 - elementListOffset;
                    if (itemY >= listY && itemY <= listY + 180) {
                        int textColor = theme.getTextColor();
                        if (mouseX >= 10 && mouseX <= 150 && mouseY >= itemY && mouseY <= itemY + 20) {
                            textColor = theme.getTabActive();
                            context.fill(10, itemY, 150, itemY + 20, theme.getButtonHover());
                        }
                        context.drawTextWithShadow(textRenderer, element.id + " (z:" + element.zIndex + ")", 20, itemY, textColor);
                    }
                }
                context.disableScissor();
            }

            if (showProperties && selectedElement != null) {
                context.fill(10, 30, 150, 430, 0xC0333333);
                context.enableScissor(10, 30, 150, 430);
                context.getMatrices().push();
                context.getMatrices().translate(0, -propertiesOffset, 0);
                int sliderY = 85;
                context.drawTextWithShadow(textRenderer, "Properties: " + selectedElement.id, 20, 40, theme.getTextColor());
                context.drawTextWithShadow(textRenderer, "X", 20, sliderY - 3, theme.getTextColor());
                sliderY += 30;
                context.drawTextWithShadow(textRenderer, "Y", 20, sliderY - 3, theme.getTextColor());
                sliderY += 30;
                context.drawTextWithShadow(textRenderer, "W", 20, sliderY - 3, theme.getTextColor());
                sliderY += 30;
                context.drawTextWithShadow(textRenderer, "H", 20, sliderY - 3, theme.getTextColor());
                sliderY += 30;
                context.drawTextWithShadow(textRenderer, "Col", 20, sliderY - 3, theme.getTextColor());
                if (showColorPicker) {
                    sliderY += 30;
                    context.drawTextWithShadow(textRenderer, "R", 20, sliderY - 3, theme.getTextColor());
                    sliderY += 20;
                    context.drawTextWithShadow(textRenderer, "G", 20, sliderY - 3, theme.getTextColor());
                    sliderY += 20;
                    context.drawTextWithShadow(textRenderer, "B", 20, sliderY - 3, theme.getTextColor());
                    sliderY += 20;
                } else {
                    sliderY += 30;
                }
                context.drawTextWithShadow(textRenderer, "Presets", 20, sliderY - 3, theme.getTextColor());
                sliderY += 30;
                context.drawTextWithShadow(textRenderer, "Snap", 20, sliderY - 3, theme.getTextColor());
                for (int i = 0; i < sliders.size(); i++) {
                    SliderWidget slider = sliders.get(i);
                    WidgetPosition pos = sliderPositions.get(i);
                    int renderY = pos.y;
                    if (renderY >= 30 + propertiesOffset && renderY <= 430 + propertiesOffset - pos.height) {
                        slider.setY(renderY);
                        slider.render(context, mouseX, mouseY, delta);
                    }
                }
                for (int i = 0; i < propertyButtons.size(); i++) {
                    VoxButton button = propertyButtons.get(i);
                    WidgetPosition pos = buttonPositions.get(i);
                    int renderY = pos.y;
                    if (renderY >= 30 + propertiesOffset && renderY <= 430 + propertiesOffset - pos.height) {
                        button.setY(renderY);
                        button.render(context, mouseX, mouseY, delta);
                    }
                }
                context.getMatrices().pop();
                context.disableScissor();
                // Scrollbar
                int barHeight = (int) (400 * (400.0 / (7 * 30)));
                int barY = 30 + (int) ((propertiesOffset / (float) maxPropertiesOffset) * (400 - barHeight));
                context.fill(145, barY, 150, barY + barHeight, 0xFF888888);
            }

            context.drawCenteredTextWithShadow(textRenderer, helpHint, width / 2, height - 20, theme.getTextColor());
        }

        for (VoxButton button : controlButtons) {
            button.render(context, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (editorMode && !previewMode) {
            for (VoxButton btn : controlButtons) {
                if (btn.mouseClicked(mouseX, mouseY, button)) return true;
            }

            if (showProperties) {
                double adjustedMouseY = mouseY + propertiesOffset;
                for (int i = 0; i < propertyButtons.size(); i++) {
                    VoxButton btn = propertyButtons.get(i);
                    WidgetPosition pos = buttonPositions.get(i);
                    if (mouseX >= pos.x && mouseX <= pos.x + pos.width &&
                            adjustedMouseY >= pos.y && adjustedMouseY <= pos.y + pos.height) {
                        if (btn.mouseClicked(mouseX, adjustedMouseY, button)) return true;
                    }
                }
                for (int i = 0; i < sliders.size(); i++) {
                    SliderWidget slider = sliders.get(i);
                    WidgetPosition pos = sliderPositions.get(i);
                    if (mouseX >= pos.x && mouseX <= pos.x + pos.width &&
                            adjustedMouseY >= pos.y && adjustedMouseY <= pos.y + pos.height) {
                        if (slider.mouseClicked(mouseX, adjustedMouseY, button)) return true;
                    }
                }
            }

            if (showCategories) {
                if (mouseX >= 10 && mouseX <= 150 && mouseY >= 235 && mouseY <= 245) {
                    showCategories = false;
                    showLayers = true;
                    helpHint = "Drag to reorder layers, click to return.";
                    init();
                    return true;
                }
                if (mouseX >= 10 && mouseX <= 150 && mouseY >= 30 && mouseY <= 230) {
                    int listY = 30;
                    int index = (int) ((mouseY - listY - 10 + elementListOffset) / 20);
                    if (index >= 0 && index < elements.size()) {
                        selectedElement = elements.get(index);
                        showCategories = false;
                        showProperties = true;
                        showColorPicker = false;
                        helpHint = "Drag element, Shift + Arrows: Move 0.5px, Ctrl + G: Snap 10px.";
                        updatePropertiesPanel();
                        return true;
                    }
                }
            }

            if (showLayers) {
                if (mouseX >= 10 && mouseX <= 150 && mouseY >= 30 && mouseY <= 230) {
                    int listY = 30;
                    int index = (int) ((mouseY - listY - 10 + elementListOffset) / 20);
                    if (index >= 0 && index < elements.size()) {
                        draggingLayer = true;
                        draggedLayerIndex = index;
                        return true;
                    }
                }
            }

            for (UIElement element : elements) {
                if (element.visible && mouseX >= element.x && mouseX <= element.x + element.width && mouseY >= element.y && mouseY <= element.y + element.height) {
                    List<UIElement> group = groups.stream().filter(g -> g.contains(element)).findFirst().orElse(null);
                    if (group != null) {
                        for (UIElement groupedElement : group) {
                            groupedElement.x += mouseX - dragOffsetX - element.x;
                            groupedElement.y += mouseY - dragOffsetY - element.y;
                        }
                    }
                    selectedElement = element;
                    dragging = true;
                    dragOffsetX = mouseX - element.x;
                    dragOffsetY = mouseY - element.y;
                    showCategories = false;
                    showProperties = true;
                    showColorPicker = false;
                    helpHint = "Drag element, Shift + Arrows: Move 0.5px, Ctrl + G: Snap 10px.";
                    updatePropertiesPanel();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (editorMode && dragging && selectedElement != null) {
            double oldX = selectedElement.x, oldY = selectedElement.y;
            double newX = mouseX - dragOffsetX;
            double newY = mouseY - dragOffsetY;
            if (snapToGrid) {
                newX = Math.round(newX / 10.0) * 10;
                newY = Math.round(newY / 10.0) * 10;
            }
            selectedElement.x = MathHelper.clamp(newX, 0, width - selectedElement.width);
            selectedElement.y = MathHelper.clamp(newY, 0, height - selectedElement.height);
            List<UIElement> group = groups.stream().filter(g -> g.contains(selectedElement)).findFirst().orElse(null);
            if (group != null) {
                double dx = selectedElement.x - (mouseX - dragOffsetX);
                double dy = selectedElement.y - (mouseY - dragOffsetY);
                for (UIElement groupedElement : group) {
                    if (groupedElement != selectedElement) {
                        groupedElement.x += dx;
                        groupedElement.y += dy;
                    }
                }
            }
            System.out.println("Dragged " + selectedElement.id + " to x=" + selectedElement.x + ", y=" + selectedElement.y);
            applyEdit("x", oldX, selectedElement.x);
            applyEdit("y", oldY, selectedElement.y);
            updateVoxScreen();
            updatePropertiesPanel();
            return true;
        }
        if (editorMode && draggingLayer && draggedLayerIndex >= 0) {
            int listY = 30;
            int newIndex = (int) ((mouseY - listY - 10 + elementListOffset) / 20);
            newIndex = MathHelper.clamp(newIndex, 0, elements.size() - 1);
            if (newIndex != draggedLayerIndex) {
                UIElement movedElement = elements.remove(draggedLayerIndex);
                elements.add(newIndex, movedElement);
                for (int i = 0; i < elements.size(); i++) {
                    elements.get(i).zIndex = elements.size() - i;
                }
                draggedLayerIndex = newIndex;
                init();
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        draggingLayer = false;
        draggedLayerIndex = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (editorMode && !previewMode) {
            if (showCategories && mouseX >= 10 && mouseX <= 150 && mouseY >= 30 && mouseY <= 230) {
                elementListOffset = MathHelper.clamp(elementListOffset - (int) (verticalAmount * 20), 0, maxElementListOffset);
                return true;
            }
            if (showLayers && mouseX >= 10 && mouseX <= 150 && mouseY >= 30 && mouseY <= 230) {
                elementListOffset = MathHelper.clamp(elementListOffset - (int) (verticalAmount * 20), 0, maxElementListOffset);
                return true;
            }
            if (showProperties && mouseX >= 10 && mouseX <= 150 && mouseY >= 30 && mouseY <= 430) {
                propertiesOffset = MathHelper.clamp(propertiesOffset - (int) (verticalAmount * 20), 0, maxPropertiesOffset);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editorMode && selectedElement != null) {
            boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
            boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                double oldX = selectedElement.x;
                double newX = selectedElement.x - (shift ? 0.5 : 1);
                if (snapToGrid) newX = Math.round(newX / 10.0) * 10;
                selectedElement.x = Math.max(0, newX);
                System.out.println("Key set x=" + selectedElement.x + " for " + selectedElement.id);
                applyEdit("x", oldX, selectedElement.x);
                updateVoxScreen();
                updatePropertiesPanel();
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                double oldX = selectedElement.x;
                double newX = selectedElement.x + (shift ? 0.5 : 1);
                if (snapToGrid) newX = Math.round(newX / 10.0) * 10;
                selectedElement.x = Math.min(width - selectedElement.width, newX);
                System.out.println("Key set x=" + selectedElement.x + " for " + selectedElement.id);
                applyEdit("x", oldX, selectedElement.x);
                updateVoxScreen();
                updatePropertiesPanel();
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_UP) {
                double oldY = selectedElement.y;
                double newY = selectedElement.y - (shift ? 0.5 : 1);
                if (snapToGrid) newY = Math.round(newY / 10.0) * 10;
                selectedElement.y = Math.max(0, newY);
                System.out.println("Key set y=" + selectedElement.y + " for " + selectedElement.id);
                applyEdit("y", oldY, selectedElement.y);
                updateVoxScreen();
                updatePropertiesPanel();
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_DOWN) {
                double oldY = selectedElement.y;
                double newY = selectedElement.y + (shift ? 0.5 : 1);
                if (snapToGrid) newY = Math.round(newY / 10.0) * 10;
                selectedElement.y = Math.min(height - selectedElement.height, newY);
                System.out.println("Key set y=" + selectedElement.y + " for " + selectedElement.id);
                applyEdit("y", oldY, selectedElement.y);
                updateVoxScreen();
                updatePropertiesPanel();
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_G && ctrl) {
                double oldX = selectedElement.x, oldY = selectedElement.y;
                selectedElement.x = Math.round(selectedElement.x / 10.0) * 10;
                selectedElement.y = Math.round(selectedElement.y / 10.0) * 10;
                System.out.println("Snap set x=" + selectedElement.x + ", y=" + selectedElement.y + " for " + selectedElement.id);
                applyEdit("x", oldX, selectedElement.x);
                applyEdit("y", oldY, selectedElement.y);
                updateVoxScreen();
                updatePropertiesPanel();
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_Z && ctrl) {
                undo();
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_Y && ctrl) {
                redo();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}