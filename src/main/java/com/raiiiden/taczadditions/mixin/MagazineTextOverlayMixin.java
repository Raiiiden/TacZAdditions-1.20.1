package com.raiiiden.taczadditions.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(value = BedrockGunModel.class, remap = false)
public class MagazineTextOverlayMixin {

    private static final Map<ItemStack, List<Float>> recentDistances = new WeakHashMap<>();
    private static final Map<ItemStack, Float> idleDistances = new WeakHashMap<>();
    private static final Map<ItemStack, Boolean> activated = new WeakHashMap<>();
    private static final Map<ItemStack, Long> lastLogTime = new WeakHashMap<>();
    private static final Map<ItemStack, Float> maxDistances = new WeakHashMap<>();

    @Inject(method = "render", at = @At("TAIL"))
    private void injectText(PoseStack poseStack, ItemStack gunItem, ItemDisplayContext context, RenderType renderType, int light, int overlay, CallbackInfo ci) {
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) return;

        BedrockGunModel model = (BedrockGunModel)(Object)this;
        BedrockPart magPart = getPartByName(model.getRootNode(), "bullet_in_mag");
        if (magPart == null) {
            String[] fallbacks = { "mag_and_lefthand", "lefthand_and_mag", "mag_and_lh", "mag_extended_1", "mag_extended_2", "mag_extended_3", "mag_standard" };
            for (String name : fallbacks) {
                magPart = getPartByName(model.getRootNode(), name);
                if (magPart != null) break;
            }
        }

        BedrockPart basePart = getPartByName(model.getRootNode(), "gun_and_righthand");
        if (basePart == null) {
            String[] fallbacks = { "righthand_and_gun", "gun", "gun_body", "body", "mag_release", "magrelease", "righthand", "righthand_pos" };
            for (String name : fallbacks) {
                basePart = getPartByName(model.getRootNode(), name);
                if (basePart != null) break;
            }
        }

        long now = System.currentTimeMillis();
        BedrockPart root = model.getRootNode();
        if (now - lastLogTime.getOrDefault(gunItem, 0L) > 2000) {
            System.out.printf("Model %s part scan:%s%s%n",
                    model,
                    (magPart == null ? " missing magPart" : " using magPart: " + magPart.name),
                    (basePart == null ? " missing basePart" : " using basePart: " + basePart.name)
            );
            if (root != null) {
                //logAllPartPositions(root, "");
            } else {
                System.out.println("Root node is null.");
            }
            lastLogTime.put(gunItem, now);
        }

        if (magPart == null || basePart == null) return;

        int ammo = iGun.getCurrentAmmoCount(gunItem);
        if (ammo < 0) return;

        float currentDist = getDistanceBetweenParts(magPart, basePart);
        List<Float> history = recentDistances.computeIfAbsent(gunItem, k -> new ArrayList<>());
        Boolean isActivated = activated.getOrDefault(gunItem, false);

        if (!isActivated) {
            if (history.size() < 10) {
                history.add(currentDist);
                return;
            }
            float avg = 0f;
            for (float f : history) avg += f;
            avg /= history.size();
            idleDistances.put(gunItem, avg);
            maxDistances.put(gunItem, currentDist);
            if (Math.abs(currentDist - avg) > 0.3f) {
                activated.put(gunItem, true);
            } else return;
        }

        float idle = idleDistances.getOrDefault(gunItem, 1f);
        float max = maxDistances.getOrDefault(gunItem, idle + 0.01f);
        if (currentDist > max) {
            maxDistances.put(gunItem, currentDist);
            max = currentDist;
        }

        float percent = (max != idle) ? (currentDist - idle) / (max - idle) : 0f;
        percent = Math.max(0f, Math.min(1f, percent));

        if (percent < 0.05f) return;

        Font font = Minecraft.getInstance().font;
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        poseStack.pushPose();
        applyCompleteTransformChain(poseStack, magPart);
        poseStack.translate(0.0F, -0.05F, 0.1F);
        poseStack.scale(0.01115F, -0.01115F, 0.0115F);
        Vector3f normal = new Vector3f(0, 1, 0);
        Vector3f magDir = getPartDirection(magPart);
        Vector3f magDirNorm = new Vector3f(magDir).normalize();
        float angle = (float) Math.toDegrees(Math.acos(normal.dot(magDirNorm)));
        if (magDir.x < 0) angle = -angle;
        poseStack.mulPose(Axis.XP.rotationDegrees(-angle));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180));

        String ammoText = String.valueOf(ammo);
        float textWidth = font.width(ammoText) * 0.5F;

        font.drawInBatch(
                ammoText,
                -textWidth,
                -4.0F,
                0x00FF00,
                false,
                poseStack.last().pose(),
                bufferSource,
                Font.DisplayMode.NORMAL,
                0,
                light
        );

        poseStack.popPose();
        bufferSource.endBatch();
    }

    private float getDistanceBetweenParts(BedrockPart a, BedrockPart b) {
        Vector3f apos = getAnimatedPosition(a);
        Vector3f bpos = getAnimatedPosition(b);
        return apos.sub(bpos).length();
    }

    private Vector3f getAnimatedPosition(BedrockPart part) {
        PoseStack stack = new PoseStack();
        applyCompleteTransformChain(stack, part);
        Matrix4f mat = stack.last().pose();
        Vector3f out = new Vector3f();
        mat.getTranslation(out);
        return out;
    }

    private void applyCompleteTransformChain(PoseStack stack, BedrockPart part) {
        for (BedrockPart p : getTransformChain(part)) {
            p.translateAndRotateAndScale(stack);
        }
    }

    private List<BedrockPart> getTransformChain(BedrockPart part) {
        List<BedrockPart> chain = new ArrayList<>();
        while (part != null) {
            chain.add(part);
            part = part.getParent();
        }
        Collections.reverse(chain);
        return chain;
    }

    private Vector3f getPartDirection(BedrockPart part) {
        PoseStack stack = new PoseStack();
        applyCompleteTransformChain(stack, part);
        Matrix4f mat = stack.last().pose();
        Vector3f up = new Vector3f(0, 1, 0);
        mat.transformDirection(up);
        return up;
    }

    private BedrockPart getPartByName(BedrockPart root, String name) {
        if (root == null) return null;
        if (name.equals(root.name)) return root;
        for (BedrockPart child : root.children) {
            BedrockPart found = getPartByName(child, name);
            if (found != null) return found;
        }
        return null;
    }

//    private void logAllPartPositions(BedrockPart part, String indent) {
//        Vector3f pos = getAnimatedPosition(part);
//        System.out.printf("%sPart '%s' -> (%.4f, %.4f, %.4f)%n", indent, part.name, pos.x, pos.y, pos.z);
//        for (BedrockPart child : part.children) {
//            logAllPartPositions(child, indent + "  ");
//        }
//    }
}
