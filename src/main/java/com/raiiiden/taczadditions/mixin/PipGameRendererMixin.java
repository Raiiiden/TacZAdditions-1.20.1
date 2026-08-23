package com.raiiiden.taczadditions.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.raiiiden.taczadditions.pip.PipScopeRenderer;
import com.tacz.guns.compat.oculus.OculusCompat;
import com.tacz.guns.util.math.MathUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Drives the REAL picture-in-picture pass, which is a second whole render of the level, because a
// crop of the finished frame cannot show the detail the magnification is supposed to reveal.
@Mixin(GameRenderer.class)
public abstract class PipGameRendererMixin {

    @Shadow
    public abstract void renderLevel(float partialTick, long finishNanoTime, PoseStack poseStack);

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void taczadditions$renderPipPass(float partialTick, long finishNanoTime, PoseStack poseStack, CallbackInfo ci) {
        // Iris drives renderLevel again for its shadow pass. Recursing there would zoom a camera
        // that is not the player's.
        if (OculusCompat.isRenderShadow()) return;
        if (PipScopeRenderer.isRenderingPip()) return;

        // Ahead of the decision, because the magnification this frame settled on is not published
        // until the field-of-view event further down, and the lens must not trail it.
        PipScopeRenderer.refreshZoom(partialTick);
        if (!PipScopeRenderer.needsRealPipRender()) return;

        // The recursion writes to the main framebuffer and reads its FOV from the hook below.
        // PipItemInHandRendererMixin suppresses the held gun, so only world geometry is captured.
        PipScopeRenderer.beginRealPipRender();
        try {
            this.renderLevel(partialTick, finishNanoTime, new PoseStack());
            PipScopeRenderer.captureAndClearForNormalRender();
        } finally {
            // Normally already a no-op, since the capture clears the flag itself.
            PipScopeRenderer.abortRealPipRender();
        }
    }

    // The scope's own field of view, applied only to the level render and only while the second
    // pass is running.
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void taczadditions$pipFov(Camera camera, float partialTicks, boolean useFovSetting,
                                      CallbackInfoReturnable<Double> cir) {
        if (useFovSetting && PipScopeRenderer.isRenderingPip()) {
            cir.setReturnValue(MathUtil.magnificationToFov(PipScopeRenderer.getCurrentZoom(), cir.getReturnValue()));
        }
    }

    // With a shader pack the lens overlay cannot be drawn inside the level render, because the
    // composite that follows would paint over it. It is replayed here instead, once Iris is done.
    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void taczadditions$replayShaderPip(float partialTick, long finishNanoTime, PoseStack poseStack, CallbackInfo ci) {
        if (OculusCompat.isRenderShadow()) return;
        PipScopeRenderer.renderShaderPip();
    }
}
