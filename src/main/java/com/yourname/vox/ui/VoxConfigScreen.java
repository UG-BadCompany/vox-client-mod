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
import java.util.List;
import java.util.stream.Collectors;

public class VoxConfigScreen extends Screen {
    private final Screen parent;
    private final VoxTheme theme;
    private final VoxConfigManager configManager;
    private boolean editorMode = false;
    private List<UIElement> elements;
    private UIElement selectedElement = null;
    private int elementListOffset = 0;
    private int maxElementListOffset;
    private List<VoxButton> controlButtons;
    private List<SliderWidget> sliders;
    private List<VoxButton> propertyButtons;
    private Deque<EditAction> undoStack;
    private Deque<EditAction> redoStack;
    private boolean previewMode = false;
    private boolean dragging = false;
    private int dragOffsetX, dragOffsetY;

    private static final int[] COLOR_OPTIONS = {
            0xFF000000, 0xFFFFFFFF, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00, 0xFF00FFFF, 0xFFFF00FF,
            0xFF4A4A4A, 0xFF1C2526, 0xFF1C3A5E, 0xFF66B2FF, 0xFF4CAF50, 0xFFE57373, 0xFF666666, 0x00000000
    };

    public VoxConfigScreen(Screen parent, VoxTheme theme, VoxConfigManager configManager) {
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
    }

    private static class UIElement {
        String id;
        String type;
        int x, y, width, height;
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

        UIElement(String id, String type, int x, int y, int width, int height) {
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
            return json;
        }

