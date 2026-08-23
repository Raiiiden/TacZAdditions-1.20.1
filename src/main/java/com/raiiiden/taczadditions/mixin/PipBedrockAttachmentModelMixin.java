package com.raiiiden.taczadditions.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.raiiiden.taczadditions.pip.PipLensRenderer;
import com.raiiiden.taczadditions.pip.PipScope;
import com.tacz.guns.client.model.BedrockAnimatedModel;
import com.tacz.guns.client.model.BedrockAttachmentModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.compat.ar.ARCompat;
import com.tacz.guns.compat.oculus.OculusCompat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

// Turns the scope's ocular pass into a picture-in-picture lens. TaCZ already carves the aperture,
// and only that pass is swapped, so with the feature off this model renders as it always did.
@Mixin(value = BedrockAttachmentModel.class, remap = false)
public class PipBedrockAttachmentModelMixin {

    @Shadow
    protected List<List<BedrockPart>> ocularNodePaths;

    @Shadow
    protected List<Boolean> isScopeOcular;

    @Shadow
    protected List<List<BedrockPart>> divisionNodePaths;

    @Shadow
    private float scopeViewRadiusModifier;

    @Shadow
    private boolean isScope;

    @Shadow
    private boolean isSight;

    @Shadow
    private void renderTempPart(PoseStack poseStack, ItemDisplayContext transformType, RenderType renderType,
                                int light, int overlay, List<BedrockPart> path) {
        throw new AssertionError();
    }

    @Shadow
    private void renderOcularStencil(PoseStack poseStack, ItemDisplayContext transformType, RenderType renderType,
                                     int light, int overlay, boolean selective) {
        throw new AssertionError();
    }

    @Shadow
    private void renderOcularAndDivision(PoseStack poseStack, ItemDisplayContext transformType, RenderType renderType,
                                         int light, int overlay, boolean selective) {
        throw new AssertionError();
    }

    @Inject(method = "renderScope", at = @At("HEAD"), remap = false)
    private void taczadditions$beforeScope(PoseStack matrixStack, ItemDisplayContext transformType,
                                           RenderType renderType, int light, int overlay, CallbackInfo ci) {
        // The accelerated path returns immediately and never reaches the stencil work below.
        if (ARCompat.shouldAccelerate()) return;
        // TaCZ clears the stencil buffer next, and a clear obeys whatever write mask is in force.
        RenderSystem.stencilMask(0xFF);
    }

    // Under a shader pack the meshes that exist only to write stencil can be flushed again later as
    // visible geometry, so they are kept out of the draw entirely and replayed after the composite.
    @Redirect(
            method = "renderScope",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/tacz/guns/client/model/BedrockAttachmentModel;renderOcularStencil(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;IIZ)V"
            ),
            remap = false
    )
    private void taczadditions$ocularStencil(BedrockAttachmentModel self, PoseStack matrixStack,
                                             ItemDisplayContext transformType, RenderType renderType,
                                             int light, int overlay, boolean selective) {
        if (taczadditions$shaderDefer()) {
            PipLensRenderer.hideOcularLeaves(ocularNodePaths);
            return;
        }
        renderOcularStencil(matrixStack, transformType, renderType, light, overlay, selective);
    }

    @Redirect(
            method = "renderScope",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/tacz/guns/client/model/BedrockAttachmentModel;renderOcularAndDivision(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;IIZ)V"
            ),
            remap = false
    )
    private void taczadditions$ocularAndDivision(BedrockAttachmentModel self, PoseStack matrixStack,
                                                 ItemDisplayContext transformType, RenderType renderType,
                                                 int light, int overlay, boolean selective) {
        if (!PipScope.enabled()) {
            renderOcularAndDivision(matrixStack, transformType, renderType, light, overlay, selective);
            return;
        }
        PipLensRenderer.render(matrixStack, transformType, renderType, light, overlay, selective,
                ocularNodePaths, isScopeOcular, divisionNodePaths, scopeViewRadiusModifier,
                this::renderTempPart);
    }

    // The remainder of the model is drawn next with the stencil test off, so the mask has to be open
    // again and the deferred meshes have to be out of the way before that happens.
    @Inject(
            method = "renderScope",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/tacz/guns/client/model/BedrockAnimatedModel;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;II)V",
                    shift = At.Shift.BEFORE
            ),
            remap = false
    )
    private void taczadditions$beforeRemainder(PoseStack matrixStack, ItemDisplayContext transformType,
                                               RenderType renderType, int light, int overlay, CallbackInfo ci) {
        RenderSystem.stencilMask(0xFF);
        if (taczadditions$shaderDefer()) {
            PipLensRenderer.hideOcularLeaves(ocularNodePaths);
        }
    }

    // The first-person scope and sight renderers each draw the whole model themselves, so the draw
    // after them is a second copy that doubles the lens work, which shows plainly under shaders.
    @Redirect(
            method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/tacz/guns/client/model/BedrockAnimatedModel;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;II)V"
            ),
            remap = false
    )
    private void taczadditions$skipDuplicateModel(BedrockAnimatedModel self, PoseStack matrixStack,
                                                  ItemDisplayContext transformType, RenderType renderType,
                                                  int light, int overlay) {
        if (PipScope.enabled() && transformType.firstPerson() && (isScope || isSight)) {
            return;
        }
        self.render(matrixStack, transformType, renderType, light, overlay);
    }

    @Unique
    private boolean taczadditions$shaderDefer() {
        return PipScope.enabled() && OculusCompat.isUsingRenderPack();
    }
}
