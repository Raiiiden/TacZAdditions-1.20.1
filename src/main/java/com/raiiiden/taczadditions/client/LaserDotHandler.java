package com.raiiiden.taczadditions.client;

import com.raiiiden.taczadditions.mixin.MuzzleDirectionMixin;
import com.raiiiden.taczadditions.network.ModNetworking;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import com.tacz.guns.util.LaserColorUtil;
import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = "taczadditions", value = Dist.CLIENT)
public class LaserDotHandler {
    private static long lastSyncedGameTime = -1;

    // High-pass filter on the camera-space muzzle position. The baseline tracks the muzzle's resting
    // pose; the (muzzle - baseline) deviation is the transient motion (sway/recoil/tuck) added to the
    // eye origin. Larger TAU = the dot holds its offset longer before settling back to the crosshair.
    private static final double MUZZLE_BASELINE_TAU = 0.30;
    // Multiplier on the muzzle deviation. 1.0 = move exactly with the muzzle; raise it to exaggerate.
    private static final float MUZZLE_MOVEMENT_SCALE = 1.0F;
    // The baseline only tracks the muzzle while the gun is within this many degrees of neutral (barrel
    // aligned with the look direction). Beyond it the baseline freezes so the full sway offset is kept
    // instead of decaying, which is what otherwise leaves the dot slightly short at the extremes.
    private static final double NEUTRAL_RESET_ANGLE = 2.0;
    // Floor on the tracking weight so the resting parallax still bleeds off (slowly) even if the gun
    // never quite returns to dead-neutral.
    private static final float MIN_BASELINE_WEIGHT = 0.05F;

    private static final Vector3f muzzleBaseline = new Vector3f();
    private static boolean muzzleBaselineInit = false;
    private static long lastMuzzleNanos = 0L;

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            RemoteLaserDots.clear();
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // Check if laser dots are enabled in config
        if (!TacZAdditionsConfig.CLIENT.enableLaserDot.get()) {
            return;
        }

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        ItemStack gunStack = mc.player.getMainHandItem();
        if (isHoldingGunWithLaser(mc.player, gunStack)) {
            // Get the laser color
            int laserColor = getLaserColor(gunStack);

            float partialTick = event.getPartialTick();

            // Use the gun's actual barrel direction instead of the crosshair look vector.
            // TaCZ caches the muzzle position in camera space after all bone animations,
            // so this correctly follows inspect, reload, and sway animations.
            Vec3 barrelDir = getBarrelDirection(mc, partialTick);

            // Anchor the ray at the eye (bullets originate there) and add the muzzle's transient
            // movement so the dot tracks the gun's sway/recoil/tuck without drifting off the
            // crosshair at rest. See getRayOrigin for details.
            Vec3 originPos = getRayOrigin(mc, partialTick, barrelDir);
            Vec3 endPos = originPos.add(barrelDir.scale(TacZAdditionsConfig.SERVER.laserDotMaxDistance.get()));

            // Check for block hit
            BlockHitResult blockHit = mc.level.clip(new ClipContext(
                    originPos,
                    endPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    mc.player
            ));

            // Check for entity hit
            EntityHitResult entityHit = rayTraceEntities(mc.player, originPos, endPos,
                    entity -> !entity.isSpectator() && entity.isPickable());

            // Determine which is closer
            Vec3 hitPos = null;
            if (entityHit != null && blockHit.getType() != HitResult.Type.MISS) {
                double entityDist = originPos.distanceToSqr(entityHit.getLocation());
                double blockDist = originPos.distanceToSqr(blockHit.getLocation());
                hitPos = entityDist < blockDist ? entityHit.getLocation() : blockHit.getLocation();
            } else if (entityHit != null) {
                hitPos = entityHit.getLocation();
            } else if (blockHit.getType() != HitResult.Type.MISS) {
                hitPos = blockHit.getLocation();
            }

            // Render the dot directly this frame. Because it is recomputed and redrawn from the same
            // partial tick, it tracks the muzzle with zero latency regardless of framerate.
            if (hitPos != null) {
                // Nudge slightly toward the shooter so the dot sits just in front of the surface
                // (avoids z-fighting while still being occluded by anything in between).
                Vec3 dotPos = hitPos.add(barrelDir.scale(-0.01));
                LaserDotRenderer.renderDot(poseStack, buffers, camera,
                        dotPos.x, dotPos.y, dotPos.z, laserColor, 1.0F, 0.08F);

                syncLaserDot(mc, dotPos, laserColor);
            }
        }

        // Other players' dots (smoothed; their updates only arrive at the 20 Hz tick rate).
        RemoteLaserDots.renderAll(poseStack, buffers, camera);

