package com.raiiiden.taczadditions.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

 // Client-side store and renderer for laser dots belonging to <em>other</em> players.

public final class RemoteLaserDots {

     // Smoothing time constant in seconds. Smaller = snappier but can look jittery if packets jump;
     // larger = smoother but trails further behind the real aim. ~35 ms reaches the target in roughly
     // one-and-a-half network ticks
    private static final double SMOOTHING_TAU = 0.035;

    // Grace period after the last packet before a dot starts fading (covers brief packet hitches). */
    private static final long FADE_START_MS = 200L;
    // Time after the last packet at which a dot is fully gone and removed. */
    private static final long EXPIRE_MS = 350L;

    private static final float HALF_SIZE = 0.08F;

    private static final Map<Integer, Dot> DOTS = new ConcurrentHashMap<>();
    private static long lastFrameNanos = 0L;

    private RemoteLaserDots() {}

    private static final class Dot {
        double targetX, targetY, targetZ;
        double renderX, renderY, renderZ;
        int color;
        long lastUpdateMs;
        boolean initialized;
    }

    // Records the latest hit position received for the given player. Call on the client thread.
    public static void update(int entityId, double x, double y, double z, int color) {
        Dot dot = DOTS.computeIfAbsent(entityId, k -> new Dot());
        dot.targetX = x;
        dot.targetY = y;
        dot.targetZ = z;
        dot.color = color;
        dot.lastUpdateMs = System.currentTimeMillis();
        if (!dot.initialized) {
            // First sighting: snap the render position so it doesn't sweep in from a stale spot.
            dot.renderX = x;
            dot.renderY = y;
            dot.renderZ = z;
            dot.initialized = true;
        }
    }

    // Drops all tracked dots (e.g. on world unload).
    public static void clear() {
        DOTS.clear();
        lastFrameNanos = 0L;
    }

    public static void renderAll(PoseStack pose, MultiBufferSource buffers, Camera camera) {
        if (DOTS.isEmpty()) {
            lastFrameNanos = 0L;
            return;
        }

        long now = System.nanoTime();
        double dt = lastFrameNanos == 0L ? 0.0 : (now - lastFrameNanos) / 1.0e9;
        lastFrameNanos = now;
        double smoothing = dt <= 0.0 ? 1.0 : 1.0 - Math.exp(-dt / SMOOTHING_TAU);

        long nowMs = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, Dot>> it = DOTS.entrySet().iterator();
        while (it.hasNext()) {
            Dot dot = it.next().getValue();
            long age = nowMs - dot.lastUpdateMs;
            if (age > EXPIRE_MS) {
                it.remove();
                continue;
            }

            dot.renderX += (dot.targetX - dot.renderX) * smoothing;
            dot.renderY += (dot.targetY - dot.renderY) * smoothing;
            dot.renderZ += (dot.targetZ - dot.renderZ) * smoothing;

            float alpha = 1.0F;
            if (age > FADE_START_MS) {
                alpha = 1.0F - (age - FADE_START_MS) / (float) (EXPIRE_MS - FADE_START_MS);
            }

            LaserDotRenderer.renderDot(pose, buffers, camera,
                    dot.renderX, dot.renderY, dot.renderZ, dot.color, alpha, HALF_SIZE);
        }
    }
}