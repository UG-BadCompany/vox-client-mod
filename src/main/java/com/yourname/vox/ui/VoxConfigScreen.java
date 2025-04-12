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
    private Deque<EditAction> undoStack;
    private Deque<EditAction> redoStack;
    private boolean previewMode = false;
    private boolean dragging = false;
    private double dragOffsetX, dragOffsetY;
    private boolean showColorWheel = false;
    private boolean isBorderColorWheel = false;
    private boolean showCategories = true;
    private boolean showProperties = false;
    private boolean showLayers = false;
    private boolean snapToGrid = false;
    private Map<String, List<int[]>> colorPresets;
    private List<List<UIElement>> groups;
    private boolean draggingLayer = false;
    private int draggedLayerIndex = -1;
    private String helpHint = "";

    private static final int[] COLOR_OPTIONS = {
            0xFF000000, 0xFFFFFFFF, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00, 0xFF00FFFF, 0xFFFF00FF,
            0xFF4A4A4A, 0xFF1C2526, 0xFF1C3A5E, 0xFF66B2FF, 0xFF4CAF50, 0xFFE57373, 0xFF666666, 0x00000000
    };

    public VoxConfigScreen(VoxScreen parent, VoxTheme theme, VoxConfigManager configManager) {
        super(Text.literal("Vox UI Editor"));
        this.parent = parent;
        this.theme = theme;
        this.configManager = configManager;
        this.elements = new ArrayList<>();
        this.controlButtons = new ArrayList<>();
        this.sliders = new ArrayList<>();
        this.propertyButtons = new ArrayList<>();
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

    @Override
    protected void init() {
        elements.clear();
        controlButtons.clear();
        sliders.clear();
        propertyButtons.clear();
        undoStack.clear();
        redoStack.clear();
        showColorWheel = false;
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
        maxPropertiesOffset = Math.max(0, 16 * 30 - 360); // 16 controls * 30px - visible height
    }

    private void updateVoxScreen() {
        if (selectedElement == null) return;
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
        parent.updateElementColor(selectedElement.id, selectedElement.r, selectedElement.g, selectedElement.b);
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
        if (selectedElement == null) return;

        int sliderX = 10, sliderY = 60, buttonX = 100;
        helpHint = "Drag element, Shift + Arrows: Move 0.5px, Ctrl + G: Snap 10px.";
        propertyButtons.add(new VoxButton(theme, sliderX, sliderY, 70, 18, Text.literal("Back"), btn -> {
            showProperties = false;
            showCategories = true;
            selectedElement = null;
            helpHint = "Click element to edit, 'Layers' to reorder.";
            init();
        }));
        sliderY += 30;
        sliders.add(new SliderWidget(theme, sliderX, sliderY, 70, 18, 0, width, selectedElement.x, v -> {
            double oldX = selectedElement.x;
            selectedElement.x = snapToGrid ? Math.round(v / 10.0) * 10 : v;
            applyEdit("x", oldX, selectedElement.x);
            updateVoxScreen();
        }));
        propertyButtons.add(new VoxButton(theme, buttonX, sliderY, 18, 18, Text.literal("-0.5"), btn -> {
            double oldX = selectedElement.x;
            selectedElement.x = Math.max(0, selectedElement.x - 0.5);
            applyEdit("x", oldX, selectedElement.x);
            updateVoxScreen();
        }));
        propertyButtons.add(new VoxButton(theme, buttonX + 20, sliderY, 18, 18, Text.literal("+0.5"), btn -> {
            double oldX = selectedElement.x;
            selectedElement.x = Math.min(width - selectedElement.width, selectedElement.x + 0.5);
            applyEdit("x", oldX, selectedElement.x);
            updateVoxScreen();
        }));
        sliderY += 30;
        sliders.add(new SliderWidget(theme, sliderX, sliderY, 70, 18, 0, height, selectedElement.y, v -> {
            double oldY = selectedElement.y;
            selectedElement.y = snapToGrid ? Math.round(v / 10.0) * 10 : v;
            applyEdit("y", oldY, selectedElement.y);
            updateVoxScreen();
        }));
        propertyButtons.add(new VoxButton(theme, buttonX, sliderY, 18, 18, Text.literal("-0.5"), btn -> {
            double oldY = selectedElement.y;
            selectedElement.y = Math.max(0, selectedElement.y - 0.5);
            applyEdit("y", oldY, selectedElement.y);
            updateVoxScreen();
        }));
        propertyButtons.add(new VoxButton(theme, buttonX + 20, sliderY, 18, 18, Text.literal("+0.5"), btn -> {
            double oldY = selectedElement.y;
            selectedElement.y = Math.min(height - selectedElement.height, selectedElement.y + 0.5);
            applyEdit("y", oldY, selectedElement.y);
            updateVoxScreen();
        }));
        sliderY += 30;
        sliders.add(new SliderWidget(theme, sliderX, sliderY, 70, 18, selectedElement.type.equals("window") ? 50 : 20, 500, selectedElement.width, v -> {
            double oldWidth = selectedElement.width;
            selectedElement.width = snapToGrid ? Math.round(v / 10.0) * 10 : v;
            applyEdit("width", oldWidth, selectedElement.width);
            updateVoxScreen();
        }));
        propertyButtons.add(new VoxButton(theme, buttonX, sliderY, 18, 18, Text.literal("-0.5"), btn -> {
            double oldWidth = selectedElement.width;
            selectedElement.width = Math.max(selectedElement.type.equals("window") ? 50 : 20, selectedElement.width - 0.5);
            applyEdit("width", oldWidth, selectedElement.width);
            updateVoxScreen();
        }));
        propertyButtons.add(new VoxButton(theme, buttonX + 20, sliderY, 18, 18, Text.literal("+0.5"), btn -> {
            double oldWidth = selectedElement.width;
            selectedElement.width = Math.min(500, selectedElement.width + 0.5);
            applyEdit("width", oldWidth, selectedElement.width);
            updateVoxScreen();
        }));
        sliderY += 30;
        sliders.add(new SliderWidget(theme, sliderX, sliderY, 70, 18, selectedElement.type.equals("window") ? 50 : 20, 600, selectedElement.height, v -> {
            double oldHeight = selectedElement.height;
            selectedElement.height = snapToGrid ? Math.round(v / 10.0) * 10 : v;
            applyEdit("height", oldHeight, selectedElement.height);
            updateVoxScreen();
        }));
        propertyButtons.add(new VoxButton(theme, buttonX, sliderY, 18, 18, Text.literal("-0.5"), btn -> {
            double oldHeight = selectedElement.height;
            selectedElement.height = Math.max(selectedElement.type.equals("window") ? 50 : 20, selectedElement.height - 0.5);
            applyEdit("height", oldHeight, selectedElement.height);
            updateVoxScreen();
        }));
        propertyButtons.add(new VoxButton(theme, buttonX + 20, sliderY, 18, 18, Text.literal("+0.5"), btn -> {
            double oldHeight = selectedElement.height;
            selectedElement.height = Math.min(600, selectedElement.height + 0.5);
            applyEdit("height", oldHeight, selectedElement.height);
            updateVoxScreen();
        }));
        sliderY += 30;
        propertyButtons.add(new VoxButton(theme, sliderX, sliderY, 70, 18, Text.literal("Pick Col"), btn -> {
            showColorWheel = !showColorWheel;
            isBorderColorWheel = false;
        }));
        propertyButtons.add(new VoxButton(theme, buttonX, sliderY, 38, 18, Text.literal("Save Col"), btn -> {
            List<int[]> presets = colorPresets.computeIfAbsent(selectedElement.id, k -> new ArrayList<>());
            if (presets.size() < 5) {
                presets.add(new int[]{selectedElement.r, selectedElement.g, selectedElement.b, (int) (selectedElement.opacity * 255)});
            }
        }));
        sliderY += 30;
        for (int i = 0; i < 5; i++) {
            int presetIndex = i;
            propertyButtons.add(new VoxButton(theme, sliderX + i * 22, sliderY, 18, 18, Text.literal(String.valueOf(i + 1)), btn -> {
                List<int[]> presets = colorPresets.getOrDefault(selectedElement.id, new ArrayList<>());
                if (presetIndex < presets.size()) {
                    int[] preset = presets.get(presetIndex);
                    int oldR = selectedElement.r, oldG = selectedElement.g, oldB = selectedElement.b;
                    float oldOpacity = selectedElement.opacity;
                    selectedElement.r = preset[0];
                    selectedElement.g = preset[1];
                    selectedElement.b = preset[2];
                    selectedElement.opacity = preset[3] / 255.0f;
                    applyEdit("color", oldR + "," + oldG + "," + oldB, selectedElement.r + "," + selectedElement.g + "," + selectedElement.b);
                    applyEdit("opacity", oldOpacity, selectedElement.opacity);
                    updateVoxScreen();
                }
            }));
        }
        sliderY += 30;
        sliders.add(new SliderWidget(theme, sliderX, sliderY, 70, 18, 0.0, 1.0, selectedElement.opacity, v -> {
            float oldOpacity = selectedElement.opacity;
            selectedElement.opacity = v.floatValue();
            applyEdit("opacity", oldOpacity, selectedElement.opacity);
            updateVoxScreen();
        }));
        propertyButtons.add(new VoxButton(theme, buttonX, sliderY, 18, 18, Text.literal("-0.1"), btn -> {
            float oldOpacity = selectedElement.opacity;
            selectedElement.opacity = Math.max(0.0f, selectedElement.opacity - 0.1f);
            applyEdit("opacity", oldOpacity, selectedElement.opacity);
            updateVoxScreen();
        }));
        propertyButtons.add(new VoxButton(theme, buttonX + 20, sliderY, 18, 18, Text.literal("+0.1"), btn -> {
            float oldOpacity = selectedElement.opacity;
            selectedElement.opacity = Math.min(1.0f, selectedElement.opacity + 0.1f);
            applyEdit("opacity", oldOpacity, selectedElement.opacity);
            updateVoxScreen();
        }));
        sliderY += 30;
        sliders.add(new SliderWidget(theme, sliderX, sliderY, 70, 18, 0.5, 2.0, selectedElement.fontSize, v -> {
            float oldFontSize = selectedElement.fontSize;
            selectedElement.fontSize = v.floatValue();
            applyEdit("fontSize", oldFontSize, selectedElement.fontSize);
            updateVoxScreen();
        }));
        propertyButtons.add(new VoxButton(theme, buttonX, sliderY, 18, 18, Text.literal("-0.1"), btn -> {
            float oldFontSize = selectedElement.fontSize;
            selectedElement.fontSize = Math.max(0.5f, selectedElement.fontSize - 0.1f);
            applyEdit("fontSize", oldFontSize, selectedElement.fontSize);
            updateVoxScreen();
        }));
        propertyButtons.add(new VoxButton(theme, buttonX + 20, sliderY, 18, 18, Text.literal("+0.1"), btn -> {
            float oldFontSize = selectedElement.fontSize;
            selectedElement.fontSize = Math.min(2.0f, selectedElement.fontSize + 0.1f);
            applyEdit("fontSize", oldFontSize, selectedElement.fontSize);
            updateVoxScreen();
        }));
        sliderY += 30;
        propertyButtons.add(new VoxButton(theme, sliderX, sliderY, 36, 18, Text.literal("Left"), btn -> {
            String oldAlign = selectedElement.textAlign;
            selectedElement.textAlign = "left";
            applyEdit("textAlign", oldAlign, selectedElement.textAlign);
            updateVoxScreen();
        }));
        propertyButtons.add(new VoxButton(theme, sliderX + 38, sliderY, 36, 18, Text.literal("Center"), btn -> {
            String oldAlign = selectedElement.textAlign;
            selectedElement.textAlign = "center";
            applyEdit("textAlign", oldAlign, selectedElement.textAlign);
            updateVoxScreen();
        }));
        propertyButtons.add(new VoxButton(theme, sliderX + 76, sliderY, 36, 18, Text.literal("Right"), btn -> {
            String oldAlign = selectedElement.textAlign;
            selectedElement.textAlign = "right";
            applyEdit("textAlign", oldAlign, selectedElement.textAlign);
            updateVoxScreen();
        }));
        sliderY += 30;
        sliders.add(new SliderWidget(theme, sliderX, sliderY, 70, 18, 0, 10, selectedElement.spacing, v -> {
            int oldSpacing = selectedElement.spacing;
            selectedElement.spacing = v.intValue();
            applyEdit("spacing", oldSpacing, selectedElement.spacing);
            updateVoxScreen();
        }));
        propertyButtons.add(new VoxButton(theme, buttonX, sliderY, 18, 18, Text.literal("-1"), btn -> {
            int oldSpacing = selectedElement.spacing;
            selectedElement.spacing = Math.max(0, selectedElement.spacing - 1);
            applyEdit("spacing", oldSpacing, selectedElement.spacing);
            updateVoxScreen();
        }));
        propertyButtons.add(new VoxButton(theme, buttonX + 20, sliderY, 18, 18, Text.literal("+1"), btn -> {
            int oldSpacing = selectedElement.spacing;
            selectedElement.spacing = Math.min(10, selectedElement.spacing + 1);
            applyEdit("spacing", oldSpacing, selectedElement.spacing);
            updateVoxScreen();
        }));
        sliderY += 30;
        propertyButtons.add(new VoxButton(theme, sliderX, sliderY, 70, 18, Text.literal("Border: " + (selectedElement.border ? "On" : "Off")), btn -> {
            boolean oldBorder = selectedElement.border;
            selectedElement.border = !selectedElement.border;
            btn.setMessage(Text.literal("Border: " + (selectedElement.border ? "On" : "Off")));
            applyEdit("border", oldBorder, selectedElement.border);
            updateVoxScreen();
        }));
        sliderY += 30;
        sliders.add(new SliderWidget(theme, sliderX, sliderY, 70, 18, 1, 5, selectedElement.borderThickness, v -> {
            int oldThickness = selectedElement.borderThickness;
            selectedElement.borderThickness = v.intValue();
            applyEdit("borderThickness", oldThickness, selectedElement.borderThickness);
            updateVoxScreen();
        }));
        propertyButtons.add(new VoxButton(theme, buttonX, sliderY, 18, 18, Text.literal("-1"), btn -> {
            int oldThickness = selectedElement.borderThickness;
            selectedElement.borderThickness = Math.max(1, selectedElement.borderThickness - 1);
            applyEdit("borderThickness", oldThickness, selectedElement.borderThickness);
            updateVoxScreen();
        }));
        propertyButtons.add(new VoxButton(theme, buttonX + 20, sliderY, 18, 18, Text.literal("+1"), btn -> {
            int oldThickness = selectedElement.borderThickness;
            selectedElement.borderThickness = Math.min(5, selectedElement.borderThickness + 1);
            applyEdit("borderThickness", oldThickness, selectedElement.borderThickness);
            updateVoxScreen();
        }));
        sliderY += 30;
        propertyButtons.add(new VoxButton(theme, sliderX, sliderY, 70, 18, Text.literal("Bdr Col"), btn -> {
            showColorWheel = !showColorWheel;
            isBorderColorWheel = true;
        }));
        sliderY += 30;
        propertyButtons.add(new VoxButton(theme, sliderX, sliderY, 70, 18, Text.literal("Visible: " + (selectedElement.visible ? "On" : "Off")), btn -> {
            boolean oldVisible = selectedElement.visible;
            selectedElement.visible = !selectedElement.visible;
            btn.setMessage(Text.literal("Visible: " + (selectedElement.visible ? "On" : "Off")));
            applyEdit("visible", oldVisible, selectedElement.visible);
            updateVoxScreen();
        }));
        sliderY += 30;
        sliders.add(new SliderWidget(theme, sliderX, sliderY, 70, 18, -10, 10, selectedElement.zIndex, v -> {
            int oldZIndex = selectedElement.zIndex;
            selectedElement.zIndex = v.intValue();
            applyEdit("zIndex", oldZIndex, selectedElement.zIndex);
            updateVoxScreen();
        }));
        propertyButtons.add(new VoxButton(theme, buttonX, sliderY, 18, 18, Text.literal("-1"), btn -> {
            int oldZIndex = selectedElement.zIndex;
            selectedElement.zIndex = Math.max(-10, selectedElement.zIndex - 1);
            applyEdit("zIndex", oldZIndex, selectedElement.zIndex);
            updateVoxScreen();
        }));
        propertyButtons.add(new VoxButton(theme, buttonX + 20, sliderY, 18, 18, Text.literal("+1"), btn -> {
            int oldZIndex = selectedElement.zIndex;
            selectedElement.zIndex = Math.min(10, selectedElement.zIndex + 1);
            applyEdit("zIndex", oldZIndex, selectedElement.zIndex);
            updateVoxScreen();
        }));
        sliderY += 30;
        propertyButtons.add(new VoxButton(theme, sliderX, sliderY, 70, 18, Text.literal("Snap: " + (snapToGrid ? "On" : "Off")), btn -> {
            snapToGrid = !snapToGrid;
            btn.setMessage(Text.literal("Snap: " + (snapToGrid ? "On" : "Off")));
        }));
        sliderY += 30;
        propertyButtons.add(new VoxButton(theme, sliderX, sliderY, 70, 18, Text.literal("Easing: " + selectedElement.easing), btn -> {
            String oldEasing = selectedElement.easing;
            String[] easings = {"linear", "ease-in", "ease-out", "ease-in-out"};
            int currentIndex = -1;
            for (int i = 0; i < easings.length; i++) {
                if (easings[i].equals(selectedElement.easing)) {
                    currentIndex = i;
                    break;
                }
            }
            selectedElement.easing = easings[(currentIndex + 1) % easings.length];
            applyEdit("easing", oldEasing, selectedElement.easing);
            btn.setMessage(Text.literal("Easing: " + selectedElement.easing));
            updateVoxScreen();
        }));

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
                context.drawTextWithShadow(textRenderer, "Properties: " + selectedElement.id, 20, 40 - propertiesOffset, theme.getTextColor());
                int sliderY = 85;
                context.drawTextWithShadow(textRenderer, "X", 20, sliderY - 2 - propertiesOffset, theme.getTextColor());
                sliderY += 30;
                context.drawTextWithShadow(textRenderer, "Y", 20, sliderY - 2 - propertiesOffset, theme.getTextColor());
                sliderY += 30;
                context.drawTextWithShadow(textRenderer, "W", 20, sliderY - 2 - propertiesOffset, theme.getTextColor());
                sliderY += 30;
                context.drawTextWithShadow(textRenderer, "H", 20, sliderY - 2 - propertiesOffset, theme.getTextColor());
                sliderY += 30;
                context.drawTextWithShadow(textRenderer, "Col", 20, sliderY - 2 - propertiesOffset, theme.getTextColor());
                sliderY += 30;
                context.drawTextWithShadow(textRenderer, "Presets", 20, sliderY - 2 - propertiesOffset, theme.getTextColor());
                sliderY += 30;
                context.drawTextWithShadow(textRenderer, "Opac", 20, sliderY - 2 - propertiesOffset, theme.getTextColor());
                sliderY += 30;
                context.drawTextWithShadow(textRenderer, "Font", 20, sliderY - 2 - propertiesOffset, theme.getTextColor());
                sliderY += 30;
                context.drawTextWithShadow(textRenderer, "Align", 20, sliderY - 2 - propertiesOffset, theme.getTextColor());
                sliderY += 30;
                context.drawTextWithShadow(textRenderer, "Space", 20, sliderY - 2 - propertiesOffset, theme.getTextColor());
                sliderY += 30;
                context.drawTextWithShadow(textRenderer, "Bdr", 20, sliderY - 2 - propertiesOffset, theme.getTextColor());
                sliderY += 30;
                context.drawTextWithShadow(textRenderer, "Bdr Size", 20, sliderY - 2 - propertiesOffset, theme.getTextColor());
                sliderY += 30;
                context.drawTextWithShadow(textRenderer, "Bdr Col", 20, sliderY - 2 - propertiesOffset, theme.getTextColor());
                sliderY += 30;
                context.drawTextWithShadow(textRenderer, "Vis", 20, sliderY - 2 - propertiesOffset, theme.getTextColor());
                sliderY += 30;
                context.drawTextWithShadow(textRenderer, "Z", 20, sliderY - 2 - propertiesOffset, theme.getTextColor());
                sliderY += 30;
                context.drawTextWithShadow(textRenderer, "Snap", 20, sliderY - 2 - propertiesOffset, theme.getTextColor());
                sliderY += 30;
                context.drawTextWithShadow(textRenderer, "Ease", 20, sliderY - 2 - propertiesOffset, theme.getTextColor());
                for (SliderWidget slider : sliders) {
                    slider.render(context, mouseX, mouseY - propertiesOffset, delta);
                }
                for (VoxButton button : propertyButtons) {
                    button.render(context, mouseX, mouseY - propertiesOffset, delta);
                }
                context.disableScissor();
            }

            if (showColorWheel) {
                int centerX = 160, centerY = 130;
                int radius = 50;
                context.fill(centerX - radius - 5, centerY - radius - 5, centerX + radius + 5, centerY + radius + 5, theme.getPanelBgStart());
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        double distance = Math.sqrt(x * x + y * y);
                        if (distance <= radius) {
                            double angle = Math.atan2(y, x);
                            if (angle < 0) angle += 2 * Math.PI;
                            double hue = angle / (2 * Math.PI);
                            double saturation = distance / radius;
                            int[] rgb = hsvToRgb(hue, saturation, 1.0);
                            int color = 0xFF000000 | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
                            context.fill(centerX + x, centerY + y, centerX + x + 1, centerY + y + 1, color);
                        }
                    }
                }
            }

            context.drawCenteredTextWithShadow(textRenderer, helpHint, width / 2, height - 20, theme.getTextColor());
        }

        for (VoxButton button : controlButtons) {
            button.render(context, mouseX, mouseY, delta);
        }
    }

    private int[] hsvToRgb(double h, double s, double v) {
        double c = v * s;
        double x = c * (1 - Math.abs((h * 6) % 2 - 1));
        double m = v - c;
        double r = 0, g = 0, b = 0;
        if (h < 1.0 / 6) {
            r = c;
            g = x;
        } else if (h < 2.0 / 6) {
            r = x;
            g = c;
        } else if (h < 3.0 / 6) {
            g = c;
            b = x;
        } else if (h < 4.0 / 6) {
            g = x;
            b = c;
        } else if (h < 5.0 / 6) {
            r = x;
            b = c;
        } else {
            r = c;
            b = x;
        }
        return new int[]{
                (int) ((r + m) * 255),
                (int) ((g + m) * 255),
                (int) ((b + m) * 255)
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (editorMode && !previewMode) {
            for (VoxButton btn : controlButtons) {
                if (btn.mouseClicked(mouseX, mouseY, button)) return true;
            }

            if (showProperties) {
                for (VoxButton btn : propertyButtons) {
                    if (btn.mouseClicked(mouseX, mouseY - propertiesOffset, button)) return true;
                }
                for (SliderWidget slider : sliders) {
                    if (slider.mouseClicked(mouseX, mouseY - propertiesOffset, button)) return true;
                }
                if (showColorWheel) {
                    int centerX = 160, centerY = 130;
                    int radius = 50;
                    double dx = mouseX - centerX;
                    double dy = mouseY - centerY;
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance <= radius) {
                        double angle = Math.atan2(dy, dx);
                        if (angle < 0) angle += 2 * Math.PI;
                        double hue = angle / (2 * Math.PI);
                        double saturation = distance / radius;
                        int[] rgb = hsvToRgb(hue, saturation, 1.0);
                        if (selectedElement != null) {
                            if (isBorderColorWheel) {
                                // Defer border colors until VoxScreen supports it
                                int oldR = selectedElement.borderR, oldG = selectedElement.borderG, oldB = selectedElement.borderB;
                                selectedElement.borderR = rgb[0];
                                selectedElement.borderG = rgb[1];
                                selectedElement.borderB = rgb[2];
                                applyEdit("borderColor", oldR + "," + oldG + "," + oldB, rgb[0] + "," + rgb[1] + "," + rgb[2]);
                            } else {
                                int oldR = selectedElement.r, oldG = selectedElement.g, oldB = selectedElement.b;
                                selectedElement.r = rgb[0];
                                selectedElement.g = rgb[1];
                                selectedElement.b = rgb[2];
                                applyEdit("color", oldR + "," + oldG + "," + oldB, rgb[0] + "," + rgb[1] + "," + rgb[2]);
                            }
                            updateVoxScreen();
                        }
                        return true;
                    } else {
                        showColorWheel = false;
                        return true;
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
                        showColorWheel = false;
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
                    showColorWheel = false;
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
                applyEdit("x", oldX, selectedElement.x);
                updateVoxScreen();
                updatePropertiesPanel();
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                double oldX = selectedElement.x;
                double newX = selectedElement.x + (shift ? 0.5 : 1);
                if (snapToGrid) newX = Math.round(newX / 10.0) * 10;
                selectedElement.x = Math.min(width - selectedElement.width, newX);
                applyEdit("x", oldX, selectedElement.x);
                updateVoxScreen();
                updatePropertiesPanel();
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_UP) {
                double oldY = selectedElement.y;
                double newY = selectedElement.y - (shift ? 0.5 : 1);
                if (snapToGrid) newY = Math.round(newY / 10.0) * 10;
                selectedElement.y = Math.max(0, newY);
                applyEdit("y", oldY, selectedElement.y);
                updateVoxScreen();
                updatePropertiesPanel();
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_DOWN) {
                double oldY = selectedElement.y;
                double newY = selectedElement.y + (shift ? 0.5 : 1);
                if (snapToGrid) newY = Math.round(newY / 10.0) * 10;
                selectedElement.y = Math.min(height - selectedElement.height, newY);
                applyEdit("y", oldY, selectedElement.y);
                updateVoxScreen();
                updatePropertiesPanel();
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_G && ctrl) {
                double oldX = selectedElement.x, oldY = selectedElement.y;
                selectedElement.x = Math.round(selectedElement.x / 10.0) * 10;
                selectedElement.y = Math.round(selectedElement.y / 10.0) * 10;
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