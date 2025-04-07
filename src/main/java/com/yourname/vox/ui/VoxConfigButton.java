package com.yourname.vox.ui;

public class VoxConfigButton extends VoxButton {
    private final Runnable action;

    public VoxConfigButton(VoxTheme theme, String label, Runnable action) {
        super(theme, label);
        this.action = action;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        action.run();
        super.onClick(mouseX, mouseY);
    }
}