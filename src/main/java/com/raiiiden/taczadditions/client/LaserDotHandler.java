package com.raiiiden.taczadditions.client;

import com.raiiiden.taczadditions.network.ModNetworking;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = "taczadditions", value = Dist.CLIENT)
public class LaserDotHandler {
    private static long lastSyncedGameTime = -1;

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            RemoteLaserDots.clear();
            LaserVisibilityCache.clear();
            LaserAttachmentTransformCache.clear();
        }
    }

    @SubscribeEvent
    public static void onLivingRendered(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        ItemStack gunStack = entity.getMainHandItem();
        if (!isHoldingGunWithLaser(gunStack)) {
            LaserVisibilityCache.removeEntity(entity.getId());
            return;
        }

        TimelessAPI.getGunDisplay(gunStack).ifPresent(display -> {
            if (display.getGunModel() != null) {
                // Capture immediately after this entity's held item was rendered. Gun models are
                // shared resources, so reading the bone later could return another entity's state.
                LaserVisibilityCache.captureEntity(entity.getId(), display.getGunModel());
            }
        });
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

        float partialTick = event.getPartialTick();
        ItemStack gunStack = mc.player.getMainHandItem();
        if (isHoldingGunWithLaser(gunStack) && LaserVisibilityCache.isLocalVisible(gunStack)) {
            // Get the laser color
            int laserColor = getLaserColor(gunStack);

            // Use the gun's actual barrel direction instead of the crosshair look vector.
            // TaCZ caches the muzzle position in camera space after all bone animations,
            // so this correctly follows inspect, reload, and sway animations.
            Vec3 barrelDir = getBarrelDirection(mc, partialTick);

            Vec3 eyePos = mc.player.getEyePosition(partialTick);
            // Normally the eye remains the collision origin so the laser agrees with the weapon's
            // point of impact. Inspect and sprint deliberately move the gun away from that firing
            // pose, so those states use the rendered laser attachment bone for the whole ray.
            Vec3 rayOrigin = eyePos;
            if (shouldFollowAttachmentOnly(mc, gunStack, partialTick)) {
                LaserRay attachmentRay = getLaserAttachmentRay(mc, gunStack);
                if (attachmentRay != null) {
                    rayOrigin = attachmentRay.origin();
                    barrelDir = attachmentRay.direction();
                }
            }
            Vec3 endPos = rayOrigin.add(barrelDir.scale(TacZAdditionsConfig.SERVER.laserDotMaxDistance.get()));

            // Check for block hit
            BlockHitResult blockHit = mc.level.clip(new ClipContext(
                    rayOrigin,
                    endPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    mc.player
            ));

            // Check for entity hit
            EntityHitResult entityHit = rayTraceEntities(mc.player, rayOrigin, endPos,
                    entity -> !entity.isSpectator() && entity.isPickable());

            // Determine which is closer
            Vec3 hitPos = null;
            if (entityHit != null && blockHit.getType() != HitResult.Type.MISS) {
                double entityDist = rayOrigin.distanceToSqr(entityHit.getLocation());
                double blockDist = rayOrigin.distanceToSqr(blockHit.getLocation());
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

        // NPCs do not have a client connection that can upload their dot like a player does, so
        // derive it from the already-synchronized entity rotation and held gun. This also acts as a
        // fallback for player-like NPC implementations; real remote players still use their more
        // accurate networked barrel direction below.
        renderEntityLaserDots(mc, poseStack, buffers, camera, partialTick);

        // Other players' dots (smoothed; their updates only arrive at the 20 Hz tick rate).
        RemoteLaserDots.renderAll(poseStack, buffers, camera);

        buffers.endBatch(LaserDotRenderer.LASER_DOT);
    }

    private static void syncLaserDot(Minecraft mc, Vec3 hitPos, int laserColor) {
        if (!ClientNetworkState.isServerModPresent()) return;

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

    private static LaserRay getLaserAttachmentRay(Minecraft mc, ItemStack gunStack) {
        if (!LaserAttachmentTransformCache.isValidFor(gunStack)) return null;

        Vector3f position = LaserAttachmentTransformCache.position;
        Vector3f forward = LaserAttachmentTransformCache.forwardDirection;
        if (forward.x == 0f && forward.y == 0f && forward.z == 0f) return null;

        Camera camera = mc.gameRenderer.getMainCamera();
        org.joml.Vector3f left = camera.getLeftVector();
        org.joml.Vector3f up = camera.getUpVector();
        org.joml.Vector3f look = camera.getLookVector();

        Vec3 origin = camera.getPosition().add(
                -left.x * position.x + up.x * position.y - look.x * position.z,
                -left.y * position.x + up.y * position.y - look.y * position.z,
                -left.z * position.x + up.z * position.y - look.z * position.z
        );

        double dx = left.x * forward.x - up.x * forward.y + look.x * forward.z;
        double dy = left.y * forward.x - up.y * forward.y + look.y * forward.z;
        double dz = left.z * forward.x - up.z * forward.y + look.z * forward.z;
        Vec3 direction = new Vec3(dx, dy, dz);
        if (direction.lengthSqr() < 1.0e-12) return null;
        return new LaserRay(origin, direction.normalize());
    }

    private static boolean shouldFollowAttachmentOnly(Minecraft mc, ItemStack gunStack, float partialTick) {
        if (!mc.options.getCameraType().isFirstPerson()) return false;
        if (mc.player.isSprinting()) return true;

        return TimelessAPI.getGunDisplay(gunStack).map(display -> {
            var stateMachine = display.getAnimationStateMachine();
            if (stateMachine == null || stateMachine.getContext() == null
                    || !stateMachine.getContext().shouldHideCrossHair()) {
                return false;
            }

            // Scoped aiming can also hide the crosshair; that should retain normal eye-origin aim.
            float aimingProgress = IClientPlayerGunOperator.fromLocalPlayer(mc.player)
                    .getClientAimingProgress(partialTick);
            return aimingProgress <= 1.0e-3F;
        }).orElse(false);
    }

    private record LaserRay(Vec3 origin, Vec3 direction) {}

    private static void renderEntityLaserDots(Minecraft mc, PoseStack poseStack,
                                               MultiBufferSource buffers, Camera camera,
                                               float partialTick) {
        double maxDistance = TacZAdditionsConfig.SERVER.laserDotMaxDistance.get();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity shooter)
                    || shooter == mc.player
                    || !shooter.isAlive()
                    || shooter.isSpectator()) {
                continue;
            }

            ItemStack gunStack = shooter.getMainHandItem();
            if (!isHoldingGunWithLaser(gunStack)) continue;
            if (!LaserVisibilityCache.isEntityVisible(shooter.getId(), gunStack)) continue;

            // A real remote player's uploaded dot includes first-person gun sway/recoil and is more
            // accurate than its entity rotation. Do not draw a second, approximate dot over it.
            if (RemoteLaserDots.hasActiveDot(shooter.getId())) continue;

            Vec3 direction = shooter.getViewVector(partialTick);
            if (direction.lengthSqr() < 1.0e-12) continue;
            direction = direction.normalize();

            Vec3 eyePos = shooter.getEyePosition(partialTick);
            Vec3 endPos = eyePos.add(direction.scale(maxDistance));
            Vec3 hitPos = findHitPosition(shooter, eyePos, endPos);
            if (hitPos == null) continue;

            Vec3 dotPos = hitPos.add(direction.scale(-0.01));
            LaserDotRenderer.renderDot(poseStack, buffers, camera,
                    dotPos.x, dotPos.y, dotPos.z, getLaserColor(gunStack), 1.0F, 0.08F);
        }
    }

    private static Vec3 findHitPosition(Entity shooter, Vec3 start, Vec3 end) {
        BlockHitResult blockHit = shooter.level().clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                shooter
        ));

        EntityHitResult entityHit = rayTraceEntities(shooter, start, end,
                entity -> !entity.isSpectator() && entity.isPickable());

        if (entityHit != null && blockHit.getType() != HitResult.Type.MISS) {
            double entityDist = start.distanceToSqr(entityHit.getLocation());
            double blockDist = start.distanceToSqr(blockHit.getLocation());
            return entityDist < blockDist ? entityHit.getLocation() : blockHit.getLocation();
        }
        if (entityHit != null) return entityHit.getLocation();
        if (blockHit.getType() != HitResult.Type.MISS) return blockHit.getLocation();
        return null;
    }

    private static int getLaserColor(ItemStack gunStack) {
        if (gunStack.getItem() instanceof IGun gun) {
            // Get color directly from the gun stack (which includes slider customization)
            if (gun.hasCustomLaserColor(gunStack)) {
                return gun.getLaserColor(gunStack);
            }

            // Otherwise try to get it from the laser attachment
            ItemStack laserAttachment = gun.getAttachment(gunStack, AttachmentType.LASER);
            if (laserAttachment.isEmpty()) {
                laserAttachment = gun.getBuiltinAttachment(gunStack, AttachmentType.LASER);
            }
            if (!laserAttachment.isEmpty()) {
                return LaserColorUtil.getLaserColor(laserAttachment);
            }
        }
        return 0xFF0000; // Default red if something goes wrong
    }

    private static EntityHitResult rayTraceEntities(Entity shooter, Vec3 start, Vec3 end, Predicate<Entity> filter) {
        Vec3 vec = end.subtract(start);
        AABB searchBox = shooter.getBoundingBox().expandTowards(vec).inflate(1.0);

        EntityHitResult closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : shooter.level().getEntities(shooter, searchBox, filter)) {
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

    private static boolean isHoldingGunWithLaser(ItemStack gunStack) {
        return com.raiiiden.taczadditions.util.LaserToggleData.hasLaser(gunStack);
    }
}
