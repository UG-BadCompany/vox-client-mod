package com.yourname.vox.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;

public class SearchWindow {
    private final VoxTheme theme;
    private final int x;
    private final int y;
    private final TextFieldWidget searchField;

    public SearchWindow(VoxTheme theme, int x, int y, TextFieldWidget searchField) {
        this.theme = theme;
        this.x = x;
        this.y = y;
        this.searchField = searchField;
        this.searchField.setX(x);
        this.searchField.setY(y);
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        searchField.render(context, mouseX, mouseY, delta);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return searchField.mouseClicked(mouseX, mouseY, button);
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        searchField.mouseReleased(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return searchField.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
}