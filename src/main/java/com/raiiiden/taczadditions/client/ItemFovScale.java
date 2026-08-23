package com.raiiiden.taczadditions.client;

import com.tacz.guns.client.event.CameraSetupEvent;

// The held model is drawn under its own projection, not the world's, so an angle applied to the
// model does not land where the same angle in the world would. This is the factor between them.
public final class ItemFovScale {

    private ItemFovScale() {
    }

    // Turning the model by one world degree times this puts it where the world sits at one degree.
    public static double get() {
        try {
            double itemFov = CameraSetupEvent.ITEM_MODEL_FOV_DYNAMICS.get();
            double worldFov = CameraSetupEvent.WORLD_FOV_DYNAMICS.get();
            if (itemFov <= 0.0 || worldFov <= 0.0) return 1.0;
            return Math.tan(Math.toRadians(itemFov / 2.0)) / Math.tan(Math.toRadians(worldFov / 2.0));
        } catch (Exception ignored) {
            return 1.0;
        }
    }

    // Aiming already draws the model under the world FOV, so the correction is faded out with it.
    public static float forAiming(float aimingProgress) {
        double scale = get();
        return (float) (scale + (1.0 - scale) * aimingProgress);
    }
}
