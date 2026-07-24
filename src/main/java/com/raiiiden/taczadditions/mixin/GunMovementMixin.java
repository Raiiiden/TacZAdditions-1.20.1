package com.raiiiden.taczadditions.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.raiiiden.taczadditions.client.GunRecoilHandler;
import com.raiiiden.taczadditions.client.GunTuckHandler;
import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import com.tacz.guns.client.resource.GunDisplayInstance;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GunItemRendererWrapper.class, remap = false)
public class GunMovementMixin {

    private static final float DEFAULT_PITCH_SENSITIVITY = 0.35F;
    private static final float DEFAULT_YAW_SENSITIVITY = 0.3F;
    private static final float DEFAULT_ROLL_SENSITIVITY = 1.2F;

    private static final float DEFAULT_DRAG_SMOOTHING = 0.15F;
    private static final float DEFAULT_DECAY_FACTOR = 0.84F;
    private static final float DEFAULT_MOMENTUM_FACTOR = 0.45F;

    private static final float DEFAULT_HIP_YAW_MULTIPLIER = 1.25F;
    private static final float DEFAULT_AIM_YAW_MULTIPLIER = 0.8F;
    private static final float DEFAULT_HIP_PITCH_MULTIPLIER = 1.2F;
    private static final float DEFAULT_HIP_ROLL_MULTIPLIER = 2.75F;
    private static final float DEFAULT_AIM_ROLL_MULTIPLIER = 2.75F;

    private static final float DEFAULT_MAX_ROLL_HIP = 20f;
    private static final float DEFAULT_MAX_ROLL_AIM = 20f;

    private static float smoothedPitch = 0;
    private static float smoothedYaw = 0;
    private static float smoothedRoll = 0;
    private static float pitchVelocity = 0;
    private static float yawVelocity = 0;
    private static float rollVelocity = 0;

    private static float smoothedStrafeYaw = 0;
    private static float smoothedStrafeRoll = 0;
    private static float strafeYawVelocity = 0;
    private static float strafeRollVelocity = 0;

    private static float lastPitch = 0;
    private static float lastYaw = 0;
    private static long lastFrameTime = 0;

