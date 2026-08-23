package com.raiiiden.taczadditions.client;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.tacz.guns.api.item.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

import java.util.Random;

// Decouples the gun from the camera: the mouse aims the weapon inside a deadzone and only pushes
// the view once it reaches the edge. Nothing here springs, so nothing bounces.
public final class FreeAimHandler {

    private static final Random RANDOM = new Random();

    // Where the gun points relative to the crosshair, in degrees: positive yaw swings it left and
    // positive pitch raises it. Both the render and the reported bullet angle read these.
    private static float targetYaw;
    private static float targetPitch;

    // What is drawn and reported: the target run through a low pass, so the once-a-tick shoves
    // below arrive as a push rather than a step.
    private static float yawOffset;
    private static float pitchOffset;
    private static float rollOffset;

    private static float lastViewYaw;
    private static float lastViewPitch;
    private static boolean hasLastView;

    private static double lastMotionX;
    private static double lastMotionY;
    private static double lastMotionZ;
    private static boolean hasLastMotion;

    private static long lastHandledRecoil;
    private static long lastUpdateTime;

    // The scale the last frame ran with, so mouse input arriving before the next one uses the same
    // deadzone the gun is actually being drawn inside.
    private static float lastScale = 1f;

    private FreeAimHandler() {
    }

    // The server gate wins: a server that withholds free aim leaves every client locked to centre.
    public static boolean isEnabled() {
        return TacZAdditionsConfig.COMMON.enableFreeAim.get()
                && TacZAdditionsConfig.CLIENT.freeAimEnabled.get();
    }

    public static float yawOffset() {
        return yawOffset;
    }

    public static float pitchOffset() {
        return pitchOffset;
    }

    public static float rollOffset() {
        return rollOffset;
    }

    public static boolean hasOffset() {
        return yawOffset != 0f || pitchOffset != 0f || rollOffset != 0f;
    }

    public static void update(LocalPlayer player, float partialTick, float deltaTime, float aimingProgress) {
        // A stowed or unrendered gun leaves the tracked view behind, so start over rather than
        // swinging on the whole turn that happened while it was away.
        long now = System.currentTimeMillis();
        if (now - lastUpdateTime > 250L) reset();
        lastUpdateTime = now;

        float step = Math.min(deltaTime, 0.05f);

        float viewYaw = player.getViewYRot(partialTick);
        float viewPitch = player.getViewXRot(partialTick);
        if (!hasLastView) {
            lastViewYaw = viewYaw;
            lastViewPitch = viewPitch;
            hasLastView = true;
        }
        float deltaYaw = Mth.wrapDegrees(viewYaw - lastViewYaw);
        float deltaPitch = viewPitch - lastViewPitch;
        lastViewYaw = viewYaw;
        lastViewPitch = viewPitch;

        double motionX = player.getX() - player.xo;
        double motionY = player.getY() - player.yo;
        double motionZ = player.getZ() - player.zo;
        if (!hasLastMotion) {
            lastMotionX = motionX;
            lastMotionY = motionY;
            lastMotionZ = motionZ;
            hasLastMotion = true;
        }
        double accelX = motionX - lastMotionX;
        double accelY = motionY - lastMotionY;
        double accelZ = motionZ - lastMotionZ;
        lastMotionX = motionX;
        lastMotionY = motionY;
        lastMotionZ = motionZ;

        if (!isEnabled()) {
            settleToCentre(step);
            return;
        }

        TacZAdditionsConfig.Client config = TacZAdditionsConfig.CLIENT;
        float strength = config.freeAimStrength.get().floatValue();
        // Aiming either keeps the whole hipfire swing or fades it out as the sights come up, since
        // anything in between leaves the shot off the reticle.
        float aimed = config.freeAimAffectAiming.get() ? 1.0f : 1.0f - aimingProgress;
        float scale = aimed * strength;
        if (scale <= 0f) {
            lastScale = 0f;
            settleToCentre(step);
            return;
        }
        lastScale = scale;

        float lag = config.freeAimLag.get().floatValue();
        targetYaw += deltaYaw * lag * scale;
        targetPitch += deltaPitch * lag * scale;

        applyMovementShove(viewYaw, accelX, accelY, accelZ, scale);
        applyFireKick(scale);

        float deadYaw = config.freeAimDeadzoneYaw.get().floatValue() * scale;
        float deadPitch = config.freeAimDeadzonePitch.get().floatValue() * scale;
        float insideRate = config.freeAimDeadzoneReturn.get().floatValue();
        float edgeRate = config.freeAimEdgeReturn.get().floatValue();

        targetYaw = catchUp(targetYaw, deadYaw, insideRate, edgeRate, step);
        targetPitch = catchUp(targetPitch, deadPitch, insideRate, edgeRate, step);

        float maxYaw = Math.max(deadYaw, config.freeAimMaxYaw.get().floatValue() * scale);
        float maxPitch = Math.max(deadPitch, config.freeAimMaxPitch.get().floatValue() * scale);
        targetYaw = Mth.clamp(targetYaw, -maxYaw, maxYaw);
        targetPitch = Mth.clamp(targetPitch, -maxPitch, maxPitch);

        // A first-order chase, so the drawn offset never overshoots the one it is chasing.
        float follow = 1f - (float) Math.exp(-config.freeAimSmoothing.get().floatValue() * step);
        yawOffset += (targetYaw - yawOffset) * follow;
        pitchOffset += (targetPitch - pitchOffset) * follow;

        float maxRoll = config.freeAimMaxRoll.get().floatValue();
        float roll = -yawOffset * config.freeAimRollCoupling.get().floatValue();
        rollOffset = Mth.clamp(roll, -maxRoll, maxRoll);
    }

