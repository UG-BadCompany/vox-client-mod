package com.yourname.vox.features.addons;

import com.yourname.vox.IVoxAddon;
import com.yourname.vox.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import java.time.Instant;

public class AutoRespond implements IVoxAddon {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    @Override
    public String getName() {
        return "AutoRespond";
    }

    @Override
    public void onEnable() {}

    @Override
    public void onTick() {}

    @Override
    public void onChat(String msg) {
        if (mc.player != null && ConfigManager.addonToggles.getOrDefault(getName(), false) &&
                ConfigManager.addonSettings.containsKey(getName() + "_trigger_" + msg)) {
            mc.player.networkHandler.sendPacket(new ChatMessageC2SPacket(
                    (String) ConfigManager.addonSettings.get(getName() + "_response_" + msg),
                    Instant.now(), 0L, null, null
            ));
        }
    }

    @Override
    public void onRenderWorldLast(float partialTicks) {}

    @Override
    public void toggle() {
        ConfigManager.addonToggles.put(getName(), !ConfigManager.addonToggles.getOrDefault(getName(), false));
    }

    @Override
    public String getDescription() {
        return "Automatically responds to chat messages.";
    }
}