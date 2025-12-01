package com.raiiiden.taczadditions.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.raiiiden.taczadditions.util.AnimationState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.WeakHashMap;

@Mixin(value = BedrockGunModel.class, remap = false)
public class MagCheckAnimationMixin {

    private static final Map<ItemStack, AnimationState> animationStates = new WeakHashMap<>();
    private static final Map<ItemStack, Boolean> keyWasPressed = new WeakHashMap<>();

    // 3.5 seconds total: ~0.5s Down, ~0.5s Up/Left, ~2.0s Hold, ~0.5s Return
    private static final int ANIMATION_DURATION_MS = 3500;

    // Aliases based on your snippets and common conventions
    private static final String[] MAG_AND_HAND_ALIASES = {"mag_and_lefthand", "lefthand_and_mag", "mag_and_lh", "mag_and_bullet"};
    private static final String[] MAG_ALIASES = {"mag", "magazine", "mag_standard", "magazine_standard", "clip"};
    private static final String[] HAND_ALIASES = {"lefthand", "left_hand", "lh", "left_arm", "hand_left", "lefthand_pos"};

    @Inject(method = "render", at = @At("HEAD"))
    private void animateMagCheck(PoseStack poseStack, ItemStack gunItem, ItemDisplayContext context,
                                 RenderType renderType, int light, int overlay, CallbackInfo ci) {
        if (!TacZAdditionsConfig.CLIENT.magazineText.get()) return;

        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) return;

        BedrockGunModel model = (BedrockGunModel)(Object)this;
        AnimationState state = animationStates.computeIfAbsent(gunItem, k -> new AnimationState());

        // 1. FIND PARTS
        // Try to find a parent bone that moves both (best case)
        BedrockPart masterPart = findPart(model, MAG_AND_HAND_ALIASES);

        // Fallback: Find them separately
        BedrockPart magPart = (masterPart == null) ? findPart(model, MAG_ALIASES) : null;
        BedrockPart handPart = (masterPart == null) ? findPart(model, HAND_ALIASES) : null;

        if (masterPart == null && magPart == null) return;

        boolean jPressed = org.lwjgl.glfw.GLFW.glfwGetKey(
                Minecraft.getInstance().getWindow().getWindow(),
                org.lwjgl.glfw.GLFW.GLFW_KEY_J
        ) == org.lwjgl.glfw.GLFW.GLFW_PRESS;

        boolean wasPressed = keyWasPressed.getOrDefault(gunItem, false);

        if (jPressed && !wasPressed && !state.isAnimating) {
            state.isAnimating = true;
            state.startTime = System.currentTimeMillis();

            if (masterPart != null) {
                captureBaseline(state, masterPart, true);
            } else {
                if (magPart != null) captureBaseline(state, magPart, true);
                if (handPart != null) captureBaseline(state, handPart, false);
            }
        }

        keyWasPressed.put(gunItem, jPressed);

