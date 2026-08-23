package com.raiiiden.taczadditions.mixin.oculus.newly;

import com.raiiiden.taczadditions.pip.PipScopeRenderer;
import net.irisshaders.iris.uniforms.SystemTimeUniforms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Keep shader frame-time history tied to the normal view only.
@Mixin(value = SystemTimeUniforms.Timer.class, remap = false)
public abstract class TimerMixin {
    @Inject(method = "beginFrame", at = @At("HEAD"), cancellable = true)
    private void taczadditions$skipPipFrameTime(long frameStartTime, CallbackInfo ci) {
        if (PipScopeRenderer.isIsolatingShaderPipeline()) ci.cancel();
    }
}
