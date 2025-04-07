package com.yourname.vox.features.addons;

import com.yourname.vox.IVoxAddon;
import com.yourname.vox.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import java.time.Instant;

public class DupeHelper implements IVoxAddon {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    @Override
    public String getName() {
        return "DupeHelper";
    }

    @Override
    public void onEnable() {}

    @Override
    public void onTick() {
        if (mc.player != null && ConfigManager.addonToggles.getOrDefault(getName(), false)) {
            for (Entity entity : mc.world.getEntitiesByClass(
                    Entity.class, mc.player.getBoundingBox().expand(5.0), e -> true)) {
                if (entity instanceof DonkeyEntity donkey && donkey.isTame()) { // Changed isTamed() to isTame()
                    if (entity.getPos().distanceTo(mc.player.getPos()) < 5) {
                        mc.player.startRiding(entity);
                        break;
                    }
                }
            }
            if (mc.player.getVehicle() instanceof DonkeyEntity) {
                mc.player.networkHandler.sendPacket(new ChatMessageC2SPacket(
                        "/kill", Instant.now(), 0L, null, null
                ));
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
        return "Assists with donkey-based duplication glitches.";
    }
}