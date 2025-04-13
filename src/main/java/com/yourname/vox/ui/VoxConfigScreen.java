package com.yourname.vox.ui;

import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private boolean needsPanelUpdate = false;
    private TextFieldWidget presetNameField;
    private List<VoxButton> presetButtons;
    private int presetListOffset = 0;
    private int maxPresetListOffset;
    // Dropdown states
    private boolean showPositionSize = false;
    private boolean showBody = false;
    private boolean showTitle = false;
    private boolean showBorder = false;
    private boolean showAppearance = false;

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
        this.presetButtons = new ArrayList<>();
    }

    private static class UIElement {
        String id;
        String type;
        double x, y, width, height;
        int r, g, b; // Body RGB
        int titleR, titleG, titleB; // Title bar RGB (windows only)
        int borderR, borderG, borderB; // Border RGB (windows only)
        int alpha; // Transparency (logo, search)
        boolean borderEnabled; // Border toggle (windows only)

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
            this.titleR = 255;
            this.titleG = 255;
            this.titleB = 255;
            this.borderR = 255;
            this.borderG = 255;
            this.borderB = 255;
            this.alpha = 255;
            this.borderEnabled = false;
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
            json.addProperty("titleR", titleR);
            json.addProperty("titleG", titleG);
            json.addProperty("titleB", titleB);
            json.addProperty("borderR", borderR);
            json.addProperty("borderG", borderG);
            json.addProperty("borderB", borderB);
            json.addProperty("alpha", alpha);
            json.addProperty("borderEnabled", borderEnabled);
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
            titleR = json.has("titleR") ? json.get("titleR").getAsInt() : 255;
            titleG = json.has("titleG") ? json.get("titleG").getAsInt() : 255;
            titleB = json.has("titleB") ? json.get("titleB").getAsInt() : 255;
            borderR = json.has("borderR") ? json.get("borderR").getAsInt() : 255;
            borderG = json.has("borderG") ? json.get("borderG").getAsInt() : 255;
            borderB = json.has("borderB") ? json.get("borderB").getAsInt() : 255;
            alpha = json.has("alpha") ? json.get("alpha").getAsInt() : 255;
            borderEnabled = json.has("borderEnabled") ? json.get("borderEnabled").getAsBoolean() : false;
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
        presetButtons.clear();
        undoStack.clear();
        redoStack.clear();
        showColorPicker = false;
        showLayers = false;
        showPositionSize = false;
        showBody = false;
        showTitle = false;
        showBorder = false;
        showAppearance = false;
        propertiesOffset = 0;
        presetListOffset = 0;
        needsPanelUpdate = false;
        if (selectedElement == null) {
            showCategories = true;
            showProperties = false;
        }
        helpHint = showCategories ? "Click element to edit, 'Layers' to reorder." : showLayers ? "Drag to reorder layers, click to return." : "Click dropdowns to edit properties.";

        // Initialize elements
        UIElement logo = new UIElement("logo", "image", parent.getLogoX(), parent.getLogoY(), parent.getLogoWidth(), parent.getLogoHeight());
        int[] logoColor = parent.getLogoColor();
        logo.r = logoColor[0];
        logo.g = logoColor[1];
        logo.b = logoColor[2];
        logo.alpha = logoColor[3];
        elements.add(logo);

        UIElement search = new UIElement("search", "search", parent.getSearchField().getX(), parent.getSearchField().getY(), parent.getSearchField().getWidth(), parent.getSearchField().getHeight());
        int[] searchColor = parent.getSearchColor();
        search.r = searchColor[0];
        search.g = searchColor[1];
        search.b = searchColor[2];
        search.alpha = searchColor[3];
        elements.add(search);

        for (CategoryWindow window : parent.getCategoryWindows()) {
            UIElement windowElement = new UIElement(window.getCategory(), "window", window.getX(), window.getY(), window.getWidth(), window.getHeight());
            int[] bodyColor = window.getBodyColor();
            int[] titleColor = window.getTitleColor();
            int[] borderColor = window.getBorderColor();
            windowElement.r = bodyColor[0];
            windowElement.g = bodyColor[1];
            windowElement.b = bodyColor[2];
            windowElement.titleR = titleColor[0];
            windowElement.titleG = titleColor[1];
            windowElement.titleB = titleColor[2];
            windowElement.borderR = borderColor[0];
            windowElement.borderG = borderColor[1];
            windowElement.borderB = borderColor[2];
            windowElement.borderEnabled = window.isBorderEnabled();
            elements.add(windowElement);
        }

        // Control buttons
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
        controlButtons.add(new VoxButton(theme, buttonX, buttonY, 100, 20, Text.literal(previewMode ? "Edit Mode" : "Preview Mode"), btn -> {
            previewMode = !previewMode;
            btn.setMessage(Text.literal(previewMode ? "Edit Mode" : "Preview Mode"));
        }));
        buttonY += 25;
        controlButtons.add(new VoxButton(theme, buttonX, buttonY, 100, 20, Text.literal("Reset"), btn -> resetToDefaults()));
        buttonY += 25;
        controlButtons.add(new VoxButton(theme, buttonX, buttonY, 100, 20, Text.literal("Undo"), btn -> undo()));
        buttonY += 25;
        controlButtons.add(new VoxButton(theme, buttonX, buttonY, 100, 20, Text.literal("Redo"), btn -> redo()));
        buttonY += 25;

        // Preset controls
        presetNameField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, buttonX, buttonY, 100, 20, Text.literal("Preset Name"));
        presetNameField.setMaxLength(32);
        presetNameField.setText("preset_" + System.currentTimeMillis());
        addDrawableChild(presetNameField);
        buttonY += 25;
        controlButtons.add(new VoxButton(theme, buttonX, buttonY, 50, 20, Text.literal("Save Preset"), btn -> {
            VoxConfigData.savePreset(presetNameField.getText(), parent);
            updatePresetButtons();
        }));
        controlButtons.add(new VoxButton(theme, buttonX + 50, buttonY, 50, 20, Text.literal("Load Preset"), btn -> {
            VoxConfigData.loadPreset(presetNameField.getText(), parent);
            updateSliders();
        }));
        buttonY += 25;

        // Numbered preset buttons
        for (int i = 1; i <= 5; i++) {
            int presetNum = i;
            controlButtons.add(new VoxButton(theme, buttonX, buttonY, 50, 20, Text.literal("Save " + i), btn -> {
                VoxConfigData.savePreset(presetNum, parent);
                updatePresetButtons();
            }));
            controlButtons.add(new VoxButton(theme, buttonX + 50, buttonY, 50, 20, Text.literal("Load " + i), btn -> {
                VoxConfigData.loadPreset(presetNum, parent);
                updateSliders();
            }));
            buttonY += 25;
        }

        // Preset selection buttons
        updatePresetButtons();

        controlButtons.forEach(this::addDrawableChild);

        if (selectedElement != null && showProperties) {
            updatePropertiesPanel();
        }

        maxElementListOffset = Math.max(0, elements.size() * 20 - 180);
        maxPropertiesOffset = Math.max(0, (selectedElement != null && selectedElement.type.equals("window") ? 6 * 20 : 3 * 20) - 360);
        maxPresetListOffset = Math.max(0, VoxConfigData.getPresetNames().size() * 20 - 180);
    }

    private void updatePresetButtons() {
        presetButtons.forEach(button -> button.visible = false);
        presetButtons.clear();
        int buttonX = width - 270, buttonY = 30;
        List<String> presets = VoxConfigData.getPresetNames();
        for (String preset : presets) {
            presetButtons.add(new VoxButton(theme, buttonX, buttonY, 100, 20, Text.literal(preset.length() > 12 ? preset.substring(0, 12) + "..." : preset), btn -> {
                presetNameField.setText(preset);
                VoxConfigData.loadPreset(preset, parent);
                updateSliders();
            }));
            buttonY += 20;
        }
        presetButtons.forEach(this::addDrawableChild);
    }

    private void updateSliders() {
        if (selectedElement != null && showProperties) {
            updatePropertiesPanel();
        }
    }

    private void updateVoxScreen() {
        if (selectedElement == null) {
            System.out.println("No element selected, skipping updateVoxScreen");
            return;
        }
        try {
            System.out.println("Updating " + selectedElement.id + ": x=" + selectedElement.x + ", y=" + selectedElement.y + ", width=" + selectedElement.width + ", height=" + selectedElement.height);
            System.out.println("Color: Red=" + selectedElement.r + ", Green=" + selectedElement.g + ", Blue=" + selectedElement.b + ", Alpha=" + selectedElement.alpha);
            if (selectedElement.type.equals("window")) {
                System.out.println("Title Color: Red=" + selectedElement.titleR + ", Green=" + selectedElement.titleG + ", Blue=" + selectedElement.titleB);
                System.out.println("Border Color: Red=" + selectedElement.borderR + ", Green=" + selectedElement.borderG + ", Blue=" + selectedElement.borderB + ", Enabled=" + selectedElement.borderEnabled);
            }

            if (selectedElement.id.equals("logo")) {
                parent.updateLogoPosition((int) selectedElement.x, (int) selectedElement.y);
                parent.updateLogoSize((int) selectedElement.width, (int) selectedElement.height);
                parent.updateLogoColor(selectedElement.r, selectedElement.g, selectedElement.b, selectedElement.alpha);
            } else if (selectedElement.id.equals("search")) {
                parent.updateSearchPosition((int) selectedElement.x, (int) selectedElement.y);
                parent.updateSearchSize((int) selectedElement.width, (int) selectedElement.height);
                parent.updateSearchColor(selectedElement.r, selectedElement.g, selectedElement.b, selectedElement.alpha);
            } else {
                parent.updateCategoryPosition(selectedElement.id, (int) selectedElement.x, (int) selectedElement.y);
                parent.updateCategorySize(selectedElement.id, (int) selectedElement.width, (int) selectedElement.height);
                parent.updateCategoryColor(selectedElement.id, selectedElement.r, selectedElement.g, selectedElement.b, selectedElement.titleR, selectedElement.titleG, selectedElement.titleB);
                parent.updateCategoryBorder(selectedElement.id, selectedElement.borderR, selectedElement.borderG, selectedElement.borderB, selectedElement.borderEnabled);
            }

            needsPanelUpdate = true;

            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> {
                try {
                    parent.render(new DrawContext(client, client.getBufferBuilders().getEntityVertexConsumers()), 0, 0, 0);
                    parent.init(client, width, height);
                    System.out.println("Reinitialized VoxScreen for " + selectedElement.id);
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

        int sliderX = 15, sliderY = 60, buttonX = 105;
        helpHint = "Click dropdowns to edit properties.";

        // Back button
        VoxButton backButton = new VoxButton(theme, sliderX, sliderY, 70, 18, Text.literal("Back"), btn -> {
            showProperties = false;
            showCategories = true;
            selectedElement = null;
            helpHint = "Click element to edit, 'Layers' to reorder.";
            init();
        });
        propertyButtons.add(backButton);
        buttonPositions.add(new WidgetPosition(sliderX, sliderY, 70, 18));
        sliderY += 20;

        if (selectedElement.type.equals("window")) {
            // Position & Size Dropdown
            VoxButton posSizeButton = new VoxButton(theme, sliderX, sliderY, 90, 18, Text.literal("Position & Size " + (showPositionSize ? "▼" : "▶")), btn -> {
                showPositionSize = !showPositionSize;
                showBody = false;
                showTitle = false;
                showBorder = false;
                showAppearance = false;
                needsPanelUpdate = true;
            });
            propertyButtons.add(posSizeButton);
            buttonPositions.add(new WidgetPosition(sliderX, sliderY, 90, 18));
            sliderY += 20;

            if (showPositionSize) {
                // X Slider
                SliderWidget xSlider = new SliderWidget(theme, sliderX + 10, sliderY, 60, 18, 0, width, selectedElement.x, v -> {
                    double oldX = selectedElement.x;
                    selectedElement.x = snapToGrid ? Math.round(v / 10.0) * 10 : v;
                    applyEdit("x", oldX, selectedElement.x);
                    updateVoxScreen();
                });
                sliders.add(xSlider);
                sliderPositions.add(new WidgetPosition(sliderX + 10, sliderY, 60, 18));
                VoxButton xMinus = new VoxButton(theme, buttonX, sliderY, 18, 18, Text.literal("-0.5"), btn -> {
                    double oldX = selectedElement.x;
                    selectedElement.x = Math.max(0, selectedElement.x - 0.5);
                    applyEdit("x", oldX, selectedElement.x);
                    updateVoxScreen();
                });
                VoxButton xPlus = new VoxButton(theme, buttonX + 20, sliderY, 18, 18, Text.literal("+0.5"), btn -> {
                    double oldX = selectedElement.x;
                    selectedElement.x = Math.min(width - selectedElement.width, selectedElement.x + 0.5);
                    applyEdit("x", oldX, selectedElement.x);
                    updateVoxScreen();
                });
                propertyButtons.add(xMinus);
                propertyButtons.add(xPlus);
                buttonPositions.add(new WidgetPosition(buttonX, sliderY, 18, 18));
                buttonPositions.add(new WidgetPosition(buttonX + 20, sliderY, 18, 18));
                sliderY += 20;

                // Y Slider
                SliderWidget ySlider = new SliderWidget(theme, sliderX + 10, sliderY, 60, 18, 0, height, selectedElement.y, v -> {
                    double oldY = selectedElement.y;
                    selectedElement.y = snapToGrid ? Math.round(v / 10.0) * 10 : v;
                    applyEdit("y", oldY, selectedElement.y);
                    updateVoxScreen();
                });
                sliders.add(ySlider);
                sliderPositions.add(new WidgetPosition(sliderX + 10, sliderY, 60, 18));
                VoxButton yMinus = new VoxButton(theme, buttonX, sliderY, 18, 18, Text.literal("-0.5"), btn -> {
                    double oldY = selectedElement.y;
                    selectedElement.y = Math.max(0, selectedElement.y - 0.5);
                    applyEdit("y", oldY, selectedElement.y);
                    updateVoxScreen();
                });
                VoxButton yPlus = new VoxButton(theme, buttonX + 20, sliderY, 18, 18, Text.literal("+0.5"), btn -> {
                    double oldY = selectedElement.y;
                    selectedElement.y = Math.min(height - selectedElement.height, selectedElement.y + 0.5);
                    applyEdit("y", oldY, selectedElement.y);
                    updateVoxScreen();
                });
                propertyButtons.add(yMinus);
                propertyButtons.add(yPlus);
                buttonPositions.add(new WidgetPosition(buttonX, sliderY, 18, 18));
                buttonPositions.add(new WidgetPosition(buttonX + 20, sliderY, 18, 18));
                sliderY += 20;

                // Width Slider
                SliderWidget wSlider = new SliderWidget(theme, sliderX + 10, sliderY, 60, 18, 50, 500, selectedElement.width, v -> {
                    double oldWidth = selectedElement.width;
                    selectedElement.width = snapToGrid ? Math.round(v / 10.0) * 10 : v;
                    applyEdit("width", oldWidth, selectedElement.width);
                    updateVoxScreen();
                });
                sliders.add(wSlider);
                sliderPositions.add(new WidgetPosition(sliderX + 10, sliderY, 60, 18));
                VoxButton wMinus = new VoxButton(theme, buttonX, sliderY, 18, 18, Text.literal("-0.5"), btn -> {
                    double oldWidth = selectedElement.width;
                    selectedElement.width = Math.max(50, selectedElement.width - 0.5);
                    applyEdit("width", oldWidth, selectedElement.width);
                    updateVoxScreen();
                });
                VoxButton wPlus = new VoxButton(theme, buttonX + 20, sliderY, 18, 18, Text.literal("+0.5"), btn -> {
                    double oldWidth = selectedElement.width;
                    selectedElement.width = Math.min(500, selectedElement.width + 0.5);
                    applyEdit("width", oldWidth, selectedElement.width);
                    updateVoxScreen();
                });
                propertyButtons.add(wMinus);
                propertyButtons.add(wPlus);
                buttonPositions.add(new WidgetPosition(buttonX, sliderY, 18, 18));
                buttonPositions.add(new WidgetPosition(buttonX + 20, sliderY, 18, 18));
                sliderY += 20;

                // Height Slider
                SliderWidget hSlider = new SliderWidget(theme, sliderX + 10, sliderY, 60, 18, 50, 600, selectedElement.height, v -> {
                    double oldHeight = selectedElement.height;
                    selectedElement.height = snapToGrid ? Math.round(v / 10.0) * 10 : v;
                    applyEdit("height", oldHeight, selectedElement.height);
                    updateVoxScreen();
                });
                sliders.add(hSlider);
                sliderPositions.add(new WidgetPosition(sliderX + 10, sliderY, 60, 18));
                VoxButton hMinus = new VoxButton(theme, buttonX, sliderY, 18, 18, Text.literal("-0.5"), btn -> {
                    double oldHeight = selectedElement.height;
                    selectedElement.height = Math.max(50, selectedElement.height - 0.5);
                    applyEdit("height", oldHeight, selectedElement.height);
                    updateVoxScreen();
                });
                VoxButton hPlus = new VoxButton(theme, buttonX + 20, sliderY, 18, 18, Text.literal("+0.5"), btn -> {
                    double oldHeight = selectedElement.height;
                    selectedElement.height = Math.min(600, selectedElement.height + 0.5);
                    applyEdit("height", oldHeight, selectedElement.height);
                    updateVoxScreen();
                });
                propertyButtons.add(hMinus);
                propertyButtons.add(hPlus);
                buttonPositions.add(new WidgetPosition(buttonX, sliderY, 18, 18));
                buttonPositions.add(new WidgetPosition(buttonX + 20, sliderY, 18, 18));
                sliderY += 20;
            }

            // Body Dropdown
            if (!showPositionSize) {
                VoxButton bodyButton = new VoxButton(theme, sliderX, sliderY, 90, 18, Text.literal("Body Colors " + (showBody ? "▼" : "▶")), btn -> {
                    showBody = !showBody;
                    showPositionSize = false;
                    showTitle = false;
                    showBorder = false;
                    showAppearance = false;
                    needsPanelUpdate = true;
                });
                propertyButtons.add(bodyButton);
                buttonPositions.add(new WidgetPosition(sliderX, sliderY, 90, 18));
                sliderY += 20;
            }

            if (showBody) {
                // Body RGB
                SliderWidget rSlider = new SliderWidget(theme, sliderX + 10, sliderY, 60, 18, 0, 255, selectedElement.r, v -> {
                    int oldR = selectedElement.r;
                    selectedElement.r = v.intValue();
                    applyEdit("red", oldR, selectedElement.r);
                    updateVoxScreen();
                });
                sliders.add(rSlider);
                sliderPositions.add(new WidgetPosition(sliderX + 10, sliderY, 60, 18));
                sliderY += 20;
                SliderWidget gSlider = new SliderWidget(theme, sliderX + 10, sliderY, 60, 18, 0, 255, selectedElement.g, v -> {
                    int oldG = selectedElement.g;
                    selectedElement.g = v.intValue();
                    applyEdit("green", oldG, selectedElement.g);
                    updateVoxScreen();
                });
                sliders.add(gSlider);
                sliderPositions.add(new WidgetPosition(sliderX + 10, sliderY, 60, 18));
                sliderY += 20;
                SliderWidget bSlider = new SliderWidget(theme, sliderX + 10, sliderY, 60, 18, 0, 255, selectedElement.b, v -> {
                    int oldB = selectedElement.b;
                    selectedElement.b = v.intValue();
                    applyEdit("blue", oldB, selectedElement.b);
                    updateVoxScreen();
                });
                sliders.add(bSlider);
                sliderPositions.add(new WidgetPosition(sliderX + 10, sliderY, 60, 18));
                sliderY += 20;
            }

            // Title Dropdown
            if (!showPositionSize && !showBody) {
                VoxButton titleButton = new VoxButton(theme, sliderX, sliderY, 90, 18, Text.literal("Title Colors " + (showTitle ? "▼" : "▶")), btn -> {
                    showTitle = !showTitle;
                    showPositionSize = false;
                    showBody = false;
                    showBorder = false;
                    showAppearance = false;
                    needsPanelUpdate = true;
                });
                propertyButtons.add(titleButton);
                buttonPositions.add(new WidgetPosition(sliderX, sliderY, 90, 18));
                sliderY += 20;
            }

            if (showTitle) {
                // Title RGB
                SliderWidget titleRSlider = new SliderWidget(theme, sliderX + 10, sliderY, 60, 18, 0, 255, selectedElement.titleR, v -> {
                    int oldTitleR = selectedElement.titleR;
                    selectedElement.titleR = v.intValue();
                    applyEdit("titleRed", oldTitleR, selectedElement.titleR);
                    updateVoxScreen();
                });
                sliders.add(titleRSlider);
                sliderPositions.add(new WidgetPosition(sliderX + 10, sliderY, 60, 18));
                sliderY += 20;
                SliderWidget titleGSlider = new SliderWidget(theme, sliderX + 10, sliderY, 60, 18, 0, 255, selectedElement.titleG, v -> {
                    int oldTitleG = selectedElement.titleG;
                    selectedElement.titleG = v.intValue();
                    applyEdit("titleGreen", oldTitleG, selectedElement.titleG);
                    updateVoxScreen();
                });
                sliders.add(titleGSlider);
                sliderPositions.add(new WidgetPosition(sliderX + 10, sliderY, 60, 18));
                sliderY += 20;
                SliderWidget titleBSlider = new SliderWidget(theme, sliderX + 10, sliderY, 60, 18, 0, 255, selectedElement.titleB, v -> {
                    int oldTitleB = selectedElement.titleB;
                    selectedElement.titleB = v.intValue();
                    applyEdit("titleBlue", oldTitleB, selectedElement.titleB);
                    updateVoxScreen();
                });
                sliders.add(titleBSlider);
                sliderPositions.add(new WidgetPosition(sliderX + 10, sliderY, 60, 18));
                sliderY += 20;
            }

            // Border Dropdown
            if (!showPositionSize && !showBody && !showTitle) {
                VoxButton borderButton = new VoxButton(theme, sliderX, sliderY, 90, 18, Text.literal("Border Settings " + (showBorder ? "▼" : "▶")), btn -> {
                    showBorder = !showBorder;
                    showPositionSize = false;
                    showBody = false;
                    showTitle = false;
                    showAppearance = false;
                    needsPanelUpdate = true;
                });
                propertyButtons.add(borderButton);
                buttonPositions.add(new WidgetPosition(sliderX, sliderY, 90, 18));
                sliderY += 20;
            }

            if (showBorder) {
                // Border RGB
                SliderWidget borderRSlider = new SliderWidget(theme, sliderX + 10, sliderY, 60, 18, 0, 255, selectedElement.borderR, v -> {
                    int oldBorderR = selectedElement.borderR;
                    selectedElement.borderR = v.intValue();
                    applyEdit("borderRed", oldBorderR, selectedElement.borderR);
                    updateVoxScreen();
                });
                sliders.add(borderRSlider);
                sliderPositions.add(new WidgetPosition(sliderX + 10, sliderY, 60, 18));
                sliderY += 20;
                SliderWidget borderGSlider = new SliderWidget(theme, sliderX + 10, sliderY, 60, 18, 0, 255, selectedElement.borderG, v -> {
                    int oldBorderG = selectedElement.borderG;
                    selectedElement.borderG = v.intValue();
                    applyEdit("borderGreen", oldBorderG, selectedElement.borderG);
                    updateVoxScreen();
                });
                sliders.add(borderGSlider);
                sliderPositions.add(new WidgetPosition(sliderX + 10, sliderY, 60, 18));
                sliderY += 20;
                SliderWidget borderBSlider = new SliderWidget(theme, sliderX + 10, sliderY, 60, 18, 0, 255, selectedElement.borderB, v -> {
                    int oldBorderB = selectedElement.borderB;
                    selectedElement.borderB = v.intValue();
                    applyEdit("borderBlue", oldBorderB, selectedElement.borderB);
                    updateVoxScreen();
                });
                sliders.add(borderBSlider);
                sliderPositions.add(new WidgetPosition(sliderX + 10, sliderY, 60, 18));
                sliderY += 20;
                VoxButton borderToggle = new VoxButton(theme, sliderX + 10, sliderY, 60, 18, Text.literal("Border: " + (selectedElement.borderEnabled ? "On" : "Off")), btn -> {
                    boolean oldBorderEnabled = selectedElement.borderEnabled;
                    selectedElement.borderEnabled = !selectedElement.borderEnabled;
                    btn.setMessage(Text.literal("Border: " + (selectedElement.borderEnabled ? "On" : "Off")));
                    applyEdit("borderEnabled", oldBorderEnabled, selectedElement.borderEnabled);
                    updateVoxScreen();
                });
                propertyButtons.add(borderToggle);
                buttonPositions.add(new WidgetPosition(sliderX + 10, sliderY, 60, 18));
                sliderY += 20;
            }
        } else {
            // Appearance Dropdown (for logo and search)
            if (!showPositionSize && !showBody && !showTitle && !showBorder) {
                String header = selectedElement.id.equals("search") ? "Header Colors" : "Appearance";
                VoxButton appearanceButton = new VoxButton(theme, sliderX, sliderY, 90, 18, Text.literal(header + " " + (showAppearance ? "▼" : "▶")), btn -> {
                    showAppearance = !showAppearance;
                    showPositionSize = false;
                    showBody = false;
                    showTitle = false;
                    showBorder = false;
                    needsPanelUpdate = true;
                });
                propertyButtons.add(appearanceButton);
                buttonPositions.add(new WidgetPosition(sliderX, sliderY, 90, 18));
                sliderY += 20;
            }

            if (showAppearance) {
                // Position & Size
                SliderWidget xSlider = new SliderWidget(theme, sliderX + 10, sliderY, 60, 18, 0, width, selectedElement.x, v -> {
                    double oldX = selectedElement.x;
                    selectedElement.x = snapToGrid ? Math.round(v / 10.0) * 10 : v;
                    applyEdit("x", oldX, selectedElement.x);
                    updateVoxScreen();
                });
                sliders.add(xSlider);
                sliderPositions.add(new WidgetPosition(sliderX + 10, sliderY, 60, 18));
                VoxButton xMinus = new VoxButton(theme, buttonX, sliderY, 18, 18, Text.literal("-0.5"), btn -> {
                    double oldX = selectedElement.x;
                    selectedElement.x = Math.max(0, selectedElement.x - 0.5);
                    applyEdit("x", oldX, selectedElement.x);
                    updateVoxScreen();
                });
                VoxButton xPlus = new VoxButton(theme, buttonX + 20, sliderY, 18, 18, Text.literal("+0.5"), btn -> {
                    double oldX = selectedElement.x;
                    selectedElement.x = Math.min(width - selectedElement.width, selectedElement.x + 0.5);
                    applyEdit("x", oldX, selectedElement.x);
                    updateVoxScreen();
                });
                propertyButtons.add(xMinus);
                propertyButtons.add(xPlus);
                buttonPositions.add(new WidgetPosition(buttonX, sliderY, 18, 18));
                buttonPositions.add(new WidgetPosition(buttonX + 20, sliderY, 18, 18));
                sliderY += 20;

                SliderWidget ySlider = new SliderWidget(theme, sliderX + 10, sliderY, 60, 18, 0, height, selectedElement.y, v -> {
                    double oldY = selectedElement.y;
                    selectedElement.y = snapToGrid ? Math.round(v / 10.0) * 10 : v;
                    applyEdit("y", oldY, selectedElement.y);
                    updateVoxScreen();
                });
                sliders.add(ySlider);
                sliderPositions.add(new WidgetPosition(sliderX + 10, sliderY, 60, 18));
                VoxButton yMinus = new VoxButton(theme, buttonX, sliderY, 18, 18, Text.literal("-0.5"), btn -> {
                    double oldY = selectedElement.y;
                    selectedElement.y = Math.max(0, selectedElement.y - 0.5);
                    applyEdit("y", oldY, selectedElement.y);
                    updateVoxScreen();
                });
                VoxButton yPlus = new VoxButton(theme, buttonX + 20, sliderY, 18, 18, Text.literal("+0.5"), btn -> {
                    double oldY = selectedElement.y;
                    selectedElement.y = Math.min(height - selectedElement.height, selectedElement.y + 0.5);
                    applyEdit("y", oldY, selectedElement.y);
                    updateVoxScreen();
                });
                propertyButtons.add(yMinus);
                propertyButtons.add(yPlus);
                buttonPositions.add(new WidgetPosition(buttonX, sliderY, 18, 18));
                buttonPositions.add(new WidgetPosition(buttonX + 20, sliderY, 18, 18));
                sliderY += 20;

                SliderWidget wSlider = new SliderWidget(theme, sliderX + 10, sliderY, 60, 18, 20, 500, selectedElement.width, v -> {
                    double oldWidth = selectedElement.width;
                    selectedElement.width = snapToGrid ? Math.round(v / 10.0) * 10 : v;
                    applyEdit("width", oldWidth, selectedElement.width);
                    updateVoxScreen();
                });
                sliders.add(wSlider);
                sliderPositions.add(new WidgetPosition(sliderX + 10, sliderY, 60, 18));
                VoxButton wMinus = new VoxButton(theme, buttonX, sliderY, 18, 18, Text.literal("-0.5"), btn -> {
                    double oldWidth = selectedElement.width;
                    selectedElement.width = Math.max(20, selectedElement.width - 0.5);
                    applyEdit("width", oldWidth, selectedElement.width);
                    updateVoxScreen();
                });
                VoxButton wPlus = new VoxButton(theme, buttonX + 20, sliderY, 18, 18, Text.literal("+0.5"), btn -> {
                    double oldWidth = selectedElement.width;
                    selectedElement.width = Math.min(500, selectedElement.width + 0.5);
                    applyEdit("width", oldWidth, selectedElement.width);
                    updateVoxScreen();
                });
                propertyButtons.add(wMinus);
                propertyButtons.add(wPlus);
                buttonPositions.add(new WidgetPosition(buttonX, sliderY, 18, 18));
                buttonPositions.add(new WidgetPosition(buttonX + 20, sliderY, 18, 18));
                sliderY += 20;

                SliderWidget hSlider = new SliderWidget(theme, sliderX + 10, sliderY, 60, 18, 20, 600, selectedElement.height, v -> {
                    double oldHeight = selectedElement.height;
                    selectedElement.height = snapToGrid ? Math.round(v / 10.0) * 10 : v;
                    applyEdit("height", oldHeight, selectedElement.height);
                    updateVoxScreen();
                });
                sliders.add(hSlider);
                sliderPositions.add(new WidgetPosition(sliderX + 10, sliderY, 60, 18));
                VoxButton hMinus = new VoxButton(theme, buttonX, sliderY, 18, 18, Text.literal("-0.5"), btn -> {
                    double oldHeight = selectedElement.height;
                    selectedElement.height = Math.max(20, selectedElement.height - 0.5);
                    applyEdit("height", oldHeight, selectedElement.height);
                    updateVoxScreen();
                });
                VoxButton hPlus = new VoxButton(theme, buttonX + 20, sliderY, 18, 18, Text.literal("+0.5"), btn -> {
                    double oldHeight = selectedElement.height;
                    selectedElement.height = Math.min(600, selectedElement.height + 0.5);
                    applyEdit("height", oldHeight, selectedElement.height);
                    updateVoxScreen();
                });
                propertyButtons.add(hMinus);
                propertyButtons.add(hPlus);
                buttonPositions.add(new WidgetPosition(buttonX, sliderY, 18, 18));
                buttonPositions.add(new WidgetPosition(buttonX + 20, sliderY, 18, 18));
                sliderY += 20;

                // RGB
                SliderWidget rSlider = new SliderWidget(theme, sliderX + 10, sliderY, 60, 18, 0, 255, selectedElement.r, v -> {
                    int oldR = selectedElement.r;
                    selectedElement.r = v.intValue();
                    applyEdit("red", oldR, selectedElement.r);
                    updateVoxScreen();
                });
                sliders.add(rSlider);
                sliderPositions.add(new WidgetPosition(sliderX + 10, sliderY, 60, 18));
                sliderY += 20;
                SliderWidget gSlider = new SliderWidget(theme, sliderX + 10, sliderY, 60, 18, 0, 255, selectedElement.g, v -> {
                    int oldG = selectedElement.g;
                    selectedElement.g = v.intValue();
                    applyEdit("green", oldG, selectedElement.g);
                    updateVoxScreen();
                });
                sliders.add(gSlider);
                sliderPositions.add(new WidgetPosition(sliderX + 10, sliderY, 60, 18));
                sliderY += 20;
                SliderWidget bSlider = new SliderWidget(theme, sliderX + 10, sliderY, 60, 18, 0, 255, selectedElement.b, v -> {
                    int oldB = selectedElement.b;
                    selectedElement.b = v.intValue();
                    applyEdit("blue", oldB, selectedElement.b);
                    updateVoxScreen();
                });
                sliders.add(bSlider);
                sliderPositions.add(new WidgetPosition(sliderX + 10, sliderY, 60, 18));
                sliderY += 20;

                // Alpha
                SliderWidget aSlider = new SliderWidget(theme, sliderX + 10, sliderY, 60, 18, 0, 255, selectedElement.alpha, v -> {
                    int oldAlpha = selectedElement.alpha;
                    selectedElement.alpha = v.intValue();
                    applyEdit("alpha", oldAlpha, selectedElement.alpha);
                    updateVoxScreen();
                });
                sliders.add(aSlider);
                sliderPositions.add(new WidgetPosition(sliderX + 10, sliderY, 60, 18));
                sliderY += 20;
            }
        }

        // Color Presets
        VoxButton pickColorButton = new VoxButton(theme, sliderX, sliderY, 70, 18, Text.literal("Pick Color"), btn -> {
            showColorPicker = !showColorPicker;
            needsPanelUpdate = true;
        });
        propertyButtons.add(pickColorButton);
        buttonPositions.add(new WidgetPosition(sliderX, sliderY, 70, 18));
        VoxButton saveColorButton = new VoxButton(theme, buttonX, sliderY, 38, 18, Text.literal("Save Color"), btn -> {
            List<int[]> presets = colorPresets.computeIfAbsent(selectedElement.id, k -> new ArrayList<>());
            if (presets.size() < 5) {
                int[] preset = selectedElement.type.equals("window") ?
                        new int[]{selectedElement.r, selectedElement.g, selectedElement.b, selectedElement.titleR, selectedElement.titleG, selectedElement.titleB, selectedElement.borderR, selectedElement.borderG, selectedElement.borderB, selectedElement.borderEnabled ? 1 : 0} :
                        new int[]{selectedElement.r, selectedElement.g, selectedElement.b, selectedElement.alpha};
                presets.add(preset);
            }
        });
        propertyButtons.add(saveColorButton);
        buttonPositions.add(new WidgetPosition(buttonX, sliderY, 38, 18));
        sliderY += 20;

        if (showColorPicker) {
            for (int i = 0; i < 5; i++) {
                int presetIndex = i;
                VoxButton presetButton = new VoxButton(theme, sliderX + i * 22, sliderY, 18, 18, Text.literal(String.valueOf(i + 1)), btn -> {
                    List<int[]> presets = colorPresets.getOrDefault(selectedElement.id, new ArrayList<>());
                    if (presetIndex < presets.size()) {
                        int[] preset = presets.get(presetIndex);
                        int oldR = selectedElement.r, oldG = selectedElement.g, oldB = selectedElement.b;
                        int oldAlpha = selectedElement.alpha;
                        int oldTitleR = selectedElement.titleR, oldTitleG = selectedElement.titleG, oldTitleB = selectedElement.titleB;
                        int oldBorderR = selectedElement.borderR, oldBorderG = selectedElement.borderG, oldBorderB = selectedElement.borderB;
                        boolean oldBorderEnabled = selectedElement.borderEnabled;
                        if (selectedElement.type.equals("window")) {
                            selectedElement.r = preset[0];
                            selectedElement.g = preset[1];
                            selectedElement.b = preset[2];
                            selectedElement.titleR = preset[3];
                            selectedElement.titleG = preset[4];
                            selectedElement.titleB = preset[5];
                            selectedElement.borderR = preset[6];
                            selectedElement.borderG = preset[7];
                            selectedElement.borderB = preset[8];
                            selectedElement.borderEnabled = preset[9] == 1;
                        } else {
                            selectedElement.r = preset[0];
                            selectedElement.g = preset[1];
                            selectedElement.b = preset[2];
                            selectedElement.alpha = preset[3];
                        }
                        applyEdit("color", oldR + "," + oldG + "," + oldB + "," + oldAlpha + "," + oldTitleR + "," + oldTitleG + "," + oldTitleB + "," + oldBorderR + "," + oldBorderG + "," + oldBorderB + "," + oldBorderEnabled,
                                selectedElement.r + "," + selectedElement.g + "," + selectedElement.b + "," + selectedElement.alpha + "," + selectedElement.titleR + "," + selectedElement.titleG + "," + selectedElement.titleB + "," + selectedElement.borderR + "," + selectedElement.borderG + "," + selectedElement.borderB + "," + selectedElement.borderEnabled);
                        updateVoxScreen();
                    }
                });
                propertyButtons.add(presetButton);
                buttonPositions.add(new WidgetPosition(sliderX + i * 22, sliderY, 18, 18));
            }
            sliderY += 20;
        }

        // Snap to Grid
        VoxButton snapButton = new VoxButton(theme, sliderX, sliderY, 70, 18, Text.literal("Snap to Grid: " + (snapToGrid ? "On" : "Off")), btn -> {
            snapToGrid = !snapToGrid;
            btn.setMessage(Text.literal("Snap to Grid: " + (snapToGrid ? "On" : "Off")));
        });
        propertyButtons.add(snapButton);
        buttonPositions.add(new WidgetPosition(sliderX, sliderY, 70, 18));
        sliderY += 20;

        sliders.forEach(this::addDrawableChild);
        propertyButtons.forEach(this::addDrawableChild);

        maxPropertiesOffset = Math.max(0, sliderY - 360);
    }

    private void resetToDefaults() {
        JsonObject before = new JsonObject();
        elements.forEach(e -> before.add(e.id, e.toJson()));
        elements.clear();
        UIElement logo = new UIElement("logo", "image", width / 2 - 50, 10, 100, 50);
        logo.r = 255;
        logo.g = 255;
        logo.b = 255;
        logo.alpha = 128;
        elements.add(logo);
        UIElement search = new UIElement("search", "search", width - 130, 70, 120, 20);
        search.r = 255;
        search.g = 255;
        search.b = 255;
        search.alpha = 255;
        elements.add(search);
        String[] categories = {"Chat", "Combat", "Miscellaneous", "Movement", "Player", "Render", "World"};
        int windowsPerRow = (int) Math.ceil(width / 85.0);
        int windowX = (width - (Math.min(categories.length, windowsPerRow) * 85 - 5)) / 2;
        int windowY = (height - ((int) Math.ceil((double) categories.length / windowsPerRow) * 400 - 5)) / 2;
        int row = 0, col = 0;
        for (String category : categories) {
            UIElement window = new UIElement(category, "window", windowX + col * 85, windowY + row * 400, 80, 400);
            window.r = 255;
            window.g = 255;
            window.b = 255;
            window.titleR = 255;
            window.titleG = 255;
            window.titleB = 255;
            window.borderR = 255;
            window.borderG = 255;
            window.borderB = 255;
            window.borderEnabled = false;
            elements.add(window);
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
        parent.updateLogoColor(255, 255, 255, 128);
        parent.updateSearchPosition(width - 130, 70);
        parent.updateSearchSize(120, 20);
        parent.updateSearchColor(255, 255, 255, 255);
        col = 0;
        row = 0;
        for (String category : categories) {
            parent.updateCategoryPosition(category, windowX + col * 85, windowY + row * 400);
            parent.updateCategorySize(category, 80, 400);
            parent.updateCategoryColor(category, 255, 255, 255, 255, 255, 255);
            parent.updateCategoryBorder(category, 255, 255, 255, false);
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

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        parent.render(context, mouseX, mouseY, delta);

        if (editorMode && !previewMode) {
            if (showCategories) {
                int listY = 30;
                context.fill(15, listY, 165, listY + 200, 0xC0333333);
                context.enableScissor(15, listY, 165, listY + 200);
                for (int i = 0; i < elements.size(); i++) {
                    UIElement element = elements.get(i);
                    int itemY = listY + 10 + i * 20 - elementListOffset;
                    if (itemY >= listY && itemY <= listY + 180) {
                        int textColor = theme.getTextColor();
                        if (mouseX >= 15 && mouseX <= 165 && mouseY >= itemY && mouseY <= itemY + 20) {
                            textColor = theme.getTabActive();
                            context.fill(15, itemY, 165, itemY + 20, theme.getButtonHover());
                        }
                        context.drawTextWithShadow(textRenderer, element.id, 25, itemY, textColor);
                    }
                }
                context.disableScissor();
                context.drawTextWithShadow(textRenderer, "Layers", 25, listY + 205, theme.getTextColor());
            }

            if (showLayers) {
                int listY = 30;
                context.fill(15, listY, 165, listY + 200, 0xC0333333);
                context.enableScissor(15, listY, 165, listY + 200);
                for (int i = 0; i < elements.size(); i++) {
                    UIElement element = elements.get(i);
                    int itemY = listY + 10 + i * 20 - elementListOffset;
                    if (itemY >= listY && itemY <= listY + 180) {
                        int textColor = theme.getTextColor();
                        if (mouseX >= 15 && mouseX <= 165 && mouseY >= itemY && mouseY <= itemY + 20) {
                            textColor = theme.getTabActive();
                            context.fill(15, itemY, 165, itemY + 20, theme.getButtonHover());
                        }
                        context.drawTextWithShadow(textRenderer, element.id, 25, itemY, textColor);
                    }
                }
                context.disableScissor();
            }

            // Preset list
            if (showCategories || showProperties) {
                int listY = 30;
                context.fill(width - 270, listY, width - 170, listY + 200, 0xC0333333);
                context.enableScissor(width - 270, listY, width - 170, listY + 200);
                for (int i = 0; i < presetButtons.size(); i++) {
                    VoxButton button = presetButtons.get(i);
                    int itemY = listY + 10 + i * 20 - presetListOffset;
                    if (itemY >= listY && itemY <= listY + 180) {
                        button.setY(itemY);
                        button.render(context, mouseX, mouseY, delta);
                    }
                }
                context.disableScissor();
            }

            if (showProperties && selectedElement != null) {
                context.fill(15, 30, 165, 430, 0xC0333333);
                context.enableScissor(15, 30, 165, 430);
                context.getMatrices().push();
                context.getMatrices().translate(0, -propertiesOffset, 0);
                int sliderY = 85;
                context.drawTextWithShadow(textRenderer, "Properties: " + selectedElement.id, 25, 40, theme.getTextColor());
                for (int i = 0; i < propertyButtons.size(); i++) {
                    VoxButton button = propertyButtons.get(i);
                    WidgetPosition pos = buttonPositions.get(i);
                    int renderY = pos.y;
                    if (renderY >= 30 + propertiesOffset && renderY <= 430 + propertiesOffset - pos.height) {
                        button.setY(renderY);
                        button.render(context, mouseX, mouseY, delta);
                    }
                }
                for (int i = 0; i < sliders.size(); i++) {
                    SliderWidget slider = sliders.get(i);
                    WidgetPosition pos = sliderPositions.get(i);
                    int renderY = pos.y;
                    if (renderY >= 30 + propertiesOffset && renderY <= 430 + propertiesOffset - pos.height) {
                        slider.setY(renderY);
                        slider.render(context, mouseX, mouseY, delta);
                    }
                }
                context.getMatrices().pop();
                context.disableScissor();
                // Scrollbar
                int barHeight = (int) (400 * (400.0 / (selectedElement.type.equals("window") ? 6 * 20 : 3 * 20)));
                int barY = 30 + (int) ((propertiesOffset / (float) maxPropertiesOffset) * (400 - barHeight));
                context.fill(160, barY, 165, barY + barHeight, 0xFF888888);
            }

            context.drawCenteredTextWithShadow(textRenderer, helpHint, width / 2, height - 20, theme.getTextColor());
        }

        for (VoxButton button : controlButtons) {
            button.render(context, mouseX, mouseY, delta);
        }
        presetNameField.render(context, mouseX, mouseY, delta);

        // Handle deferred panel update
        if (needsPanelUpdate && showProperties && selectedElement != null) {
            needsPanelUpdate = false;
            updatePropertiesPanel();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (editorMode && !previewMode) {
            for (VoxButton btn : controlButtons) {
                if (btn.mouseClicked(mouseX, mouseY, button)) return true;
            }
            for (VoxButton btn : presetButtons) {
                if (btn.mouseClicked(mouseX, mouseY, button)) return true;
            }
            if (presetNameField.mouseClicked(mouseX, mouseY, button)) {
                setFocused(presetNameField);
                return true;
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
                if (mouseX >= 15 && mouseX <= 165 && mouseY >= 235 && mouseY <= 245) {
                    showCategories = false;
                    showLayers = true;
                    helpHint = "Drag to reorder layers, click to return.";
                    init();
                    return true;
                }
                if (mouseX >= 15 && mouseX <= 165 && mouseY >= 30 && mouseY <= 230) {
                    int listY = 30;
                    int index = (int) ((mouseY - listY - 10 + elementListOffset) / 20);
                    if (index >= 0 && index < elements.size()) {
                        selectedElement = elements.get(index);
                        showCategories = false;
                        showProperties = true;
                        showColorPicker = false;
                        helpHint = "Click dropdowns to edit properties.";
                        updatePropertiesPanel();
                        return true;
                    }
                }
            }

            if (showLayers) {
                if (mouseX >= 15 && mouseX <= 165 && mouseY >= 30 && mouseY <= 230) {
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
                if (mouseX >= element.x && mouseX <= element.x + element.width && mouseY >= element.y && mouseY <= element.y + element.height) {
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
                    helpHint = "Click dropdowns to edit properties.";
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
            return true;
        }
        if (editorMode && draggingLayer && draggedLayerIndex >= 0) {
            int listY = 30;
            int newIndex = (int) ((mouseY - listY - 10 + elementListOffset) / 20);
            newIndex = MathHelper.clamp(newIndex, 0, elements.size() - 1);
            if (newIndex != draggedLayerIndex) {
                UIElement movedElement = elements.remove(draggedLayerIndex);
                elements.add(newIndex, movedElement);
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
        if (needsPanelUpdate && showProperties && selectedElement != null) {
            needsPanelUpdate = false;
            updatePropertiesPanel();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (editorMode && !previewMode) {
            if (showCategories && mouseX >= 15 && mouseX <= 165 && mouseY >= 30 && mouseY <= 230) {
                elementListOffset = MathHelper.clamp(elementListOffset - (int) (verticalAmount * 20), 0, maxElementListOffset);
                return true;
            }
            if (showLayers && mouseX >= 15 && mouseX <= 165 && mouseY >= 30 && mouseY <= 230) {
                elementListOffset = MathHelper.clamp(elementListOffset - (int) (verticalAmount * 20), 0, maxElementListOffset);
                return true;
            }
            if (showProperties && mouseX >= 15 && mouseX <= 165 && mouseY >= 30 && mouseY <= 430) {
                propertiesOffset = MathHelper.clamp(propertiesOffset - (int) (verticalAmount * 20), 0, maxPropertiesOffset);
                return true;
            }
            if ((showCategories || showProperties) && mouseX >= width - 270 && mouseX <= width - 170 && mouseY >= 30 && mouseY <= 230) {
                presetListOffset = MathHelper.clamp(presetListOffset - (int) (verticalAmount * 20), 0, maxPresetListOffset);
                updatePresetButtons();
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
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                double oldX = selectedElement.x;
                double newX = selectedElement.x + (shift ? 0.5 : 1);
                if (snapToGrid) newX = Math.round(newX / 10.0) * 10;
                selectedElement.x = Math.min(width - selectedElement.width, newX);
                applyEdit("x", oldX, selectedElement.x);
                updateVoxScreen();
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_UP) {
                double oldY = selectedElement.y;
                double newY = selectedElement.y - (shift ? 0.5 : 1);
                if (snapToGrid) newY = Math.round(newY / 10.0) * 10;
                selectedElement.y = Math.max(0, newY);
                applyEdit("y", oldY, selectedElement.y);
                updateVoxScreen();
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_DOWN) {
                double oldY = selectedElement.y;
                double newY = selectedElement.y + (shift ? 0.5 : 1);
                if (snapToGrid) newY = Math.round(newY / 10.0) * 10;
                selectedElement.y = Math.min(height - selectedElement.height, newY);
                applyEdit("y", oldY, selectedElement.y);
                updateVoxScreen();
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_G && ctrl) {
                double oldX = selectedElement.x, oldY = selectedElement.y;
                selectedElement.x = Math.round(selectedElement.x / 10.0) * 10;
                selectedElement.y = Math.round(selectedElement.y / 10.0) * 10;
                applyEdit("x", oldX, selectedElement.x);
                applyEdit("y", oldY, selectedElement.y);
                updateVoxScreen();
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