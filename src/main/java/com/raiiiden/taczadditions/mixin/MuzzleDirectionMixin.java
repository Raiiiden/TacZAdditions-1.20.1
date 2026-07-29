package com.raiiiden.taczadditions.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.raiiiden.taczadditions.client.LaserVisibilityCache;
import com.raiiiden.taczadditions.client.MuzzleCache;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
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
        // Capture visibility while the rendered gun's animated bones are current.
        LaserVisibilityCache.captureLocal(gunModel);

        if (gunModel.getMuzzleFlashPosPath() == null) {
            MuzzleCache.clear();
            return;
        }

        // Read the animated muzzle pose before TaCZ clears its transforms.
        poseStack.pushPose();
        for (BedrockPart part : gunModel.getMuzzleFlashPosPath()) {
            part.translateAndRotateAndScale(poseStack);
        }
        Matrix4f pose = poseStack.last().pose();
        // m20/m21/m22 represent the barrel bone's local Z-axis.
        MuzzleCache.muzzleForwardDirection.set(pose.m20(), pose.m21(), pose.m22());
        poseStack.popPose();
    }
}