        buffers.endBatch(LaserDotRenderer.LASER_DOT);
    }

    private static void syncLaserDot(Minecraft mc, Vec3 hitPos, int laserColor) {
        long gameTime = mc.level.getGameTime();
        if (gameTime == lastSyncedGameTime) return;

        lastSyncedGameTime = gameTime;
        ModNetworking.sendLaserDot(hitPos, laserColor);
    }

    private static Vec3 getBarrelDirection(Minecraft mc, float partialTick) {
        try {
            Vector3f muzzle = GunItemRendererWrapper.muzzleRenderOffset;
            if (muzzle != null && (muzzle.x != 0f || muzzle.y != 0f || muzzle.z != 0f)) {
                Vector3f fwd = MuzzleCache.muzzleForwardDirection;

                if (fwd.x != 0f || fwd.y != 0f || fwd.z != 0f) {
                    Camera camera = mc.gameRenderer.getMainCamera();
                    org.joml.Vector3f left = camera.getLeftVector();
                    org.joml.Vector3f up   = camera.getUpVector();
                    org.joml.Vector3f look = camera.getLookVector();

                    double wx = -left.x * fwd.x + up.x * fwd.y - look.x * fwd.z;
                    double wy = -left.y * fwd.x + up.y * fwd.y - look.y * fwd.z;
                    double wz = -left.z * fwd.x + up.z * fwd.y - look.z * fwd.z;

                    double len = Math.sqrt(wx * wx + wy * wy + wz * wz);
                    if (len > 1e-6) {
                        return new Vec3(-wx / len, -wy / len, -wz / len);
                    }
                }
            }
        } catch (Exception ignored) {}

        return mc.player.getViewVector(partialTick);
    }

     // Laser ray origin. Anchored at the eye (where bullets actually originate, so the dot converges
     // on the crosshair when settled), then nudged by the muzzle's <em>movement</em>.
    private static Vec3 getRayOrigin(Minecraft mc, float partialTick, Vec3 barrelDir) {
        Vec3 eyePos = mc.player.getEyePosition(partialTick);
        try {
            Vector3f muzzle = GunItemRendererWrapper.muzzleRenderOffset;
            Vector3f fwd = MuzzleCache.muzzleForwardDirection;
            if ((muzzle.x != 0f || muzzle.y != 0f || muzzle.z != 0f)
                    && (fwd.x != 0f || fwd.y != 0f || fwd.z != 0f)) {
                long now = System.nanoTime();
                double dt = lastMuzzleNanos == 0L ? 0.0 : (now - lastMuzzleNanos) / 1.0e9;
                lastMuzzleNanos = now;

                Camera camera = mc.gameRenderer.getMainCamera();
                Vector3f left = camera.getLeftVector();
                Vector3f up   = camera.getUpVector();
                Vector3f look = camera.getLookVector();

                // How far the barrel is from neutral (aligned with the look direction). Near neutral we
                // let the baseline track the muzzle (so it settles onto the crosshair); once the gun is
                // meaningfully deviated we freeze it, preserving the full offset so the dot doesn't fall
                // short at the extremes.
                double align = barrelDir.x * look.x + barrelDir.y * look.y + barrelDir.z * look.z;
                double angleDeg = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, align))));
                float weight = (float) Math.max(MIN_BASELINE_WEIGHT,
                        Math.min(1.0, 1.0 - angleDeg / NEUTRAL_RESET_ANGLE));

                if (!muzzleBaselineInit) {
                    muzzleBaseline.set(muzzle);
                    muzzleBaselineInit = true;
                } else if (dt > 0.0) {
                    // Frame-rate independent low-pass toward the muzzle, gated by how neutral the gun is.
                    muzzleBaseline.lerp(muzzle, (float) (1.0 - Math.exp(-dt / MUZZLE_BASELINE_TAU)) * weight);
                }

                // Camera-space deviation of the muzzle from its resting pose.
                float dx = (muzzle.x - muzzleBaseline.x) * MUZZLE_MOVEMENT_SCALE;
                float dy = (muzzle.y - muzzleBaseline.y) * MUZZLE_MOVEMENT_SCALE;
                float dz = (muzzle.z - muzzleBaseline.z) * MUZZLE_MOVEMENT_SCALE;

                // Same camera-space -> world-space basis as getBarrelDirection (without the
                // barrel-axis negation, since this is a position offset, not a direction).
                double wx = -left.x * dx + up.x * dy - look.x * dz;
                double wy = -left.y * dx + up.y * dy - look.y * dz;
                double wz = -left.z * dx + up.z * dy - look.z * dz;

                return eyePos.add(wx, wy, wz);
            }
        } catch (Exception ignored) {}

        // No valid muzzle this frame: reset so we don't snap when it comes back.
        muzzleBaselineInit = false;
        lastMuzzleNanos = 0L;
        return eyePos;
    }

    private static int getLaserColor(ItemStack gunStack) {
        if (gunStack.getItem() instanceof IGun gun) {
            // Get color directly from the gun stack (which includes slider customization)
            if (gun.hasCustomLaserColor(gunStack)) {
                return gun.getLaserColor(gunStack);
            }

            // Otherwise try to get it from the laser attachment
            ItemStack laserAttachment = gun.getAttachment(gunStack, AttachmentType.LASER);
            if (!laserAttachment.isEmpty()) {
                return LaserColorUtil.getLaserColor(laserAttachment);
            }
        }
        return 0xFF0000; // Default red if something goes wrong
    }

    private static EntityHitResult rayTraceEntities(Player player, Vec3 start, Vec3 end, Predicate<Entity> filter) {
        Vec3 vec = end.subtract(start);
        AABB searchBox = player.getBoundingBox().expandTowards(vec).inflate(1.0);

        EntityHitResult closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : player.level().getEntities(player, searchBox, filter)) {
            AABB entityBox = entity.getBoundingBox().inflate(0.0);
            Optional<Vec3> hitOpt = entityBox.clip(start, end);

            if (hitOpt.isPresent()) {
                Vec3 hitVec = hitOpt.get();
                double dist = start.distanceToSqr(hitVec);
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = new EntityHitResult(entity, hitVec);
                }
            }
        }

        return closest;
    }

    private static boolean isHoldingGunWithLaser(Player player, ItemStack gunStack) {
        if (gunStack.getItem() instanceof IGun gun) {
            ItemStack laserAttachment = gun.getAttachment(gunStack, AttachmentType.LASER);
            return !laserAttachment.isEmpty();
        }
        return false;
    }
}
