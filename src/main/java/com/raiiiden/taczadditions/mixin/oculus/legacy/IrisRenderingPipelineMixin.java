package com.raiiiden.taczadditions.mixin.oculus.legacy;

import com.raiiiden.taczadditions.pip.PipScopeRenderer;
import com.raiiiden.taczadditions.pip.ShaderHistorySnapshot;
import net.coderbot.iris.gl.buffer.ShaderStorageBuffer;
import net.coderbot.iris.gl.buffer.ShaderStorageBufferHolder;
import net.coderbot.iris.gl.image.GlImage;
import net.coderbot.iris.pipeline.newshader.NewWorldRenderingPipeline;
import net.coderbot.iris.rendertarget.RenderTarget;
import net.coderbot.iris.rendertarget.RenderTargets;
import net.coderbot.iris.shadows.ShadowRenderTargets;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// Legacy Oculus/Iris 1.6.x shader-history isolation for REAL PIP.
@Mixin(value = NewWorldRenderingPipeline.class, remap = false)
public abstract class IrisRenderingPipelineMixin {
    @Shadow @Final private RenderTargets renderTargets;
    @Shadow private Set<GlImage> customImages;
    @Shadow private ShaderStorageBufferHolder shaderStorageBufferHolder;
    @Shadow private ShadowRenderTargets shadowRenderTargets;
    @Unique private List<ShaderHistorySnapshot.TextureRef> taczadditions$pipTextures;
    @Unique private List<ShaderHistorySnapshot.BufferRef> taczadditions$pipBuffers;
    @Unique private boolean[] taczadditions$normalShadowFlipped;
    @Unique private boolean[] taczadditions$pipShadowFlipped;
    @Unique private boolean taczadditions$normalFullClear;
    @Unique private boolean taczadditions$normalTranslucentDepthDirty;
    @Unique private boolean taczadditions$normalHandDepthDirty;
    @Unique private boolean taczadditions$pipFullClear;
    @Unique private boolean taczadditions$pipTranslucentDepthDirty;
    @Unique private boolean taczadditions$pipHandDepthDirty;
    @Unique private boolean taczadditions$pipTargetFlagsValid;
    @Unique private boolean taczadditions$normalShadowFullClear;
    @Unique private boolean taczadditions$normalShadowTranslucentDepthDirty;
    @Unique private boolean taczadditions$pipShadowFullClear;
    @Unique private boolean taczadditions$pipShadowTranslucentDepthDirty;
    @Unique private boolean taczadditions$pipShadowFlagsValid;

    @Inject(method = "beginLevelRendering", at = @At("HEAD"))
    private void taczadditions$snapshotBeforePip(CallbackInfo ci) {
        if (!PipScopeRenderer.isIsolatingShaderPipeline()) return;

        RenderTargetsAccessor targetState = (RenderTargetsAccessor) renderTargets;
        taczadditions$normalFullClear = targetState.taczadditions$isFullClearRequired();
        taczadditions$normalTranslucentDepthDirty = targetState.taczadditions$isTranslucentDepthDirty();
        taczadditions$normalHandDepthDirty = targetState.taczadditions$isHandDepthDirty();

        List<ShaderHistorySnapshot.TextureRef> textures = new ArrayList<>();
        for (int i = 0; i < renderTargets.getRenderTargetCount(); i++) {
            RenderTarget target = renderTargets.get(i);
            if (target == null) continue;
            textures.add(new ShaderHistorySnapshot.TextureRef(target.getMainTexture(), GL11.GL_TEXTURE_2D));
            textures.add(new ShaderHistorySnapshot.TextureRef(target.getAltTexture(), GL11.GL_TEXTURE_2D));
        }
        textures.add(new ShaderHistorySnapshot.TextureRef(
                renderTargets.getDepthTextureNoTranslucents().getTextureId(), GL11.GL_TEXTURE_2D));
        textures.add(new ShaderHistorySnapshot.TextureRef(
                renderTargets.getDepthTextureNoHand().getTextureId(), GL11.GL_TEXTURE_2D));
        if (shadowRenderTargets != null) {
            for (int i = 0; i < shadowRenderTargets.getRenderTargetCount(); i++) {
                RenderTarget target = shadowRenderTargets.get(i);
                if (target == null) continue;
                textures.add(new ShaderHistorySnapshot.TextureRef(target.getMainTexture(), GL11.GL_TEXTURE_2D));
                textures.add(new ShaderHistorySnapshot.TextureRef(target.getAltTexture(), GL11.GL_TEXTURE_2D));
            }
            textures.add(new ShaderHistorySnapshot.TextureRef(
                    shadowRenderTargets.getDepthTexture().getTextureId(), GL11.GL_TEXTURE_2D));
            textures.add(new ShaderHistorySnapshot.TextureRef(
                    shadowRenderTargets.getDepthTextureNoTranslucents().getTextureId(), GL11.GL_TEXTURE_2D));
            ShadowRenderTargetsAccessor shadowState = (ShadowRenderTargetsAccessor) shadowRenderTargets;
            boolean[] flipped = shadowState.taczadditions$getFlipped();
            taczadditions$normalShadowFlipped = flipped.clone();
            taczadditions$normalShadowFullClear = shadowState.taczadditions$isFullClearRequired();
            taczadditions$normalShadowTranslucentDepthDirty = shadowState.taczadditions$isTranslucentDepthDirty();
        }
        if (customImages != null) {
            for (GlImage image : customImages) {
                textures.add(new ShaderHistorySnapshot.TextureRef(image.getId(), image.getTarget().getGlType()));
            }
        }

        List<ShaderHistorySnapshot.BufferRef> buffers = new ArrayList<>();
        if (shaderStorageBufferHolder != null) {
            ShaderStorageBuffer[] shaderBuffers =
                    ((ShaderStorageBufferHolderAccessor) shaderStorageBufferHolder).taczadditions$getBuffers();
            if (shaderBuffers != null) {
                for (ShaderStorageBuffer buffer : shaderBuffers) {
                    if (buffer != null) {
                        buffers.add(new ShaderHistorySnapshot.BufferRef(buffer.getId(), buffer.getSize()));
                    }
                }
            }
        }

        taczadditions$pipTextures = textures;
        taczadditions$pipBuffers = buffers;
        PipScopeRenderer.beginShaderHistoryIsolation(ShaderHistorySnapshot.capture(textures, buffers));
        if (taczadditions$pipTargetFlagsValid) {
            targetState.taczadditions$setFullClearRequired(taczadditions$pipFullClear);
            targetState.taczadditions$setTranslucentDepthDirty(taczadditions$pipTranslucentDepthDirty);
            targetState.taczadditions$setHandDepthDirty(taczadditions$pipHandDepthDirty);
        }
        if (shadowRenderTargets != null && taczadditions$pipShadowFlipped != null) {
            ShadowRenderTargetsAccessor shadowState = (ShadowRenderTargetsAccessor) shadowRenderTargets;
            boolean[] flipped = shadowState.taczadditions$getFlipped();
            if (flipped.length == taczadditions$pipShadowFlipped.length) {
                System.arraycopy(taczadditions$pipShadowFlipped, 0, flipped, 0, flipped.length);
            }
            if (taczadditions$pipShadowFlagsValid) {
                shadowState.taczadditions$setFullClearRequired(taczadditions$pipShadowFullClear);
                shadowState.taczadditions$setTranslucentDepthDirty(taczadditions$pipShadowTranslucentDepthDirty);
            }
        }
    }