        void fromJson(JsonObject json) {
            x = json.get("x").getAsInt();
            y = json.get("y").getAsInt();
            width = json.get("width").getAsInt();
            height = json.get("height").getAsInt();
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

        // Initialize UI elements
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

        // Initialize control buttons
        int buttonX = width - 150, buttonY = height - 150;
        controlButtons.add(new VoxButton(theme, buttonX, buttonY, 100, 20, Text.literal(editorMode ? "Normal Mode" : "Editor Mode"), btn -> {
            editorMode = !editorMode;
            btn.setMessage(Text.literal(editorMode ? "Normal Mode" : "Editor Mode"));
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

        // Initialize properties panel if an element is selected
        if (selectedElement != null) {
            updatePropertiesPanel();
        }

        maxElementListOffset = Math.max(0, elements.size() * 20 - 180);
    }

    private void updatePropertiesPanel() {
        sliders.clear();
        propertyButtons.clear();
        if (selectedElement == null) return;

        int sliderX = 20, sliderY = 100, buttonX = 130;
        sliders.add(new SliderWidget(theme, sliderX, sliderY, 100, 20, 0, width, selectedElement.x, v -> applyEdit("x", selectedElement.x, selectedElement.x = v.intValue())));
        propertyButtons.add(new VoxButton(theme, buttonX, sliderY, 20, 20, Text.literal("-10"), btn -> applyEdit("x", selectedElement.x, selectedElement.x = Math.max(0, selectedElement.x - 10))));
        propertyButtons.add(new VoxButton(theme, buttonX + 25, sliderY, 20, 20, Text.literal("+10"), btn -> applyEdit("x", selectedElement.x, selectedElement.x = Math.min(width - selectedElement.width, selectedElement.x + 10))));
        sliderY += 25;
        sliders.add(new SliderWidget(theme, sliderX, sliderY, 100, 20, 0, height, selectedElement.y, v -> applyEdit("y", selectedElement.y, selectedElement.y = v.intValue())));
        propertyButtons.add(new VoxButton(theme, buttonX, sliderY, 20, 20, Text.literal("-10"), btn -> applyEdit("y", selectedElement.y, selectedElement.y = Math.max(0, selectedElement.y - 10))));
        propertyButtons.add(new VoxButton(theme, buttonX + 25, sliderY, 20, 20, Text.literal("+10"), btn -> applyEdit("y", selectedElement.y, selectedElement.y = Math.min(height - selectedElement.height, selectedElement.y + 10))));
        sliderY += 25;
        sliders.add(new SliderWidget(theme, sliderX, sliderY, 100, 20, selectedElement.type.equals("window") ? 50 : 20, 500, selectedElement.width, v -> applyEdit("width", selectedElement.width, selectedElement.width = v.intValue())));
        propertyButtons.add(new VoxButton(theme, buttonX, sliderY, 20, 20, Text.literal("-10"), btn -> applyEdit("width", selectedElement.width, selectedElement.width = Math.max(selectedElement.type.equals("window") ? 50 : 20, selectedElement.width - 10))));
        propertyButtons.add(new VoxButton(theme, buttonX + 25, sliderY, 20, 20, Text.literal("+10"), btn -> applyEdit("width", selectedElement.width, selectedElement.width = Math.min(500, selectedElement.width + 10))));
        sliderY += 25;
        sliders.add(new SliderWidget(theme, sliderX, sliderY, 100, 20, selectedElement.type.equals("window") ? 50 : 20, 600, selectedElement.height, v -> applyEdit("height", selectedElement.height, selectedElement.height = v.intValue())));
        propertyButtons.add(new VoxButton(theme, buttonX, sliderY, 20, 20, Text.literal("-10"), btn -> applyEdit("height", selectedElement.height, selectedElement.height = Math.max(selectedElement.type.equals("window") ? 50 : 20, selectedElement.height - 10))));
        propertyButtons.add(new VoxButton(theme, buttonX + 25, sliderY, 20, 20, Text.literal("+10"), btn -> applyEdit("height", selectedElement.height, selectedElement.height = Math.min(600, selectedElement.height + 10))));
        sliderY += 25;
        sliders.add(new SliderWidget(theme, sliderX, sliderY, 100, 20, 0, 255, selectedElement.r, v -> applyEdit("r", selectedElement.r, selectedElement.r = v.intValue())));
        propertyButtons.add(new VoxButton(theme, buttonX, sliderY, 20, 20, Text.literal("-10"), btn -> applyEdit("r", selectedElement.r, selectedElement.r = Math.max(0, selectedElement.r - 10))));
        propertyButtons.add(new VoxButton(theme, buttonX + 25, sliderY, 20, 20, Text.literal("+10"), btn -> applyEdit("r", selectedElement.r, selectedElement.r = Math.min(255, selectedElement.r + 10))));
        sliderY += 25;
        sliders.add(new SliderWidget(theme, sliderX, sliderY, 100, 20, 0, 255, selectedElement.g, v -> applyEdit("g", selectedElement.g, selectedElement.g = v.intValue())));
        propertyButtons.add(new VoxButton(theme, buttonX, sliderY, 20, 20, Text.literal("-10"), btn -> applyEdit("g", selectedElement.g, selectedElement.g = Math.max(0, selectedElement.g - 10))));
        propertyButtons.add(new VoxButton(theme, buttonX + 25, sliderY, 20, 20, Text.literal("+10"), btn -> applyEdit("g", selectedElement.g, selectedElement.g = Math.min(255, selectedElement.g + 10))));
        sliderY += 25;
        sliders.add(new SliderWidget(theme, sliderX, sliderY, 100, 20, 0, 255, selectedElement.b, v -> applyEdit("b", selectedElement.b, selectedElement.b = v.intValue())));
        propertyButtons.add(new VoxButton(theme, buttonX, sliderY, 20, 20, Text.literal("-10"), btn -> applyEdit("b", selectedElement.b, selectedElement.b = Math.max(0, selectedElement.b - 10))));
        propertyButtons.add(new VoxButton(theme, buttonX + 25, sliderY, 20, 20, Text.literal("+10"), btn -> applyEdit("b", selectedElement.b, selectedElement.b = Math.min(255, selectedElement.b + 10))));
        sliderY += 25;
        sliders.add(new SliderWidget(theme, sliderX, sliderY, 100, 20, 0.0, 1.0, selectedElement.opacity, v -> applyEdit("opacity", selectedElement.opacity, selectedElement.opacity = v.floatValue())));
        propertyButtons.add(new VoxButton(theme, buttonX, sliderY, 20, 20, Text.literal("-0.1"), btn -> applyEdit("opacity", selectedElement.opacity, selectedElement.opacity = Math.max(0.0f, selectedElement.opacity - 0.1f))));
        propertyButtons.add(new VoxButton(theme, buttonX + 25, sliderY, 20, 20, Text.literal("+0.1"), btn -> applyEdit("opacity", selectedElement.opacity, selectedElement.opacity = Math.min(1.0f, selectedElement.opacity + 0.1f))));
        sliderY += 25;
        sliders.add(new SliderWidget(theme, sliderX, sliderY, 100, 20, 0.5, 2.0, selectedElement.fontSize, v -> applyEdit("fontSize", selectedElement.fontSize, selectedElement.fontSize = v.floatValue())));
        propertyButtons.add(new VoxButton(theme, buttonX, sliderY, 20, 20, Text.literal("-0.1"), btn -> applyEdit("fontSize", selectedElement.fontSize, selectedElement.fontSize = Math.max(0.5f, selectedElement.fontSize - 0.1f))));
        propertyButtons.add(new VoxButton(theme, buttonX + 25, sliderY, 20, 20, Text.literal("+0.1"), btn -> applyEdit("fontSize", selectedElement.fontSize, selectedElement.fontSize = Math.min(2.0f, selectedElement.fontSize + 0.1f))));
        sliderY += 25;
        propertyButtons.add(new VoxButton(theme, sliderX, sliderY, 50, 20, Text.literal("Left"), btn -> applyEdit("textAlign", selectedElement.textAlign, selectedElement.textAlign = "left")));
        propertyButtons.add(new VoxButton(theme, sliderX + 55, sliderY, 50, 20, Text.literal("Center"), btn -> applyEdit("textAlign", selectedElement.textAlign, selectedElement.textAlign = "center")));
        propertyButtons.add(new VoxButton(theme, sliderX + 110, sliderY, 50, 20, Text.literal("Right"), btn -> applyEdit("textAlign", selectedElement.textAlign, selectedElement.textAlign = "right")));
        sliderY += 25;
        sliders.add(new SliderWidget(theme, sliderX, sliderY, 100, 20, 0, 10, selectedElement.spacing, v -> applyEdit("spacing", selectedElement.spacing, selectedElement.spacing = v.intValue())));
        propertyButtons.add(new VoxButton(theme, buttonX, sliderY, 20, 20, Text.literal("-1"), btn -> applyEdit("spacing", selectedElement.spacing, selectedElement.spacing = Math.max(0, selectedElement.spacing - 1))));
        propertyButtons.add(new VoxButton(theme, buttonX + 25, sliderY, 20, 20, Text.literal("+1"), btn -> applyEdit("spacing", selectedElement.spacing, selectedElement.spacing = Math.min(10, selectedElement.spacing + 1))));
        sliderY += 25;
        propertyButtons.add(new VoxButton(theme, sliderX, sliderY, 100, 20, Text.literal("Border: " + (selectedElement.border ? "On" : "Off")), btn -> {
            applyEdit("border", selectedElement.border, selectedElement.border = !selectedElement.border);
            btn.setMessage(Text.literal("Border: " + (selectedElement.border ? "On" : "Off")));
        }));
        sliderY += 25;
        sliders.add(new SliderWidget(theme, sliderX, sliderY, 100, 20, 1, 5, selectedElement.borderThickness, v -> applyEdit("borderThickness", selectedElement.borderThickness, selectedElement.borderThickness = v.intValue())));
        propertyButtons.add(new VoxButton(theme, buttonX, sliderY, 20, 20, Text.literal("-1"), btn -> applyEdit("borderThickness", selectedElement.borderThickness, selectedElement.borderThickness = Math.max(1, selectedElement.borderThickness - 1))));
        propertyButtons.add(new VoxButton(theme, buttonX + 25, sliderY, 20, 20, Text.literal("+1"), btn -> applyEdit("borderThickness", selectedElement.borderThickness, selectedElement.borderThickness = Math.min(5, selectedElement.borderThickness + 1))));
        sliderY += 25;
        sliders.add(new SliderWidget(theme, sliderX, sliderY, 100, 20, 0, 255, selectedElement.borderR, v -> applyEdit("borderR", selectedElement.borderR, selectedElement.borderR = v.intValue())));
        propertyButtons.add(new VoxButton(theme, buttonX, sliderY, 20, 20, Text.literal("-10"), btn -> applyEdit("borderR", selectedElement.borderR, selectedElement.borderR = Math.max(0, selectedElement.borderR - 10))));
        propertyButtons.add(new VoxButton(theme, buttonX + 25, sliderY, 20, 20, Text.literal("+10"), btn -> applyEdit("borderR", selectedElement.borderR, selectedElement.borderR = Math.min(255, selectedElement.borderR + 10))));
        sliderY += 25;
        sliders.add(new SliderWidget(theme, sliderX, sliderY, 100, 20, 0, 255, selectedElement.borderG, v -> applyEdit("borderG", selectedElement.borderG, selectedElement.borderG = v.intValue())));
        propertyButtons.add(new VoxButton(theme, buttonX, sliderY, 20, 20, Text.literal("-10"), btn -> applyEdit("borderG", selectedElement.borderG, selectedElement.borderG = Math.max(0, selectedElement.borderG - 10))));
        propertyButtons.add(new VoxButton(theme, buttonX + 25, sliderY, 20, 20, Text.literal("+10"), btn -> applyEdit("borderG", selectedElement.borderG, selectedElement.borderG = Math.min(255, selectedElement.borderG + 10))));
        sliderY += 25;
        sliders.add(new SliderWidget(theme, sliderX, sliderY, 100, 20, 0, 255, selectedElement.borderB, v -> applyEdit("borderB", selectedElement.borderB, selectedElement.borderB = v.intValue())));
        propertyButtons.add(new VoxButton(theme, buttonX, sliderY, 20, 20, Text.literal("-10"), btn -> applyEdit("borderB", selectedElement.borderB, selectedElement.borderB = Math.max(0, selectedElement.borderB - 10))));
        propertyButtons.add(new VoxButton(theme, buttonX + 25, sliderY, 20, 20, Text.literal("+10"), btn -> applyEdit("borderB", selectedElement.borderB, selectedElement.borderB = Math.min(255, selectedElement.borderB + 10))));
        sliderY += 25;
        propertyButtons.add(new VoxButton(theme, sliderX, sliderY, 100, 20, Text.literal("Visible: " + (selectedElement.visible ? "On" : "Off")), btn -> {
            applyEdit("visible", selectedElement.visible, selectedElement.visible = !selectedElement.visible);
            btn.setMessage(Text.literal("Visible: " + (selectedElement.visible ? "On" : "Off")));
        }));
        sliderY += 25;
        sliders.add(new SliderWidget(theme, sliderX, sliderY, 100, 20, -10, 10, selectedElement.zIndex, v -> applyEdit("zIndex", selectedElement.zIndex, selectedElement.zIndex = v.intValue())));
        propertyButtons.add(new VoxButton(theme, buttonX, sliderY, 20, 20, Text.literal("-1"), btn -> applyEdit("zIndex", selectedElement.zIndex, selectedElement.zIndex = Math.max(-10, selectedElement.zIndex - 1))));
        propertyButtons.add(new VoxButton(theme, buttonX + 25, sliderY, 20, 20, Text.literal("+1"), btn -> applyEdit("zIndex", selectedElement.zIndex, selectedElement.zIndex = Math.min(10, selectedElement.zIndex + 1))));

        sliders.forEach(this::addDrawableChild);
        propertyButtons.forEach(this::addDrawableChild);
    }

    private void applyEdit(String property, Object oldValue, Object newValue) {
        if (!oldValue.equals(newValue)) {
            JsonObject before = selectedElement.toJson();
            JsonObject after = selectedElement.toJson();
            undoStack.push(new EditAction(selectedElement, before, after));
            redoStack.clear();
            updatePropertiesPanel();
        }
    }

    private void resetToDefaults() {
        JsonObject before = new JsonObject();
        elements.forEach(e -> before.add(e.id, e.toJson()));
        String[] categories = {"Chat", "Combat", "Miscellaneous", "Movement", "Player", "Render", "World"};
        int windowsPerRow = (int) Math.ceil(width / 85.0);
        int windowX = (width - (Math.min(categories.length, windowsPerRow) * 85 - 5)) / 2;
        int windowY = (height - ((int) Math.ceil((double) categories.length / windowsPerRow) * 400 - 5)) / 2;
        int row = 0, col = 0;
        elements.clear();
        elements.add(new UIElement("logo", "image", width / 2 - 50, 10, 100, 50));
        elements.add(new UIElement("search", "search", width - 130, 70, 120, 20));
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
        init();
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, theme.getBackgroundColor());
        context.fill(0, 0, width, 20, theme.getPanelBgStart());
        context.drawTextWithShadow(textRenderer, "Vox UI Editor", 10, 5, theme.getTextColor());

        // Render UI elements
        List<UIElement> sortedElements = elements.stream()
                .sorted((a, b) -> Integer.compare(a.zIndex, b.zIndex))
                .collect(Collectors.toList());
        for (UIElement element : sortedElements) {
            if (!element.visible && !editorMode) continue;
            int color = (element.r << 16) | (element.g << 8) | element.b | ((int) (element.opacity * 255) << 24);
            context.fill(element.x, element.y, element.x + element.width, element.y + element.height, color);
            if (element.border) {
                int borderColor = (element.borderR << 16) | (element.borderG << 8) | element.borderB | 0xFF000000;
                context.fill(element.x, element.y, element.x + element.width, element.y + element.borderThickness, borderColor);
                context.fill(element.x, element.y + element.height - element.borderThickness, element.x + element.width, element.y + element.height, borderColor);
                context.fill(element.x, element.y, element.x + element.borderThickness, element.y + element.height, borderColor);
                context.fill(element.x + element.width - element.borderThickness, element.y, element.x + element.width, element.y + element.height, borderColor);
            }
            float scale = element.fontSize;
            context.getMatrices().push();
            context.getMatrices().scale(scale, scale, 1.0f);
            int textX = element.textAlign.equals("left") ? element.x + 5 : element.textAlign.equals("right") ? element.x + element.width - textRenderer.getWidth(element.id) - 5 : element.x + (element.width - textRenderer.getWidth(element.id)) / 2;
            context.drawTextWithShadow(textRenderer, element.id, (int) (textX / scale), (int) ((element.y + 5) / scale), theme.getTextColor());
            context.getMatrices().pop();
        }

        // Render editor interface
        if (editorMode && !previewMode) {
            int listY = 30;
            context.fill(10, listY, 150, listY + 200, theme.getPanelBgStart());
            context.enableScissor(10, listY, 150, listY + 200);
            for (int i = 0; i < elements.size(); i++) {
                UIElement element = elements.get(i);
                int itemY = listY + 10 + i * 20 - elementListOffset;
                if (itemY >= listY && itemY <= listY + 180) {
                    context.drawTextWithShadow(textRenderer, element.id, 20, itemY, element == selectedElement ? theme.getTabActive() : theme.getTextColor());
                }
            }
            context.disableScissor();

            if (selectedElement != null) {
                context.fill(160, 30, 300, height - 30, theme.getPanelBgStart());
                context.drawTextWithShadow(textRenderer, "Properties: " + selectedElement.id, 170, 40, theme.getTextColor());
                int color = (selectedElement.r << 16) | (selectedElement.g << 8) | selectedElement.b | ((int) (selectedElement.opacity * 255) << 24);
                context.fill(170, 60, 190, 80, color);
                for (SliderWidget slider : sliders) {
                    slider.render(context, mouseX, mouseY, delta);
                }
                for (VoxButton button : propertyButtons) {
                    button.render(context, mouseX, mouseY, delta);
                }
            }
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
            for (VoxButton btn : propertyButtons) {
                if (btn.mouseClicked(mouseX, mouseY, button)) return true;
            }
            for (SliderWidget slider : sliders) {
                if (slider.mouseClicked(mouseX, mouseY, button)) return true;
            }
            if (mouseX >= 10 && mouseX <= 150 && mouseY >= 30 && mouseY <= 230) {
                int index = (int) ((mouseY - 40 + elementListOffset) / 20);
                if (index >= 0 && index < elements.size()) {
                    selectedElement = elements.get(index);
                    updatePropertiesPanel();
                }
                return true;
            }
            for (UIElement element : elements) {
                if (element.visible && mouseX >= element.x && mouseX <= element.x + element.width && mouseY >= element.y && mouseY <= element.y + element.height) {
                    selectedElement = element;
                    dragging = true;
                    dragOffsetX = (int) (mouseX - element.x);
                    dragOffsetY = (int) (mouseY - element.y);
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
            JsonObject before = selectedElement.toJson();
            selectedElement.x = MathHelper.clamp((int) (mouseX - dragOffsetX), 0, width - selectedElement.width);
            selectedElement.y = MathHelper.clamp((int) (mouseY - dragOffsetY), 0, height - selectedElement.height);
            applyEdit("position", before, selectedElement.toJson());
            updatePropertiesPanel();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (editorMode && mouseX >= 10 && mouseX <= 150 && mouseY >= 30 && mouseY <= 230) {
            elementListOffset = MathHelper.clamp(elementListOffset - (int) (verticalAmount * 20), 0, maxElementListOffset);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editorMode && selectedElement != null) {
            JsonObject before = selectedElement.toJson();
            boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
            boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                selectedElement.x = Math.max(0, selectedElement.x - (shift ? 1 : 10));
                applyEdit("x", before, selectedElement.toJson());
                updatePropertiesPanel();
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                selectedElement.x = Math.min(width - selectedElement.width, selectedElement.x + (shift ? 1 : 10));
                applyEdit("x", before, selectedElement.toJson());
                updatePropertiesPanel();
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_UP) {
                selectedElement.y = Math.max(0, selectedElement.y - (shift ? 1 : 10));
                applyEdit("y", before, selectedElement.toJson());
                updatePropertiesPanel();
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_DOWN) {
                selectedElement.y = Math.min(height - selectedElement.height, selectedElement.y + (shift ? 1 : 10));
                applyEdit("y", before, selectedElement.toJson());
                updatePropertiesPanel();
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_G && ctrl) {
                selectedElement.x = Math.round(selectedElement.x / 10.0f) * 10;
                selectedElement.y = Math.round(selectedElement.y / 10.0f) * 10;
                applyEdit("position", before, selectedElement.toJson());
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