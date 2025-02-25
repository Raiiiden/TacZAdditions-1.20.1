package com.raiiiden.taczadditions.mixin;

import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.GunRecoil;
import com.tacz.guns.resource.pojo.data.gun.GunRecoilKeyFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Arrays;

@Mixin(CommonGunIndex.class)
public class NoRecoilRecoveryMixin {

    /**
     * Modify `checkRecoil(GunData)` to prevent recoil recovery.
     */
    @Inject(method = "checkRecoil", at = @At("HEAD"), cancellable = true, remap = false)
    private static void preventRecoilReset(GunData data, CallbackInfo ci) {
        GunRecoil recoil = data.getRecoil();
        if (recoil == null) return;

        GunRecoilKeyFrame[] pitch = recoil.getPitch();
        GunRecoilKeyFrame[] yaw = recoil.getYaw();

        if (pitch != null) {
            for (GunRecoilKeyFrame keyFrame : pitch) {
                float[] value = keyFrame.getValue();

                // Prevent recovery by setting left and right values equal
                value[1] = value[0];

                // Ensure the keyframe is still valid
                keyFrame.setValue(value);
            }
            Arrays.sort(pitch);
        }

        if (yaw != null) {
            for (GunRecoilKeyFrame keyFrame : yaw) {
                float[] value = keyFrame.getValue();

                // Prevent yaw recovery
                value[1] = value[0];

                keyFrame.setValue(value);
            }
            Arrays.sort(yaw);
        }

        ci.cancel(); // Stop the original function from modifying recoil recovery
    }
}
