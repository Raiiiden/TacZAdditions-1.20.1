package com.raiiiden.taczadditions.client;

import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class LaserAttachmentTransformCache {
    public static final Vector3f position = new Vector3f();
    public static final Vector3f forwardDirection = new Vector3f();

    private static ItemStack renderedGun = ItemStack.EMPTY;
    private static boolean valid;

    private LaserAttachmentTransformCache() {}

    public static void capture(ItemStack gunStack, Matrix4f transform) {
        float projectionScale = 1.0F;
        try {
            double itemFov = com.tacz.guns.client.event.CameraSetupEvent.ITEM_MODEL_FOV_DYNAMICS.get();
            double worldFov = com.tacz.guns.client.event.CameraSetupEvent.WORLD_FOV_DYNAMICS.get();
            projectionScale = (float) (Math.tan(Math.toRadians(itemFov / 2.0))
                    / Math.tan(Math.toRadians(worldFov / 2.0)));
        } catch (Exception ignored) {}

        // First-person guns use a separate item FOV. Correct both points that define the ray:
        // applying this only to the origin produces an angular error whenever the gun is rotated.
        position.set(transform.m30(), transform.m31(), transform.m32() * projectionScale);
        forwardDirection.set(transform.m20(), transform.m21(), transform.m22() * projectionScale);
        renderedGun = gunStack.copy();
        valid = true;
    }

    public static boolean isValidFor(ItemStack gunStack) {
        return valid && ItemStack.isSameItemSameTags(renderedGun, gunStack);
    }

    public static void clear() {
        valid = false;
        renderedGun = ItemStack.EMPTY;
        position.set(0f, 0f, 0f);
        forwardDirection.set(0f, 0f, 0f);
    }
}
