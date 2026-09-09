package com.raiiiden.taczadditions.client;

import com.raiiiden.taczadditions.util.GunTuckMath;
import net.minecraft.world.entity.LivingEntity;

public class GunTuckHandler {
    public static float tuckProgress = 0f;

    public static void update(float target, float deltaTime) {
        tuckProgress = GunTuckMath.advance(tuckProgress, target, deltaTime);
    }

    // Returns tuck strength for the nearest block or entity bounding box in front of the gun.
    public static float calculateTarget(LivingEntity shooter, float partialTick, float maxDistance) {
        return GunTuckMath.calculateTarget(shooter, partialTick, maxDistance);
    }
}