    @Inject(method = "beginHand", at = @At("HEAD"), cancellable = true)
    private void taczadditions$preserveCenterDepthHistory(CallbackInfo ci) {
        if (PipScopeRenderer.isIsolatingShaderPipeline()) ci.cancel();
    }

    @Inject(method = "finalizeLevelRendering", at = @At("RETURN"))
    private void taczadditions$restoreAfterPip(CallbackInfo ci) {
        if (PipScopeRenderer.isIsolatingShaderPipeline()) {
            ShaderHistorySnapshot pipSnapshot = ShaderHistorySnapshot.capture(taczadditions$pipTextures, taczadditions$pipBuffers);
            RenderTargetsAccessor targetState = (RenderTargetsAccessor) renderTargets;
            taczadditions$pipFullClear = targetState.taczadditions$isFullClearRequired();
            taczadditions$pipTranslucentDepthDirty = targetState.taczadditions$isTranslucentDepthDirty();
            taczadditions$pipHandDepthDirty = targetState.taczadditions$isHandDepthDirty();
            taczadditions$pipTargetFlagsValid = true;
            targetState.taczadditions$setFullClearRequired(taczadditions$normalFullClear);
            targetState.taczadditions$setTranslucentDepthDirty(taczadditions$normalTranslucentDepthDirty);
            targetState.taczadditions$setHandDepthDirty(taczadditions$normalHandDepthDirty);
            if (shadowRenderTargets != null) {
                ShadowRenderTargetsAccessor shadowState = (ShadowRenderTargetsAccessor) shadowRenderTargets;
                boolean[] flipped = shadowState.taczadditions$getFlipped();
                taczadditions$pipShadowFlipped = flipped.clone();
                taczadditions$pipShadowFullClear = shadowState.taczadditions$isFullClearRequired();
                taczadditions$pipShadowTranslucentDepthDirty = shadowState.taczadditions$isTranslucentDepthDirty();
                taczadditions$pipShadowFlagsValid = true;
                if (taczadditions$normalShadowFlipped != null && flipped.length == taczadditions$normalShadowFlipped.length) {
                    System.arraycopy(taczadditions$normalShadowFlipped, 0, flipped, 0, flipped.length);
                }
                shadowState.taczadditions$setFullClearRequired(taczadditions$normalShadowFullClear);
                shadowState.taczadditions$setTranslucentDepthDirty(taczadditions$normalShadowTranslucentDepthDirty);
            }
            taczadditions$normalShadowFlipped = null;
            taczadditions$pipTextures = null;
            taczadditions$pipBuffers = null;
            PipScopeRenderer.finishShaderHistoryIsolation(pipSnapshot);
        }
    }
}
