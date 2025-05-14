package com.raiiiden.taczadditions.mixin;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.tacz.guns.client.event.CameraSetupEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.event.ViewportEvent;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CameraSetupEvent.class, priority = 1500)
public class NoRecoilRecoveryMixin {

    @Shadow(remap = false)
    private static PolynomialSplineFunction pitchSplineFunction;
    @Shadow(remap = false)
    private static PolynomialSplineFunction yawSplineFunction;
    @Shadow(remap = false)
    private static long shootTimeStamp;
    @Shadow(remap = false)
    private static double xRotO;
    @Shadow(remap = false)
    private static double yRotO;

    /**
     * This mixin applies the recoil delta from the gun's spline function.
     * If recoil recovery is disabled via config (enableRecoilRecovery = false),
     * then negative deltas (which would normally recover the view) are ignored.
     */
    @Inject(method = "applyCameraRecoil", at = @At("HEAD"), cancellable = true, remap = false)
    private static void applyCustomRecoil(ViewportEvent.ComputeCameraAngles event, CallbackInfo ci) {
        boolean recoilRecoveryEnabled = TacZAdditionsConfig.CLIENT.enableRecoilRecovery.get();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        long timeTotal = System.currentTimeMillis() - shootTimeStamp;

        if (!recoilRecoveryEnabled && timeTotal > 150) {
            ci.cancel();
            return;
        }

        if (pitchSplineFunction != null && pitchSplineFunction.isValidPoint(timeTotal)) {
            double newPitchValue = pitchSplineFunction.value(timeTotal);
            double deltaPitch = newPitchValue - xRotO;

            if (!recoilRecoveryEnabled && deltaPitch < 0) {
            } else {
                player.setXRot(player.getXRot() - (float) deltaPitch);
                xRotO = newPitchValue;
            }
        }

        if (yawSplineFunction != null && yawSplineFunction.isValidPoint(timeTotal)) {
            double newYawValue = yawSplineFunction.value(timeTotal);
            double deltaYaw = newYawValue - yRotO;

            if (!recoilRecoveryEnabled && deltaYaw < 0) {
            } else {
                player.setYRot(player.getYRot() - (float) deltaYaw);
                yRotO = newYawValue;
            }
        }
        ci.cancel();
    }
}