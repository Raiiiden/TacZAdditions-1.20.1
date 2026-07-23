package com.raiiiden.taczadditions.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.raiiiden.taczadditions.client.ClientLaserToggleState;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.model.BedrockAttachmentModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BedrockAttachmentModel.class, remap = false)
public class BedrockAttachmentModelLaserToggleMixin {
    @Unique
    private BedrockPart taczadditions$hiddenLaserRoot;
    @Unique
    private boolean taczadditions$savedLaserVisibility;

    @Inject(method = "render", at = @At("HEAD"))
    private void hideDisabledAttachmentLaser(ItemStack attachmentStack, ItemStack gunStack,
                                             PoseStack poseStack, ItemDisplayContext context,
                                             RenderType renderType, int light, int overlay,
                                             CallbackInfo ci) {
        ItemStack safeGunStack = gunStack == null ? ItemStack.EMPTY : gunStack;
        ClientLaserToggleState.beginAttachmentRender(safeGunStack);
        taczadditions$hiddenLaserRoot = null;
        if (!taczadditions$isEquippedLaser(attachmentStack, safeGunStack)) return;

        BedrockPart root = ((BedrockAttachmentModel) (Object) this).getRootNode();
        if (ClientLaserToggleState.isLaserEnabled(safeGunStack)) return;
        if (root != null) {
            taczadditions$hiddenLaserRoot = root;
            taczadditions$savedLaserVisibility = root.visible;
            root.visible = false;
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void restoreAttachmentLaser(ItemStack attachmentStack, ItemStack gunStack,
                                        PoseStack poseStack, ItemDisplayContext context,
                                        RenderType renderType, int light, int overlay,
                                        CallbackInfo ci) {
        if (taczadditions$hiddenLaserRoot != null) {
            taczadditions$hiddenLaserRoot.visible = taczadditions$savedLaserVisibility;
            taczadditions$hiddenLaserRoot = null;
        }
        ClientLaserToggleState.endAttachmentRender();
    }

    @Unique
    private static boolean taczadditions$isEquippedLaser(ItemStack attachmentStack, ItemStack gunStack) {
        if (attachmentStack == null || attachmentStack.isEmpty()
                || gunStack == null || gunStack.isEmpty()) return false;
        if (!(gunStack.getItem() instanceof IGun gun)) return false;

        ItemStack laser = gun.getAttachment(gunStack, AttachmentType.LASER);
        if (laser.isEmpty()) {
            laser = gun.getBuiltinAttachment(gunStack, AttachmentType.LASER);
        }
        return !laser.isEmpty() && ItemStack.isSameItemSameTags(laser, attachmentStack);
    }

}
