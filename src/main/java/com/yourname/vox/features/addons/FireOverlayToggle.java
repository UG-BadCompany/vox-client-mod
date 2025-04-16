package com.yourname.vox.features.addons;

import com.yourname.vox.IVoxAddon;
import com.yourname.vox.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.text.Text;
import java.util.Arrays;

public class FireOverlayToggle implements IVoxAddon {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public static boolean enabled = false;
    public static String mode = "auto";
    public static boolean shouldHideFireOverlay = false;

    @Override
    public String getName() {
        return "FireOverlayToggle";
    }

    @Override
    public void onEnable() {
        enabled = true;
        mode = "auto";
        updateShouldHide();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("Fire Overlay Toggle enabled"), false);
        }
    }

    @Override
    public void onTick() {
        updateShouldHide();
    }

    @Override
    public void onChat(String msg) {}

    @Override
    public void onRenderWorldLast(float partialTicks) {}

    @Override
    public void toggle() {
        enabled = !enabled;
        ConfigManager.addonToggles.put(getName(), enabled);
        updateShouldHide();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("Fire Overlay Toggle " + (enabled ? "enabled" : "disabled")), false);
        }
    }

    @Override
    public String getDescription() {
        return "Hides the fire overlay when toggled. Modes: auto (with Fire Resistance), always, off.";
    }

    public void setMode(String newMode) {
        if (Arrays.asList("auto", "always", "off").contains(newMode.toLowerCase())) {
            mode = newMode.toLowerCase();
            updateShouldHide();
            ConfigManager.addonSettings.put(getName() + "_mode", mode);
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("Fire Overlay mode set to " + mode), false);
            }
        } else {
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("Invalid mode. Use: auto, always, off"), false);
            }
        }
    }

    private void updateShouldHide() {
        if (mc.player == null || !enabled) {
            shouldHideFireOverlay = false;
        } else if (mode.equals("always")) {
            shouldHideFireOverlay = true;
        } else if (mode.equals("auto")) {
            boolean hasFireResistance = mc.player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE);
            shouldHideFireOverlay = hasFireResistance;
        } else {
            shouldHideFireOverlay = false;
        }
        System.out.println("[Vox] Updated shouldHideFireOverlay: " + shouldHideFireOverlay + ", mode: " + mode + ", enabled: " + enabled + ", hasFireResistance: " + (mc.player != null && mc.player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)));
    }
}