package com.raiiiden.taczadditions.mixin;

import com.tacz.guns.client.event.FirstPersonRenderGunEvent;
import com.tacz.guns.client.input.AimKey;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonRenderGunEvent.class)
public class CombinedSwayMixin {

    // ----- Translation Sway -----
    // Default stuff
    private static final float DEFAULT_DEFAULT_MULTIPLIER = 1.5F;
    private static final float DEFAULT_AIM_MULTIPLIER = 6.0F;
    private static final float DEFAULT_SMOOTHING_FACTOR = 0.045F;

    //store the last smoothed multiplier (starting with default)
    private static float lastMultiplier = DEFAULT_DEFAULT_MULTIPLIER;

    private static float getTargetMultiplier() {
        float defaultMultiplier = DEFAULT_DEFAULT_MULTIPLIER;
        float aimMultiplier = DEFAULT_AIM_MULTIPLIER;
        try {
            defaultMultiplier = TacZAdditionsConfig.CLIENT.defaultMultiplier.get().floatValue();
        } catch (IllegalStateException e) {
            // Use default if config isn't loaded
        }
        try {
            aimMultiplier = TacZAdditionsConfig.CLIENT.aimMultiplier.get().floatValue();
        } catch (IllegalStateException e) {
        }
        return AimKey.AIM_KEY.isDown() ? aimMultiplier : defaultMultiplier;
    }

    private static float getSmoothedMultiplier(float target) {
        float smoothingFactor = DEFAULT_SMOOTHING_FACTOR;
        try {
            smoothingFactor = TacZAdditionsConfig.CLIENT.smoothingFactor.get().floatValue();
        } catch (IllegalStateException e) {
        }
        lastMultiplier += (target - lastMultiplier) * smoothingFactor;
        return lastMultiplier;
    }

    // ----- Extra Roll (Sideways Tilt) -----
    // Default roll values
    private static final float DEFAULT_HIPFIRE_ROLL_FACTOR = 0.5F;
    private static final float DEFAULT_AIMING_ROLL_FACTOR = 1.5F;
    private static final float DEFAULT_ROLL_SENSITIVITY = 15.0F;
    private static final float DEFAULT_ROLL_SMOOTHING = 0.04F;
    private static final float DEFAULT_MAX_TILT_ANGLE = 30.0F;

    private static float lastPlayerYaw = 0.0F;
    private static float currentRoll = 0.0F;

    /**
     * Inject into applyShootSwayAndRotation at the TAIL to adjust both translation sway and extra roll.
     */
    @Inject(method = "applyShootSwayAndRotation", at = @At("TAIL"), remap = false)
    private static void modifyShootingSway(BedrockGunModel model, float aimingProgress, CallbackInfo ci) {
        // Check if camera sway is enabled. Default is true
        boolean enableCameraSway = true;
        try {
            enableCameraSway = TacZAdditionsConfig.CLIENT.enableCameraSway.get();
        } catch (IllegalStateException e) {
            // use default
        }
        if (!enableCameraSway) {
            return;
        }

        // --- Translation Sway ---
        float targetMultiplier = getTargetMultiplier();
        float smoothedMultiplier = getSmoothedMultiplier(targetMultiplier);
        BedrockPart root = model.getRootNode();
        if (root != null) {
            root.offsetX *= smoothedMultiplier;
            root.offsetY *= smoothedMultiplier;
        }

        // --- Extra Roll (Sideways Tilt) ---
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && root != null) {
            float currentYaw = player.getViewYRot(1.0F);
            float deltaYaw = currentYaw - lastPlayerYaw;
            lastPlayerYaw = currentYaw;

            float rollFactor = (AimKey.AIM_KEY.isDown() ? DEFAULT_AIMING_ROLL_FACTOR : DEFAULT_HIPFIRE_ROLL_FACTOR);
            float rollSensitivity = DEFAULT_ROLL_SENSITIVITY;
            float rollSmoothing = DEFAULT_ROLL_SMOOTHING;
            float maxTilt = DEFAULT_MAX_TILT_ANGLE;
            try {
                rollFactor = AimKey.AIM_KEY.isDown() ? TacZAdditionsConfig.CLIENT.aimingRollFactor.get().floatValue() :
                        TacZAdditionsConfig.CLIENT.hipfireRollFactor.get().floatValue();
            } catch (IllegalStateException e) { }
            try {
                rollSensitivity = TacZAdditionsConfig.CLIENT.rollSensitivity.get().floatValue();
            } catch (IllegalStateException e) { }
            try {
                rollSmoothing = TacZAdditionsConfig.CLIENT.rollSmoothing.get().floatValue();
            } catch (IllegalStateException e) { }
            try {
                maxTilt = TacZAdditionsConfig.CLIENT.maxTiltAngle.get().floatValue();
            } catch (IllegalStateException e) { }

            float targetRoll = -deltaYaw * rollFactor * rollSensitivity;
            currentRoll += (targetRoll - currentRoll) * rollSmoothing;
            if (currentRoll > maxTilt) currentRoll = maxTilt;
            if (currentRoll < -maxTilt) currentRoll = -maxTilt;
            root.additionalQuaternion.rotateZ((float) Math.toRadians(currentRoll));
        }
    }
}