package com.raiiiden.taczadditions.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class GunTuckHandler {
    public static float tuckProgress = 0f;

    public static void update(float target, float deltaTime) {
        float timeFactor = deltaTime * 60f;
        // Simple lerp -- no velocity, so a target change does not cause an overshoot.
        float speed = 0.12f * timeFactor;
        tuckProgress += (target - tuckProgress) * Math.min(1f, speed);
        tuckProgress = Math.max(0f, Math.min(1f, tuckProgress));
    }

    // Returns tuck strength for the nearest block or entity bounding box in front of the gun.
    public static float calculateTarget(LivingEntity shooter, float partialTick, float maxDistance) {
        Vec3 eye = shooter.getEyePosition(partialTick);
        Vec3 ray = shooter.getViewVector(partialTick).scale(maxDistance);
        Vec3 end = eye.add(ray);

        double closestDistance = maxDistance;
        boolean hitSomething = false;

        BlockHitResult blockHit = shooter.level().clip(new ClipContext(
                eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter
        ));
        if (blockHit.getType() != HitResult.Type.MISS) {
            closestDistance = eye.distanceTo(blockHit.getLocation());
            hitSomething = true;
        }

        AABB searchBox = shooter.getBoundingBox().expandTowards(ray).inflate(1.0);
        for (Entity entity : shooter.level().getEntities(shooter, searchBox,
                entity -> !entity.isSpectator()
                        && entity.isPickable()
                        && entity != shooter.getVehicle())) {
            AABB bounds = entity.getBoundingBox();
            double distance;

            if (bounds.contains(eye)) {
                distance = 0.0;
            } else {
                Optional<Vec3> hit = bounds.clip(eye, end);
                if (hit.isEmpty()) continue;
                distance = eye.distanceTo(hit.get());
            }

            if (distance < closestDistance) {
                closestDistance = distance;
                hitSomething = true;
            }
        }

        if (!hitSomething) return 0f;
        return Math.max(0f, Math.min(1f, 1.0f - (float) (closestDistance / maxDistance)));
    }
}
