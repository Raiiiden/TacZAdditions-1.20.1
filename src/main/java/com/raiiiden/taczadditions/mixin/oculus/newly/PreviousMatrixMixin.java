package com.raiiiden.taczadditions.mixin.oculus.newly;

import com.raiiiden.taczadditions.pip.PipScopeRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

// Prevent the scope projection from becoming the normal frame's previous matrix.
@Mixin(targets = "net.irisshaders.iris.uniforms.MatrixUniforms$Previous", remap = false)
public abstract class PreviousMatrixMixin {
    @Shadow private Supplier<Matrix4f> parent;

    @Inject(method = "get()Lorg/joml/Matrix4f;", at = @At("HEAD"), cancellable = true)
    private void taczadditions$useCurrentAsPreviousForPip(CallbackInfoReturnable<Matrix4f> cir) {
        if (PipScopeRenderer.isIsolatingShaderPipeline()) {
            cir.setReturnValue(new Matrix4f(parent.get()));
        }
    }
}
