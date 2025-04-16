package com.yourname.vox.mixin;

import com.yourname.vox.features.addons.FireOverlayToggle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InGameOverlayRenderer.class, priority = 2000)
public class InGameOverlayRendererMixin {
    static {
        System.out.println("[Vox] InGameOverlayRendererMixin class loaded");
    }

    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void renderFireOverlay(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        System.out.println("[Vox] Mixin injected: checking fire overlay, shouldHideFireOverlay=" + FireOverlayToggle.shouldHideFireOverlay);
        if (client.player != null && FireOverlayToggle.shouldHideFireOverlay) {
            System.out.println("[Vox] Suppressing fire overlay for player");
            ci.cancel();
        }
    }
}