    // Takes the part of a sideways mouse movement that fits inside the deadzone and aims the gun
    // with it instead, handing back whatever is left for the camera to turn by.
    public static double absorbYaw(double degrees) {
        float absorb = absorbFraction();
        if (absorb <= 0f) return degrees;

        float limit = TacZAdditionsConfig.CLIENT.freeAimDeadzoneYaw.get().floatValue() * lastScale;
        // Mouse right is a larger yaw but a gun to the right is a smaller offset, so it is negated.
        float took = steer(targetYaw, (float) (-degrees * absorb), limit);
        targetYaw += took;
        return degrees + took;
    }

    // The vertical half of the same, with the deadzone measured against its own height.
    public static double absorbPitch(double degrees) {
        float absorb = absorbFraction();
        if (absorb <= 0f) return degrees;

        float limit = TacZAdditionsConfig.CLIENT.freeAimDeadzonePitch.get().floatValue() * lastScale;
        // Mouse down is a larger pitch but a lowered gun is a smaller offset, so this is negated too.
        float took = steer(targetPitch, (float) (-degrees * absorb), limit);
        targetPitch += took;
        return degrees + took;
    }

    // Only a rendered first person gun has somewhere to put the movement, so anything else takes
    // none of it and leaves the camera turning exactly as it always did.
    private static float absorbFraction() {
        if (!isEnabled() || lastScale <= 0f) return 0f;

        float absorb = TacZAdditionsConfig.CLIENT.freeAimCameraAbsorb.get().floatValue();
        if (absorb <= 0f) return 0f;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !minecraft.options.getCameraType().isFirstPerson()) return 0f;
        return IGun.mainHandHoldGun(player) ? absorb : 0f;
    }

    // How much of a request the gun can take without being pushed further out than the deadzone.
    // Anything already past the edge can still be steered back in, just not further out.
    private static float steer(float offset, float request, float limit) {
        float low = Math.min(-limit, offset);
        float high = Math.max(limit, offset);
        return Mth.clamp(offset + request, low, high) - offset;
    }

    // Speed changes shove the muzzle: setting off, stopping, strafing and landing all register here.
    private static void applyMovementShove(float viewYaw, double accelX, double accelY,
                                           double accelZ, float scale) {
        float moveInfluence = TacZAdditionsConfig.CLIENT.freeAimMoveInfluence.get().floatValue();
        float fallInfluence = TacZAdditionsConfig.CLIENT.freeAimFallInfluence.get().floatValue();
        if (moveInfluence <= 0f && fallInfluence <= 0f) return;

        if (moveInfluence > 0f) {
            // Split the horizontal shove into the player's own right and forward, so strafing yaws
            // the gun while starting and stopping pitches it.
            double radians = Math.toRadians(viewYaw);
            double sin = Math.sin(radians);
            double cos = Math.cos(radians);
            double sideways = accelX * cos + accelZ * sin;
            double forward = accelZ * cos - accelX * sin;
            targetYaw += (float) sideways * moveInfluence * scale;
            targetPitch -= (float) forward * moveInfluence * scale;
        }
        if (fallInfluence > 0f) {
            targetPitch -= (float) accelY * fallInfluence * scale;
        }
    }

    // Each shot throws the gun off centre and the catch-up walks it back, so a burst drifts.
    private static void applyFireKick(float scale) {
        if (GunRecoilHandler.lastRecoilTime == lastHandledRecoil) return;
        lastHandledRecoil = GunRecoilHandler.lastRecoilTime;

        float kick = TacZAdditionsConfig.CLIENT.freeAimFireKick.get().floatValue() * scale;
        if (kick <= 0f) return;

        float spread = TacZAdditionsConfig.CLIENT.freeAimFireKickSpread.get().floatValue();
        targetPitch += kick;
        targetYaw += (RANDOM.nextFloat() * 2f - 1f) * kick * spread;
    }

    // Reels the offset in without ever crossing centre. The part past the deadzone comes back at
    // its own rate, and inside it a rate of zero leaves the gun floating where it drifted.
    private static float catchUp(float offset, float deadzone, float insideRate, float edgeRate, float step) {
        float magnitude = Math.abs(offset);
        if (magnitude < 1.0e-4f) return 0f;

        float past = Math.max(0f, magnitude - deadzone);
        float inside = magnitude - past;
        past *= (float) Math.exp(-edgeRate * step);
        inside *= (float) Math.exp(-insideRate * step);
        return Math.signum(offset) * (past + inside);
    }

    // Eases back to centre when the feature is off, so toggling it never snaps the gun.
    private static void settleToCentre(float step) {
        if (!hasOffset() && targetYaw == 0f && targetPitch == 0f) return;

        float decay = (float) Math.exp(-8f * step);
        targetYaw *= decay;
        targetPitch *= decay;
        yawOffset *= decay;
        pitchOffset *= decay;
        rollOffset *= decay;

        if (Math.abs(yawOffset) < 0.001f && Math.abs(pitchOffset) < 0.001f) {
            reset();
        }
    }

    public static void reset() {
        targetYaw = 0f;
        targetPitch = 0f;
        yawOffset = 0f;
        pitchOffset = 0f;
        rollOffset = 0f;
        hasLastView = false;
        hasLastMotion = false;
        lastScale = 1f;
    }
}
