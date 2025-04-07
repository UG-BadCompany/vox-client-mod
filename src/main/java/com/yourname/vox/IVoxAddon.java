package com.yourname.vox;

public interface IVoxAddon {
    String getName();
    void onEnable();
    void onTick();
    void onChat(String msg);
    void onRenderWorldLast(float partialTicks);
    void toggle();
    String getDescription();
}