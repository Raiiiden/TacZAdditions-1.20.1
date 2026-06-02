package com.raiiiden.taczadditions.client;

public class GunTuckHandler {
    public static float tuckProgress = 0f;

    public static void update(float target, float deltaTime) {
        float timeFactor = deltaTime * 60f;
        // Simple lerp — no velocity so no snap when target suddenly drops to 0
        float speed = 0.12f * timeFactor;
        tuckProgress = tuckProgress + (target - tuckProgress) * Math.min(1f, speed);
        tuckProgress = Math.max(0f, Math.min(1f, tuckProgress));
    }
}