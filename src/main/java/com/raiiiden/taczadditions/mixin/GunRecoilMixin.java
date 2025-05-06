package com.raiiiden.taczadditions.mixin;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.tacz.guns.resource.pojo.data.gun.GunRecoil;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GunRecoil.class)
public class GunRecoilMixin {

    @ModifyVariable(
            method = "getSplineFunction",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true,
            remap = false
    )
    private float applyConfigMultiplier(float modifier) {
        float configMult = TacZAdditionsConfig.CLIENT.recoilCameraMultiplier.get().floatValue();
        return modifier * configMult;
    }
}
