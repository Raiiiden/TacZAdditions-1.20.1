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

import java.util.ArrayList;
import java.util.List;

@Mixin(value = BedrockAttachmentModel.class, remap = false)
public class BedrockAttachmentModelLaserToggleMixin {
    @Unique
    private List<BedrockPart> taczadditions$hiddenLaserParts;
    @Unique
    private List<Boolean> taczadditions$savedLaserVisibility;

    @Inject(method = "render", at = @At("HEAD"))
    private void hideDisabledAttachmentLaser(ItemStack attachmentStack, ItemStack gunStack,
                                             PoseStack poseStack, ItemDisplayContext context,
                                             RenderType renderType, int light, int overlay,
                                             CallbackInfo ci) {
        ItemStack safeGunStack = gunStack == null ? ItemStack.EMPTY : gunStack;
        ClientLaserToggleState.beginAttachmentRender(safeGunStack);
        taczadditions$hiddenLaserParts = null;
        taczadditions$savedLaserVisibility = null;
        if (!taczadditions$isEquippedLaser(attachmentStack, safeGunStack)) return;

        if (ClientLaserToggleState.isLaserEnabled(safeGunStack)) return;
        BedrockPart root = ((BedrockAttachmentModel) (Object) this).getRootNode();
        if (root != null) {
            taczadditions$hiddenLaserParts = new ArrayList<>();
            taczadditions$savedLaserVisibility = new ArrayList<>();
            taczadditions$hideLaserIndicators(root);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void restoreAttachmentLaser(ItemStack attachmentStack, ItemStack gunStack,
                                        PoseStack poseStack, ItemDisplayContext context,
                                        RenderType renderType, int light, int overlay,
                                        CallbackInfo ci) {
        if (taczadditions$hiddenLaserParts != null && taczadditions$savedLaserVisibility != null) {
            for (int i = 0; i < taczadditions$hiddenLaserParts.size(); i++) {
                taczadditions$hiddenLaserParts.get(i).visible =
                        taczadditions$savedLaserVisibility.get(i);
            }
        }
        taczadditions$hiddenLaserParts = null;
        taczadditions$savedLaserVisibility = null;
        ClientLaserToggleState.endAttachmentRender();
    }

    @Unique
    private void taczadditions$hideLaserIndicators(BedrockPart part) {
        String name = part.name;
        if ("laser_beam".equalsIgnoreCase(name)
                || "laser_illuminated".equalsIgnoreCase(name)
                || "light_illuminated".equalsIgnoreCase(name)) {
            taczadditions$hiddenLaserParts.add(part);
            taczadditions$savedLaserVisibility.add(part.visible);
            part.visible = false;
        }

        for (BedrockPart child : part.children) {
            taczadditions$hideLaserIndicators(child);
        }
    }

    @Unique
    private static boolean taczadditions$isEquippedLaser(ItemStack attachmentStack, ItemStack gunStack) {
        if (attachmentStack == null || attachmentStack.isEmpty()
                || gunStack == null || gunStack.isEmpty()) return false;
        if (!(gunStack.getItem() instanceof IGun gun)) return false;

        ItemStack laser = gun.getAttachment(gunStack, AttachmentType.LASER);
        if (laser == null || laser.isEmpty()) {
            laser = gun.getBuiltinAttachment(gunStack, AttachmentType.LASER);
        }
        return laser != null && !laser.isEmpty()
                && ItemStack.isSameItemSameTags(laser, attachmentStack);
    }

}
