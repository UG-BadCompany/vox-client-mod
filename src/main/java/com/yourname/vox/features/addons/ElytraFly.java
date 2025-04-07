package com.yourname.vox.features.addons;

import com.yourname.vox.IVoxAddon;
import com.yourname.vox.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class ElytraFly implements IVoxAddon {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private int flightTicks = 0;
    private boolean wasOnGround = false;

    @Override
    public String getName() {
        return "ElytraFly";
    }

    @Override
    public void onEnable() {
        flightTicks = 0;
        wasOnGround = false;
    }

    @Override
    public void onTick() {
        if (!ConfigManager.addonToggles.getOrDefault(getName(), false) || mc.player == null) return;

        ClientPlayerEntity player = mc.player;
        if (player.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA) {
            player.sendMessage(Text.literal("ElytraFly: Equip an Elytra to use this module"), false);
            ConfigManager.addonToggles.put(getName(), false);
            return;
        }

        // Auto-start flight on jump
        if (player.isOnGround() && !wasOnGround && mc.options.jumpKey.isPressed()) {
            player.startFallFlying();
        }
        wasOnGround = player.isOnGround();

        if (!player.isFallFlying()) return;

        // Flight mechanics
        float baseSpeed = 0.3f;
        float speed = ConfigManager.addonToggles.getOrDefault("Speed", false) ? Math.min(baseSpeed * 2, 1.0f) : baseSpeed;
        float yaw = player.getYaw();
        float pitch = player.getPitch();
        Vec3d motion = player.getVelocity();

        // Apply forward motion based on look direction
        double forwardX = Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
        double forwardZ = Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
        double forwardY = -Math.sin(Math.toRadians(pitch));
        player.setVelocity(
                motion.x + forwardX * speed,
                motion.y + forwardY * speed + 0.05, // Slight upward nudge
                motion.z + forwardZ * speed
        );

        // Anti-kick: Send flight packet every 20 ticks
        flightTicks++;
        if (flightTicks % 20 == 0) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_FALL_FLYING, BlockPos.ORIGIN, Direction.UP));
        }

        // Avoid liquid slowdown
        if (player.isTouchingWater() || player.isInLava()) {
            player.setVelocity(player.getVelocity().add(0, 0.1, 0));
        }
    }

    @Override
    public void onChat(String msg) {
    }

    @Override
    public void onRenderWorldLast(float partialTicks) {
    }

    @Override
    public void toggle() {
        ConfigManager.addonToggles.put(getName(), !ConfigManager.addonToggles.getOrDefault(getName(), false));
        if (!ConfigManager.addonToggles.getOrDefault(getName(), false) && mc.player != null) {
            mc.player.stopFallFlying();
        }
    }

    @Override
    public String getDescription() {
        return "Enables sustained Elytra flight with speed boost and auto-start.";
    }
}