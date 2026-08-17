package com.raiiiden.taczadditions.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.raiiiden.taczadditions.client.ZoomHandReach;
import com.tacz.guns.client.model.BedrockAnimatedModel;
import com.tacz.guns.client.model.functional.LeftHandRender;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Lets the off hand travel to the optic while the magnification is being scrolled.
//
// The pose handed to this renderer is already at the lefthand_pos bone, and FunctionalBedrockPart
// pushed it before the call and pops it afterwards, so translating here moves only the arm.
@Mixin(value = LeftHandRender.class, remap = false)
public class LeftHandReachMixin {

    @Shadow
    @Final
    private BedrockAnimatedModel bedrockGunModel;

    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void taczadditions$reachForScope(PoseStack poseStack, VertexConsumer consumer, ItemDisplayContext context,
                                             int light, int overlay, CallbackInfo ci) {
        // Third person draws the same model through a different arm, which this does not apply to.
        if (!context.firstPerson()) return;
        ZoomHandReach.applyReach(poseStack, bedrockGunModel);
    }
}
