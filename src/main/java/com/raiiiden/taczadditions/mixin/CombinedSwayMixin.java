package com.raiiiden.taczadditions.mixin;

import com.tacz.guns.client.event.FirstPersonRenderGunEvent;
import com.tacz.guns.client.input.AimKey;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonRenderGunEvent.class)
public class CombinedSwayMixin {

    // ----- Translation multipliers -----
    private static final float DEFAULT_MULTIPLIER = 1.5F;
    private static final float AIM_MULTIPLIER = 10.0F;
    private static final float SMOOTHING_FACTOR = 0.04F;
    private static float lastMultiplier = DEFAULT_MULTIPLIER;

    private static float getTargetMultiplier() {
        return AimKey.AIM_KEY.isDown() ? AIM_MULTIPLIER : DEFAULT_MULTIPLIER;
    }

    private static float getSmoothedMultiplier(float target) {
        lastMultiplier += (target - lastMultiplier) * SMOOTHING_FACTOR;
        return lastMultiplier;
    }

    // ----- Extra Roll (Sideways Tilt) -----
    private static final float HIPFIRE_ROLL_FACTOR = 0.5F; // Tilt intensity when not aiming
    private static final float AIMING_ROLL_FACTOR = 1.5F; // Tilt intensity when aiming
    private static final float ROLL_SENSITIVITY = 15.0F;
    private static final float ROLL_SMOOTHING = 0.05F;
    private static final float MAX_TILT_ANGLE = 30.0F;
    private static float lastPlayerYaw = 0.0F;
    private static float currentRoll = 0.0F;

    /**
     * Inject into applyShootSwayAndRotation at the TAIL to adjust both translation sway and extra roll.
     */
    @Inject(method = "applyShootSwayAndRotation", at = @At("TAIL"), remap = false)
    private static void modifyShootingSway(BedrockGunModel model, float aimingProgress, CallbackInfo ci) {
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
            // Get current view yaw
            float currentYaw = player.getViewYRot(1.0F);
            float deltaYaw = currentYaw - lastPlayerYaw; // Compute yaw difference
            lastPlayerYaw = currentYaw; // Store current yaw for next tick

            // Determine the roll factor based on aiming state
            float rollFactor = AimKey.AIM_KEY.isDown() ? AIMING_ROLL_FACTOR : HIPFIRE_ROLL_FACTOR;

            // Compute roll in degrees (negative to invert direction)
            float targetRoll = -deltaYaw * rollFactor * ROLL_SENSITIVITY;

            // Smooth the roll transition
            currentRoll += (targetRoll - currentRoll) * ROLL_SMOOTHING;

            // Clamp roll within max tilt range
            if (currentRoll > MAX_TILT_ANGLE) currentRoll = MAX_TILT_ANGLE;
            if (currentRoll < -MAX_TILT_ANGLE) currentRoll = -MAX_TILT_ANGLE;

            // Apply rotation around Z-axis for tilt effect
            root.additionalQuaternion.rotateZ((float) Math.toRadians(currentRoll));
        }
    }
}