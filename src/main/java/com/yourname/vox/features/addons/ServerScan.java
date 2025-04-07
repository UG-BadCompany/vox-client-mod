package com.yourname.vox.features.addons;

import com.yourname.vox.IVoxAddon;
import com.yourname.vox.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import java.time.Instant;

public class ServerScan implements IVoxAddon {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    @Override
    public String getName() {
        return "ServerScan";
    }

    @Override
    public void onEnable() {}

    @Override
    public void onTick() {
        if (mc.player != null && ConfigManager.addonToggles.getOrDefault(getName(), false)) {
            for (String k : ConfigManager.addonSettings.keySet()) {
                Object v = ConfigManager.addonSettings.get(k);
                if (k.startsWith(getName())) {
                    mc.player.networkHandler.sendPacket(new ChatMessageC2SPacket(
                            k + ": " + v, Instant.now(), 0L, null, null
                    ));
                }
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
        return "Scans server settings and reports them.";
    }
}