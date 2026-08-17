package com.raiiiden.taczadditions.mixin;

import com.raiiiden.taczadditions.client.VariableZoomState;
import com.tacz.guns.client.event.CameraSetupEvent;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Scales the scope model along with the swept magnification.
//
// The magnification itself is published by ClientAttachmentIndexZoomMixin, which the world FOV
// already picks up. Only the scope model FOV needs its own hook, because it comes from a second
// array that is indexed by the zoom level rather than derived from the magnification.
//
// This is a redirect rather than a rewrite so TaCZ keeps ownership of the aiming-progress lerp and
// the second-order smoothing on either side of the call.
@Mixin(value = CameraSetupEvent.class, remap = false)
public class VariableZoomFovMixin {

    // Scope model FOV. The surrounding lambda reads viewsFov[zoomNumber % length], so returning a
    // single-element array makes that index resolve to our interpolated value whatever the counter is.
    @Redirect(
            method = "lambda$applyGunModelFovModifying$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/tacz/guns/client/resource/index/ClientAttachmentIndex;getViewsFov()[F"
            ),
            remap = false
    )
    private static float[] taczadditions$variableViewsFov(ClientAttachmentIndex index) {
        float[] original = index.getViewsFov();
        float override = VariableZoomState.viewsFovOverride(index);
        if (Float.isNaN(override)) return original;
        return new float[]{override};
    }
}
