package com.raiiiden.taczadditions.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.raiiiden.taczadditions.client.ClientLaserToggleState;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.model.functional.BeamRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = BeamRenderer.class, remap = false)
public class BeamRendererMixin {
    @Inject(method = "renderLaserBeam", at = @At("HEAD"), cancellable = true)
    private static void hideDisabledLaserBeam(ItemStack gunStack, PoseStack poseStack,
                                              ItemDisplayContext context, List<BedrockPart> path,
                                              CallbackInfo ci) {
        if (!ClientLaserToggleState.isRenderedLaserEnabled(gunStack)) ci.cancel();
    }
}
