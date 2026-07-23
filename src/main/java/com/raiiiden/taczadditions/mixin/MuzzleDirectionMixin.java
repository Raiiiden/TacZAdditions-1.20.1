package com.raiiiden.taczadditions.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import com.raiiiden.taczadditions.client.LaserVisibilityCache;
import com.raiiiden.taczadditions.client.MuzzleCache;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GunItemRendererWrapper.class, remap = false)
public class MuzzleDirectionMixin {

    @Inject(
            method = "cacheMuzzlePosition",
            at = @At("TAIL"),
            remap = false
    )
    private static void captureMuzzleForward(PoseStack poseStack, BedrockGunModel gunModel, CallbackInfo ci) {
        // This runs immediately after the first-person model was rendered, while animation-driven
        // bone visibility still represents this gun and this frame.
        LaserVisibilityCache.captureLocal(gunModel);

        if (gunModel.getMuzzleFlashPosPath() == null) {
            MuzzleCache.clear();
            return;
        }

        // Bones are still animated here — we're inside cacheMuzzlePosition,
        // before cleanAnimationTransform. The poseStack still has all the
        // renderFirstPerson setup transforms on it, same as when TACZ walked
        // the path just above us to compute muzzleRenderOffset.
        poseStack.pushPose();
        for (BedrockPart part : gunModel.getMuzzleFlashPosPath()) {
            part.translateAndRotateAndScale(poseStack);
        }
        Matrix4f pose = poseStack.last().pose();
        // m20/m21/m22 = local Z-axis of the barrel bone in camera space
        MuzzleCache.muzzleForwardDirection.set(pose.m20(), pose.m21(), pose.m22());
        poseStack.popPose();
    }
}
