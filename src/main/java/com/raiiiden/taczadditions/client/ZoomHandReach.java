package com.raiiiden.taczadditions.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.tacz.guns.client.model.BedrockAnimatedModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

// Reaches the off hand up to the scope while the magnification is being scrolled.
//
// Turning a magnification ring is a hand movement, so the arm that normally rests on the handguard
// travels up to the optic for as long as the player keeps adjusting and settles back afterwards.
// Purely visual: nothing here feeds back into the magnification, the hit scan or any packet.
//
// The travel is expressed as a translation in the lefthand_pos bone local space. Both bones are read
// off the same model hierarchy, so composing the two chains and inverting the hand one cancels every
// transform they share, including the first person placement and whatever the animations are doing.
public final class ZoomHandReach {

    private static final String HAND_BONE = "lefthand_pos";
    // scope_pos is the mount the optic sits on. The sight alignment nodes stand in for guns that
    // carry an integrated optic and therefore never authored a mount.
    private static final String[] TARGET_BONES = {"scope_pos", "idle_view", "iron_view"};

    // Bedrock authoring units are sixteenths of a block, which is what the config offsets are in.
    private static final float PIXEL = 1f / 16f;

    // A frame long enough to have been a pause rather than a frame is integrated as a single step,
    // so a stutter cannot fling the spring.
    private static final float MAX_STEP_SECONDS = 0.05f;

    private static float progress;
    private static float velocity;
    private static long lastFrameNanos;
    // Zero means never adjusted. A sentinel far in the past cannot be used here: subtracting it from
    // the current time overflows, the difference comes out negative, and the hand reads as held.
    private static long lastAdjustMillis;

    private ZoomHandReach() {
    }

    public static void reset() {
        progress = 0f;
        velocity = 0f;
        lastFrameNanos = 0L;
        lastAdjustMillis = 0L;
    }

    // Called whenever a scroll notch was consumed as a magnification change.
    public static void markAdjusting() {
        lastAdjustMillis = System.currentTimeMillis();
    }

    // Moves the pose from the off hand bone toward the optic, by however far the reach has settled.
    public static void applyReach(PoseStack poseStack, BedrockAnimatedModel model) {
        float reach = advance();
        if (reach <= 1.0e-4f || model == null) return;

        BedrockPart hand = model.getNode(HAND_BONE);
        BedrockPart target = findTarget(model);
        if (hand == null || target == null || hand == target) return;

        // Bedrock pivots are converted with the vertical axis flipped, see BedrockModel#convertPivot,
        // so this space runs Y downward while X and Z keep the authored orientation. Negating Y here
        // keeps the config reading as up is positive.
        Matrix4f handMatrix = chainMatrix(hand);
        Vector3f delta = chainMatrix(target).transformPosition(new Vector3f());
        delta.sub(handMatrix.transformPosition(new Vector3f()));

        delta.add(
                0f,
                -TacZAdditionsConfig.CLIENT.variableZoomHandReachOffsetY.get().floatValue() * PIXEL,
                TacZAdditionsConfig.CLIENT.variableZoomHandReachOffsetZ.get().floatValue() * PIXEL);

        // Sideways travel is set outright rather than added to, so the hand keeps its own line up the
        // gun no matter where a given model parked the two bones relative to each other.
        delta.x = TacZAdditionsConfig.CLIENT.variableZoomHandReachOffsetX.get().floatValue() * PIXEL;

        // The move was measured across the model, so it has to be expressed in the bone own frame
        // before the pose can carry it. Only the direction matters, never the bone own position.
        new Matrix4f(handMatrix).invert().transformDirection(delta);

        poseStack.translate(delta.x * reach, delta.y * reach, delta.z * reach);

        // Rotating after the move pivots the arm about the hand itself, so the twist straightens the
        // forearm in place instead of swinging the hand off the optic.
        applyTwist(poseStack, reach);
    }

    private static void applyTwist(PoseStack poseStack, float reach) {
        float pitch = TacZAdditionsConfig.CLIENT.variableZoomHandReachTwistX.get().floatValue() * reach;
        float yaw = TacZAdditionsConfig.CLIENT.variableZoomHandReachTwistY.get().floatValue() * reach;
        float roll = TacZAdditionsConfig.CLIENT.variableZoomHandReachTwistZ.get().floatValue() * reach;

        if (pitch != 0f) poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        if (yaw != 0f) poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        if (roll != 0f) poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
    }

    private static BedrockPart findTarget(BedrockAnimatedModel model) {
        for (String name : TARGET_BONES) {
            BedrockPart part = model.getNode(name);
            if (part != null) return part;
        }
        return null;
    }

    // The bone transform relative to the top of its own hierarchy, rebuilt every frame because the
    // animations write straight into the parts.
    private static Matrix4f chainMatrix(BedrockPart part) {
        List<BedrockPart> chain = new ArrayList<>();
        for (BedrockPart node = part; node != null; node = node.getParent()) {
            chain.add(node);
        }

        PoseStack scratch = new PoseStack();
        for (int i = chain.size() - 1; i >= 0; i--) {
            chain.get(i).translateAndRotateAndScale(scratch);
        }
        return new Matrix4f(scratch.last().pose());
    }

    // Damped spring toward held or released, integrated on real time so it looks the same at any
    // frame rate. Damping below one gives the small overshoot that reads as the hand arriving.
    private static float advance() {
        long now = System.nanoTime();
        float step = lastFrameNanos == 0L ? 0f : (now - lastFrameNanos) / 1.0e9f;
        lastFrameNanos = now;
        step = Mth.clamp(step, 0f, MAX_STEP_SECONDS);

        float goal = goal();
        if (step <= 0f) return progress;

        double frequency = TacZAdditionsConfig.CLIENT.variableZoomHandReachSpeed.get();
        double damping = TacZAdditionsConfig.CLIENT.variableZoomHandReachDamping.get();
        double stiffness = frequency * frequency;
        double drag = 2.0 * damping * frequency;

        velocity += (float) ((stiffness * (goal - progress) - drag * velocity) * step);
        progress += velocity * step;

        // The spring is allowed to overshoot on the way in but never to fold the arm past its rest.
        if (progress < 0f) {
            progress = 0f;
            velocity = 0f;
        }
        return progress;
    }

    private static float goal() {
        if (!TacZAdditionsConfig.CLIENT.variableZoomHandReach.get()) return 0f;
        if (!VariableZoomState.isSweeping()) return 0f;

        if (lastAdjustMillis == 0L) return 0f;

        long holdMillis = (long) (TacZAdditionsConfig.CLIENT.variableZoomHandReachHoldSeconds.get() * 1000.0);
        return System.currentTimeMillis() - lastAdjustMillis <= holdMillis ? 1f : 0f;
    }
}
