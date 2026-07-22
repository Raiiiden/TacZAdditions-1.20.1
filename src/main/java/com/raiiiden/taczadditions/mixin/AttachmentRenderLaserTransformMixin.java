package com.raiiiden.taczadditions.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.raiiiden.taczadditions.client.LaserAttachmentTransformCache;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.functional.AttachmentRender;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AttachmentRender.class, remap = false)
public class AttachmentRenderLaserTransformMixin {
    @Shadow
    @Final
    private BedrockGunModel bedrockGunModel;

    @Shadow
    @Final
    private AttachmentType type;

    @Inject(method = "render", at = @At("HEAD"))
    private void captureLaserAttachmentPoint(PoseStack poseStack, VertexConsumer buffer,
                                             ItemDisplayContext context, int light, int overlay,
                                             CallbackInfo ci) {
        if (type != AttachmentType.LASER || !context.firstPerson()) return;

        ItemStack laserStack = bedrockGunModel.getCurrentAttachmentItem().get(AttachmentType.LASER);
        ItemStack gunStack = bedrockGunModel.getCurrentGunItem();
        if (laserStack == null || laserStack.isEmpty() || gunStack.isEmpty()) return;

        // This pose is the animated gun's LASER attachment bone. Capture it before TaCZ applies
        // its attachment-model alignment translation and the model's internal root pivots.
        LaserAttachmentTransformCache.capture(gunStack, poseStack.last().pose());
    }
}
