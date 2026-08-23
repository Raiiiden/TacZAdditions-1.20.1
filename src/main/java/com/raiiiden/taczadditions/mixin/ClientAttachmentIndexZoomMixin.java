package com.raiiiden.taczadditions.mixin;

import com.raiiiden.taczadditions.client.VariableZoomState;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Publishes the swept magnification as the scope's zoom array.
// Overriding at the source keeps the world FOV and the aim sensitivity reading the same value.
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