    @Inject(method = "renderFirstPerson", at = @At("HEAD"))
    private void applyCustomGunSway(LocalPlayer player, ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int light, float partialTick, CallbackInfo ci) {
        if (!TacZAdditionsConfig.CLIENT.enableGunMovement.get()) return;
        if (!(stack.getItem() instanceof AbstractGunItem)) return;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (lastFrameTime == 0) ? 0.016f : Math.min(0.05f, (currentTime - lastFrameTime) / 1000f);
        lastFrameTime = currentTime;

        float timeFactor = deltaTime * 60f;
        float currentPitch = player.getViewXRot(partialTick);
        float currentYaw = player.getViewYRot(partialTick);
        // Use raw per-frame delta — multiplying by timeFactor squared the deltaTime
        // dependence and amplified frame-time jitter into visible jagged motion.
        float deltaPitch = currentPitch - lastPitch;
        float deltaYaw = currentYaw - lastYaw;

        float aimingProgress = IClientPlayerGunOperator.fromLocalPlayer(player).getClientAimingProgress(partialTick);

        float hipYaw = get("hipfireYawMultiplier", DEFAULT_HIP_YAW_MULTIPLIER);
        float aimYaw = get("aimingYawMultiplier", DEFAULT_AIM_YAW_MULTIPLIER);
        float hipFireFactor = aimYaw + (1.0F - aimingProgress) * (hipYaw - aimYaw);

        float pitchMult = get("hipfirePitchMultiplier", DEFAULT_HIP_PITCH_MULTIPLIER);
        float hipFirePitchFactor = 1.0F + ((1.0F - aimingProgress) * (pitchMult - 1.0F));

        float hipRoll = get("hipfireRollFactor", DEFAULT_HIP_ROLL_MULTIPLIER);
        float aimRoll = get("aimingRollFactor", DEFAULT_AIM_ROLL_MULTIPLIER);
        float hipFireRollFactor = aimRoll + ((1.0F - aimingProgress) * (hipRoll - aimRoll));

        float drag = get("dragSmoothing", DEFAULT_DRAG_SMOOTHING);
        float decay = get("decayFactor", DEFAULT_DECAY_FACTOR);
        float momentum = get("momentumFactor", DEFAULT_MOMENTUM_FACTOR);
        float rollSens = get("rollSensitivity", DEFAULT_ROLL_SENSITIVITY);
        float maxRoll = get("maxTiltAngle", DEFAULT_MAX_ROLL_AIM + (DEFAULT_MAX_ROLL_HIP - DEFAULT_MAX_ROLL_AIM) * (1.0f - aimingProgress));

        // Velocity decay is per-time-unit (pow with timeFactor), not per-frame, so the
        // gun no longer "drifts longer" at low FPS. Position accumulation scales by
        // timeFactor so per-second motion is the same regardless of framerate.
        float velDecay = (float) Math.pow(0.85f, timeFactor);
        pitchVelocity = pitchVelocity * velDecay + deltaPitch * drag * hipFirePitchFactor;
        yawVelocity = yawVelocity * velDecay + deltaYaw * drag * hipFireFactor;
        rollVelocity = rollVelocity * velDecay + (-yawVelocity * 0.2f * hipFireRollFactor);

        smoothedPitch += pitchVelocity * momentum * timeFactor;
        smoothedYaw += yawVelocity * momentum * timeFactor;
        smoothedRoll += rollVelocity * momentum * timeFactor;

        smoothedPitch *= Math.pow(decay, timeFactor);
        smoothedYaw *= Math.pow(decay, timeFactor);
        smoothedRoll *= Math.pow(decay, timeFactor);

        float oscillation = 0.03f * (1.0f - aimingProgress) * timeFactor;
        smoothedPitch += Math.sin(currentTime * 0.003) * oscillation;
        smoothedYaw += Math.sin(currentTime * 0.002) * oscillation;

        float maxPitch = Mth.lerp(
                aimingProgress,
                TacZAdditionsConfig.CLIENT.maxHipPitch.get().floatValue(),
                TacZAdditionsConfig.CLIENT.maxAimPitch.get().floatValue()
        );

        float maxYaw = Mth.lerp(
                aimingProgress,
                TacZAdditionsConfig.CLIENT.maxHipYaw.get().floatValue(),
                TacZAdditionsConfig.CLIENT.maxAimYaw.get().floatValue()
        );

        smoothedPitch = clamp(smoothedPitch, -maxPitch, maxPitch);
        smoothedYaw = Mth.lerp(0.1f, smoothedYaw, clamp(smoothedYaw, -maxYaw, maxYaw));
        smoothedRoll = clamp(smoothedRoll, -maxRoll, maxRoll);

        poseStack.mulPose(Axis.XP.rotationDegrees(-smoothedPitch * DEFAULT_PITCH_SENSITIVITY));
        poseStack.mulPose(Axis.YP.rotationDegrees(smoothedYaw * DEFAULT_YAW_SENSITIVITY * hipFireFactor * 0.9f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(smoothedRoll * rollSens));

        float xOffset = smoothedYaw * 0.012f * hipFireFactor * 0.9f;
        float yOffset = -smoothedPitch * 0.012f * hipFirePitchFactor;
        poseStack.translate(xOffset, yOffset, 0);

        // --- Strafing movement (smooth like pitch/yaw) ---
        if (TacZAdditionsConfig.CLIENT.enableStrafeMovement.get()) {
            float strafeInput = Mth.clamp(player.xxa, -1f, 1f);

            float strafeYawFactor = lerp(aimingProgress,
                    TacZAdditionsConfig.CLIENT.aimStrafeYawMultiplier.get().floatValue(),
                    TacZAdditionsConfig.CLIENT.strafeYawMultiplier.get().floatValue());

            float strafeRollFactor = lerp(aimingProgress,
                    TacZAdditionsConfig.CLIENT.aimStrafeRollMultiplier.get().floatValue(),
                    TacZAdditionsConfig.CLIENT.strafeRollMultiplier.get().floatValue());

            float strafeTargetYaw = strafeInput * strafeYawFactor;
            float strafeTargetRoll = strafeInput * strafeRollFactor;

            float strafeDrag = get("strafeSmoothing", 0.15f);

            // Apply smoothing with separate strafe smoothing factor
            strafeYawVelocity = strafeYawVelocity * velDecay + (strafeTargetYaw - smoothedStrafeYaw) * strafeDrag * timeFactor;
            strafeRollVelocity = strafeRollVelocity * velDecay + (strafeTargetRoll - smoothedStrafeRoll) * strafeDrag * timeFactor;

            smoothedStrafeYaw += strafeYawVelocity * momentum * timeFactor;
            smoothedStrafeRoll += strafeRollVelocity * momentum * timeFactor;

            smoothedStrafeYaw *= Math.pow(decay, timeFactor);
            smoothedStrafeRoll *= Math.pow(decay, timeFactor);

            float maxStrafeYaw = TacZAdditionsConfig.CLIENT.maxStrafeYaw.get().floatValue();
            float maxStrafeRoll = TacZAdditionsConfig.CLIENT.maxStrafeRoll.get().floatValue();
            smoothedStrafeYaw = clamp(smoothedStrafeYaw, -maxStrafeYaw, maxStrafeYaw);
            smoothedStrafeRoll = clamp(smoothedStrafeRoll, -maxStrafeRoll, maxStrafeRoll);

            poseStack.mulPose(Axis.YP.rotationDegrees(smoothedStrafeYaw));
            poseStack.mulPose(Axis.ZP.rotationDegrees(smoothedStrafeRoll));
        }

        // --- Recoil translation ---
        float recoilProgress = 1.0f - (System.currentTimeMillis() - GunRecoilHandler.lastRecoilTime) / 300f;
        if (recoilProgress > 0f) {
            recoilProgress *= recoilProgress;
            poseStack.translate(
                    GunRecoilHandler.recoilX * recoilProgress * 0.01f,
                    GunRecoilHandler.recoilY * recoilProgress * 0.01f,
                    GunRecoilHandler.recoilZ * recoilProgress * 0.01f
            );
        }

        // --- Gun tuck ---
        if (TacZAdditionsConfig.CLIENT.enableGunTuck.get() && stack.getItem() instanceof AbstractGunItem) {
            float maxDist = TacZAdditionsConfig.CLIENT.gunTuckDistance.get().floatValue();
            float tuckTarget = GunTuckHandler.calculateTarget(player, partialTick, maxDist);
            // Always update — passes 0 on miss so it smoothly returns rather than snapping
            GunTuckHandler.update(tuckTarget, deltaTime);
        } else {
            // Still decay toward 0 when disabled or no gun, prevents snap if toggled
            GunTuckHandler.update(0f, deltaTime);
        }

        lastPitch = currentPitch;
        lastYaw = currentYaw;
    }

    private static float get(String key, float def) {
        try {
            return switch (key) {
                case "hipfireRollFactor" -> TacZAdditionsConfig.CLIENT.hipfireRollFactor.get().floatValue();
                case "aimingRollFactor" -> TacZAdditionsConfig.CLIENT.aimingRollFactor.get().floatValue();
                case "rollSensitivity" -> TacZAdditionsConfig.CLIENT.rollSensitivity.get().floatValue();
                case "maxTiltAngle" -> TacZAdditionsConfig.CLIENT.maxTiltAngle.get().floatValue();
                case "hipfireYawMultiplier" -> TacZAdditionsConfig.CLIENT.hipfireYawMultiplier.get().floatValue();
                case "aimingYawMultiplier" -> TacZAdditionsConfig.CLIENT.aimingYawMultiplier.get().floatValue();
                case "hipfirePitchMultiplier" -> TacZAdditionsConfig.CLIENT.hipfirePitchMultiplier.get().floatValue();
                case "dragSmoothing" -> TacZAdditionsConfig.CLIENT.dragSmoothing.get().floatValue();
                case "decayFactor" -> TacZAdditionsConfig.CLIENT.decayFactor.get().floatValue();
                case "momentumFactor" -> TacZAdditionsConfig.CLIENT.momentumFactor.get().floatValue();
                case "strafeSmoothing" -> TacZAdditionsConfig.CLIENT.strafeSmoothing.get().floatValue();
                default -> def;
            };
        } catch (Exception e) {
            return def;
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float lerp(float alpha, float from, float to) {
        return from + (to - from) * (1.0f - alpha);
    }

    @Inject(
            method = "lambda$renderFirstPerson$5",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/tacz/guns/client/event/FirstPersonRenderGunEvent;applyFirstPersonGunTransform(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/tacz/guns/client/model/BedrockGunModel;F)V",
                    shift = At.Shift.AFTER
            ),
            remap = false
    )
    private void applyRecoilKick(ItemStack stack, LocalPlayer player, float partialTick, PoseStack poseStack, ItemDisplayContext ctx, int light, GunDisplayInstance display, CallbackInfo ci) {
        // Recoil kick
        float kickAngle = TacZAdditionsConfig.CLIENT.recoilKickAngle.get().floatValue();
        if (kickAngle != 0f) {
            float recoilProgress = 1.0f - (System.currentTimeMillis() - GunRecoilHandler.lastRecoilTime) / 300f;
            if (recoilProgress > 0f) {
                recoilProgress *= recoilProgress;
                poseStack.mulPose(Axis.XP.rotationDegrees(-kickAngle * recoilProgress));
            }
        }

        // Gun tuck
        if (TacZAdditionsConfig.CLIENT.enableGunTuck.get() && GunTuckHandler.tuckProgress > 0f) {
            float maxAngle = TacZAdditionsConfig.CLIENT.gunTuckMaxAngle.get().floatValue();
            float maxTranslate = TacZAdditionsConfig.CLIENT.gunTuckMaxTranslate.get().floatValue();
            float t = GunTuckHandler.tuckProgress;

            float pivotShift = 0.5f; // shift pivot toward player, tune this
            poseStack.translate(0f, 0f, -pivotShift);
            poseStack.mulPose(Axis.XP.rotationDegrees(-maxAngle * t));
            poseStack.translate(0f, 0f, pivotShift);
            poseStack.translate(0f, 0f, maxTranslate * t);
        }
    }
}
