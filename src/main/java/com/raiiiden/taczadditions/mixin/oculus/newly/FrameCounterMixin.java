package com.raiiiden.taczadditions.mixin.oculus.newly;

import com.raiiiden.taczadditions.pip.PipScopeRenderer;
import net.irisshaders.iris.uniforms.SystemTimeUniforms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Do not count the secondary scope view as another shader-pack frame.
@Mixin(value = SystemTimeUniforms.FrameCounter.class, remap = false)
public abstract class FrameCounterMixin {
    @Inject(method = "beginFrame", at = @At("HEAD"), cancellable = true)
    private void taczadditions$skipPipFrame(CallbackInfo ci) {
        if (PipScopeRenderer.isIsolatingShaderPipeline()) ci.cancel();
    }
}