        if (state.isAnimating) {
            long elapsed = System.currentTimeMillis() - state.startTime;

            if (elapsed >= ANIMATION_DURATION_MS) {
                state.isAnimating = false;
                if (masterPart != null) resetTransform(masterPart, state.originalMagPos, state.originalMagRot);
                if (magPart != null) resetTransform(magPart, state.originalMagPos, state.originalMagRot);
                if (handPart != null) resetTransform(handPart, state.originalLeftHandPos, state.originalLeftHandRot);
            } else {
                // If we have a master part, animate just that
                if (masterPart != null) {
                    applyKeyframeAnimation(masterPart, state, elapsed, true);
                }
                // Otherwise animate both separately
                else {
                    if (magPart != null) applyKeyframeAnimation(magPart, state, elapsed, true);

                    if (handPart != null) {
                        // SPECIAL: Pass 'false' for isMag to apply "Hand Grab" logic
                        applyKeyframeAnimation(handPart, state, elapsed, false);
                    }
                }
            }
        }
    }

    private void captureBaseline(AnimationState state, BedrockPart part, boolean isMag) {
        if (isMag) {
            state.originalMagPos.set(part.offsetX, part.offsetY, part.offsetZ);
            state.originalMagRot.set(part.xRot, part.yRot, part.zRot);
        } else {
            state.originalLeftHandPos.set(part.offsetX, part.offsetY, part.offsetZ);
            state.originalLeftHandRot.set(part.xRot, part.yRot, part.zRot);
        }
    }

    private void applyKeyframeAnimation(BedrockPart part, AnimationState state, long elapsed, boolean isMag) {
        float progress = (float) elapsed / ANIMATION_DURATION_MS;
        float scale = 1.0f / 16.0f;

        Vector3f originPos = isMag ? state.originalMagPos : state.originalLeftHandPos;
        Vector3f originRot = isMag ? state.originalMagRot : state.originalLeftHandRot;

        // --- HAND GRAB LOGIC ---
        // If this is the hand, we want to pull it towards the magazine's Z-depth slightly
        // to ensure it looks like it's holding it, resolving the "Gap" issue.
        float grabOffsetX = 0;
        float grabOffsetY = 0;
        float grabOffsetZ = 0;

        if (!isMag) {
            // Calculate simple difference to bring hand closer to mag center
            // We interpolate this "Grab" during the first 10% of animation
            float grabProgress = Math.min(1.0f, progress / 0.1f);
            grabProgress = easeOutQuad(grabProgress);

            // Small manual offsets usually help align the hand bone to the mag bone
            // Adjust these if the hand clips into the mag
            grabOffsetZ = lerp(0, 0.5f * scale, grabProgress);
            grabOffsetY = lerp(0, -0.5f * scale, grabProgress);
        }

        // --- MOTION KEYFRAMES ---

        // A: STRAIGHT DOWN
        // Y is positive to go down. X/Z are 0 to stay straight.
        float posA_x = 0.0f;
        float posA_y = 6.0f * scale;
        float posA_z = 0.0f;

        // Rotations 0 to keep it straight
        float rotA_x = 0; float rotA_y = 0; float rotA_z = 0;


        // B: LEFT & UP (INSPECT VIEW)
        // Move Left (X+) and Up (Y- relative to the down position)
        float posB_x = 2.5f * scale;
        float posB_y = -1.5f * scale;  // Move UP higher than resting to inspect
        float posB_z = 0.0f;

        // Slight tilt to face player, NO left/right twist
        float rotB_x = (float) Math.toRadians(-15.0);
        float rotB_y = 0;
        float rotB_z = (float) Math.toRadians(5.0); // Very slight tilt for style

        // --- TIMING ---
        // 0-15%: Down
        // 15-30%: Left & Up
        // 30-85%: Hold
        // 85-100%: Return

        float currentX, currentY, currentZ;
        float currentRX, currentRY, currentRZ;

        if (progress < 0.15f) {
            // Phase 1: DOWN
            float t = easeOutQuad(progress / 0.15f);
            currentX = lerp(0, posA_x, t);
            currentY = lerp(0, posA_y, t);
            currentZ = lerp(0, posA_z, t);

            currentRX = lerp(0, rotA_x, t);
            currentRY = lerp(0, rotA_y, t);
            currentRZ = lerp(0, rotA_z, t);

        } else if (progress < 0.30f) {
            // Phase 2: LEFT & UP
            float t = easeInOutCubic((progress - 0.15f) / 0.15f);
            currentX = lerp(posA_x, posB_x, t);
            currentY = lerp(posA_y, posB_y, t);
            currentZ = lerp(posA_z, posB_z, t);

            currentRX = lerp(rotA_x, rotB_x, t);
            currentRY = lerp(rotA_y, rotB_y, t);
            currentRZ = lerp(rotA_z, rotB_z, t);

        } else if (progress < 0.85f) {
            // Phase 3: HOLD
            currentX = posB_x;
            currentY = posB_y;
            currentZ = posB_z;

            currentRX = rotB_x;
            currentRY = rotB_y;
            currentRZ = rotB_z;

        } else {
            // Phase 4: RETURN
            float t = easeInOutCubic((progress - 0.85f) / 0.15f);
            currentX = lerp(posB_x, 0, t);
            currentY = lerp(posB_y, 0, t);
            currentZ = lerp(posB_z, 0, t);

            currentRX = lerp(rotB_x, 0, t);
            currentRY = lerp(rotB_y, 0, t);
            currentRZ = lerp(rotB_z, 0, t);
        }

        // Apply Transform + Grab Offsets (if hand)
        part.offsetX = originPos.x + currentX + grabOffsetX;
        part.offsetY = originPos.y + currentY + grabOffsetY;
        part.offsetZ = originPos.z + currentZ + grabOffsetZ;

        part.xRot = originRot.x + currentRX;
        part.yRot = originRot.y + currentRY;
        part.zRot = originRot.z + currentRZ;
    }

    private BedrockPart findPart(BedrockGunModel model, String[] names) {
        BedrockPart root = model.getRootNode();
        for (String name : names) {
            BedrockPart part = searchPartRecursive(root, name);
            if (part != null) return part;
        }
        return null;
    }

    private BedrockPart searchPartRecursive(BedrockPart parent, String name) {
        if (parent == null) return null;
        if (name.equals(parent.name)) return parent;
        for (BedrockPart child : parent.children) {
            BedrockPart found = searchPartRecursive(child, name);
            if (found != null) return found;
        }
        return null;
    }

    private void resetTransform(BedrockPart part, Vector3f originPos, Vector3f originRot) {
        part.offsetX = originPos.x;
        part.offsetY = originPos.y;
        part.offsetZ = originPos.z;
        part.xRot = originRot.x;
        part.yRot = originRot.y;
        part.zRot = originRot.z;
    }

    private float lerp(float start, float end, float t) { return start + (end - start) * t; }
    private float easeOutQuad(float t) { return 1 - (1 - t) * (1 - t); }
    private float easeInOutCubic(float t) { return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2; }
}