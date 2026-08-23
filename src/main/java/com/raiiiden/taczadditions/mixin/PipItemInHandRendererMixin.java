package com.raiiiden.taczadditions.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.raiiiden.taczadditions.pip.PipScopeRenderer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Keeps the held gun out of the picture-in-picture pass, where it would sit in front of its own
// optic. Runs ahead of TaCZ's own hook, so its hand-animation event never fires for that pass.
@Mixin(value = ItemInHandRenderer.class, priority = 900)
public class PipItemInHandRendererMixin {

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void taczadditions$skipHandsDuringPip(float partialTicks, PoseStack poseStack,
                                                  MultiBufferSource.BufferSource buffer, LocalPlayer player,
                                                  int combinedLight, CallbackInfo ci) {
        if (PipScopeRenderer.isRenderingPip()) {
            ci.cancel();
            return;
        }
        // The last moment in the frame at which the world stands alone, which is what the cropped
        // lens image has to be taken from.
        PipScopeRenderer.captureFrameIfNeeded();
    }
}
