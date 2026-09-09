package com.raiiiden.taczadditions.mixin;

import com.raiiiden.taczadditions.client.VariableZoomState;
import com.tacz.guns.client.input.ZoomKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Makes the zoom key work on guns with a built-in scope. TaCZ's server-side cycle never reads
// getBuiltInAttachmentId, so for those the level is advanced client-side and the packet dropped.
@Mixin(value = ZoomKey.class, remap = false)
public class ZoomKeyMixin {

    @Inject(method = "doZoomLogic", at = @At("HEAD"), cancellable = true, remap = false)
    private static void taczadditions$builtInScopeZoom(CallbackInfo ci) {
        if (VariableZoomState.handleBuiltInZoomKey()) {
            ci.cancel();
        }
    }
}
