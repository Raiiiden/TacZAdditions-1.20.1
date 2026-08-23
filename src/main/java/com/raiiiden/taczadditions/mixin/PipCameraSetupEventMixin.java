package com.raiiiden.taczadditions.mixin;

import com.raiiiden.taczadditions.pip.PipScope;
import com.raiiiden.taczadditions.pip.PipScopeRenderer;
import com.tacz.guns.client.event.CameraSetupEvent;
import com.tacz.guns.util.math.MathUtil;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Hands the magnification to the scope instead of to the screen: the world keeps its normal field
// of view, and the held model is no longer drawn larger as the magnification climbs.
@Mixin(value = CameraSetupEvent.class, remap = false)
public class PipCameraSetupEventMixin {

    // The magnified pass drives its own field of view from GameRenderer, and running the world FOV
    // smoothing a second time for it would leave the normal frame chasing the scope.
    @Inject(method = "applyScopeMagnification", at = @At("HEAD"), cancellable = true, remap = false)
    private static void taczadditions$skipPipPass(CallbackInfo ci) {
        if (PipScopeRenderer.isRenderingPip()) {
            ci.cancel();
        }
    }

    // The local player's branch. The second call site in this method belongs to other entities,
    // whose cameras are never the one looking through the scope.
    @Redirect(
            method = "applyScopeMagnification",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/tacz/guns/util/math/MathUtil;magnificationToFov(DD)D",
                    ordinal = 0
            ),
            remap = false
    )
    private static double taczadditions$captureMagnification(double magnification, double baseFov) {
        // Leaving the base FOV alone is what keeps the world at normal scale around the lens.
        if (PipScope.enabled()) return baseFov;
        return MathUtil.magnificationToFov(magnification, baseFov);
    }

    // Keeps the gun and hands where they are. TaCZ narrows the held-item FOV as the scope zooms to
    // sell a full-screen zoom, but with the magnification inside the lens the model must not move.
    @Redirect(
            method = "applyGunModelFovModifying",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(FFF)F", remap = true),
            remap = false
    )
    private static float taczadditions$keepGunModelScale(float delta, float start, float end) {
        if (PipScope.enabled()) return start;
        return Mth.lerp(delta, start, end);
    }
}
