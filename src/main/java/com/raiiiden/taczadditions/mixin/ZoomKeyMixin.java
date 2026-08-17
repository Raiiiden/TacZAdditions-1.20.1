package com.raiiiden.taczadditions.mixin;

import com.raiiiden.taczadditions.client.VariableZoomState;
import com.tacz.guns.client.input.ZoomKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Makes the zoom key work on guns with a built-in scope.
//
// TaCZ server-side cycle reads getAttachmentId and never getBuiltInAttachmentId, so the packet this
// key sends is discarded for integrated optics and their extra zoom levels are unreachable. When we
// are handling such a gun the level is advanced client-side instead and the dead packet suppressed.
@Mixin(value = ZoomKey.class, remap = false)
public class ZoomKeyMixin {

    @Inject(method = "doZoomLogic", at = @At("HEAD"), cancellable = true, remap = false)
    private static void taczadditions$builtInScopeZoom(CallbackInfo ci) {
        if (VariableZoomState.handleBuiltInZoomKey()) {
            ci.cancel();
        }
    }
}
