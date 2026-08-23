package com.raiiiden.taczadditions.mixin.oculus.legacy;

import com.raiiiden.taczadditions.pip.PipScopeRenderer;
import net.coderbot.iris.uniforms.SystemTimeUniforms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Legacy Oculus/Iris 1.6.x frame-timer isolation.
@Mixin(value = SystemTimeUniforms.Timer.class, remap = false)
public abstract class TimerMixin {
    @Inject(method = "beginFrame", at = @At("HEAD"), cancellable = true)
    private void taczadditions$skipPipFrameTime(long frameStartTime, CallbackInfo ci) {
        if (PipScopeRenderer.isIsolatingShaderPipeline()) ci.cancel();
    }
}
