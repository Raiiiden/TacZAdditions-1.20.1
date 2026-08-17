package com.raiiiden.taczadditions.mixin;

import com.raiiiden.taczadditions.client.VariableZoomState;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Publishes the swept magnification as the scope zoom array.
//
// Everything that cares about how far a scope is zoomed reads it from here: the world FOV by way of
// IGun#getAimingZoom, and the aim sensitivity in TaCZ own MouseHandler mixin, which reads this
// getter directly. Overriding at the source keeps those in step, where overriding each use site
// separately left the mouse scaled for a magnification the player was no longer looking through.
@Mixin(value = ClientAttachmentIndex.class, remap = false)
public class ClientAttachmentIndexZoomMixin {

    @Inject(method = "getZoom", at = @At("HEAD"), cancellable = true, remap = false)
    private void taczadditions$continuousZoom(CallbackInfoReturnable<float[]> cir) {
        float[] override = VariableZoomState.zoomArrayOverride((ClientAttachmentIndex) (Object) this);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}
