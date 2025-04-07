package com.yourname.vox.features.addons;

import com.yourname.vox.IVoxAddon;
import com.yourname.vox.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class AntiAnticheat implements IVoxAddon {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    @Override
    public String getName() {
        return "AntiAnticheat";
    }

    @Override
    public void onEnable() {}

    @Override
    public void onTick() {
        if (mc.player != null && mc.world != null && ConfigManager.addonToggles.getOrDefault(getName(), false)) {
            double spoofX = mc.player.getX() + (Math.random() * 0.01 - 0.005);
            double spoofY = mc.player.getY();
            double spoofZ = mc.player.getZ() + (Math.random() * 0.01 - 0.005);
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    spoofX, spoofY, spoofZ, mc.player.isOnGround()
            ));
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
        return "Spoofs small movements to evade anticheat detection.";
    }
}