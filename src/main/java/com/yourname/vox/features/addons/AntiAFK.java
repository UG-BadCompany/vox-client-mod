package com.yourname.vox.features.addons;

import com.yourname.vox.IVoxAddon;
import com.yourname.vox.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import java.time.Instant;

public class AntiAFK implements IVoxAddon {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private int tickCounter = 0;

    @Override
    public String getName() {
        return "AntiAFK";
    }

    @Override
    public void onEnable() {
        tickCounter = 0;
    }

    @Override
    public void onTick() {
        if (mc.player != null && ConfigManager.addonToggles.getOrDefault(getName(), false)) {
            tickCounter++;
            if (tickCounter >= 6000) { // Every 5 minutes
                mc.player.networkHandler.sendPacket(new ChatMessageC2SPacket(
                        "/ping", Instant.now(), 0L, null, null
                ));
                tickCounter = 0;
            }
        }
    }

    @Override
    public void onChat(String msg) {}

    @Override
    public void onRenderWorldLast(float partialTicks) {}

    @Override
    public void toggle() {
        ConfigManager.addonToggles.put(getName(), !ConfigManager.addonToggles.getOrDefault(getName(), false));
    }

    @Override
    public String getDescription() {
        return "Prevents AFK kicks by sending periodic commands.";
    }
}