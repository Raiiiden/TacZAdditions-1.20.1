package com.raiiiden.taczadditions.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.raiiiden.taczadditions.client.ClientLaserToggleState;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = BedrockGunModel.class, remap = false)
public class BedrockGunModelLaserToggleMixin {
    @Unique
    private Boolean taczadditions$savedLaserVisibility;

    @Inject(method = "render", at = @At("HEAD"))
    private void hideDisabledLaserBone(PoseStack poseStack, ItemStack gunStack,
                                       ItemDisplayContext context, RenderType renderType,
                                       int light, int overlay, CallbackInfo ci) {
        taczadditions$savedLaserVisibility = null;
        if (ClientLaserToggleState.isLaserEnabled(gunStack)) return;

        List<BedrockPart> path = ((BedrockGunModelAccessor) this).taczadditions$getLaserBeamPaths();
        if (path == null || path.isEmpty()) return;

        BedrockPart laserBone = path.get(path.size() - 1);
        taczadditions$savedLaserVisibility = laserBone.visible;
        laserBone.visible = false;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void restoreLaserBoneVisibility(PoseStack poseStack, ItemStack gunStack,
                                            ItemDisplayContext context, RenderType renderType,
                                            int light, int overlay, CallbackInfo ci) {
        if (taczadditions$savedLaserVisibility == null) return;

        List<BedrockPart> path = ((BedrockGunModelAccessor) this).taczadditions$getLaserBeamPaths();
        if (path != null && !path.isEmpty()) {
            path.get(path.size() - 1).visible = taczadditions$savedLaserVisibility;
        }
        taczadditions$savedLaserVisibility = null;
    }
}
