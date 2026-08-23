package com.raiiiden.taczadditions.mixin;

import com.raiiiden.taczadditions.client.VariableZoomState;
import com.tacz.guns.client.event.CameraSetupEvent;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Scales the scope model along with the swept magnification, which needs its own hook because it
// comes from a second array. A redirect, so TaCZ keeps its aiming lerp and smoothing either side.
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
