package com.raiiiden.taczadditions.client;

import com.raiiiden.taczadditions.network.ModNetworking;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
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
    private static final double BLOCK_SURFACE_OFFSET = 0.002;
    private static long lastSyncedGameTime = -1;

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            RemoteLaserDots.clear();
            LaserVisibilityCache.clear();
            MuzzleCache.clear();
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
            Vec3 eyePos = mc.player.getEyePosition(partialTick);
            boolean correctAnimatedAngle = shouldUseAnimatedGunAngle(mc, gunStack, partialTick);
            Vec3 barrelDir = getBarrelDirection(mc, partialTick, correctAnimatedAngle);
            if (!isFinite(barrelDir)) {
                barrelDir = mc.player.getViewVector(partialTick);
            }
            double maxDistance = TacZAdditionsConfig.SERVER.laserDotMaxDistance.get();
            Vec3 endPos = eyePos.add(barrelDir.scale(maxDistance));
            LaserHit hit = findHitPosition(mc.player, eyePos, endPos);

            // Render the dot directly from the eye-origin ray resolved for this frame.
            if (hit != null) {
                Vec3 dotPos = renderHit(poseStack, buffers, camera, hit, barrelDir, laserColor);

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

    private static Vec3 getBarrelDirection(Minecraft mc, float partialTick,
                                           boolean correctItemFov) {
        Vector3f forward = MuzzleCache.muzzleForwardDirection;
        if (!isFinite(forward)
                || forward.x == 0.0F && forward.y == 0.0F && forward.z == 0.0F) {
            return mc.player.getViewVector(partialTick);
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        org.joml.Vector3f left = camera.getLeftVector();
        org.joml.Vector3f up = camera.getUpVector();
        org.joml.Vector3f look = camera.getLookVector();
        double forwardZ = forward.z * (correctItemFov ? getItemToWorldProjectionScale() : 1.0);

        // The model's beam points down local -Z. Transform that rotated axis into world space.
        double wx = left.x * forward.x - up.x * forward.y + look.x * forwardZ;
        double wy = left.y * forward.x - up.y * forward.y + look.y * forwardZ;
        double wz = left.z * forward.x - up.z * forward.y + look.z * forwardZ;
        double length = Math.sqrt(wx * wx + wy * wy + wz * wz);
        return length > 1.0e-6
                ? new Vec3(wx / length, wy / length, wz / length)
                : mc.player.getViewVector(partialTick);
    }

    private static double getItemToWorldProjectionScale() {
        try {
            double itemFov = com.tacz.guns.client.event.CameraSetupEvent.ITEM_MODEL_FOV_DYNAMICS.get();
            double worldFov = com.tacz.guns.client.event.CameraSetupEvent.WORLD_FOV_DYNAMICS.get();
            return Math.tan(Math.toRadians(itemFov / 2.0))
                    / Math.tan(Math.toRadians(worldFov / 2.0));
        } catch (Exception ignored) {
            return 1.0;
        }
    }

    private static boolean shouldUseAnimatedGunAngle(Minecraft mc, ItemStack gunStack,
                                                     float partialTick) {
        if (!mc.options.getCameraType().isFirstPerson()) return false;
        if (mc.player.isSprinting()) return true;

        return TimelessAPI.getGunDisplay(gunStack).map(display -> {
            var stateMachine = display.getAnimationStateMachine();
            if (stateMachine == null || stateMachine.getContext() == null
                    || !stateMachine.getContext().shouldHideCrossHair()) {
                return false;
            }

            float aimingProgress = IClientPlayerGunOperator.fromLocalPlayer(mc.player)
                    .getClientAimingProgress(partialTick);
            return aimingProgress <= 1.0e-3F;
        }).orElse(false);
    }

    private static boolean isFinite(Vector3f vector) {
        return vector != null && Float.isFinite(vector.x) && Float.isFinite(vector.y)
                && Float.isFinite(vector.z);
    }

    private static boolean isFinite(Vec3 vector) {
        return vector != null && Double.isFinite(vector.x) && Double.isFinite(vector.y)
                && Double.isFinite(vector.z);
    }

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
            LaserHit hit = findHitPosition(shooter, eyePos, endPos);
            if (hit == null) continue;

            renderHit(poseStack, buffers, camera, hit, direction, getLaserColor(gunStack));
        }
    }

    private static LaserHit findHitPosition(Entity shooter, Vec3 start, Vec3 end) {
        BlockHitResult blockHit = clipBlocks(shooter, start, end);
        if (blockHit.getType() != HitResult.Type.MISS && blockHit.isInside()) {
            blockHit = resolveInsideBlockHit(shooter, start, end, blockHit);
            // A ray that begins inside solid geometry must not continue through that geometry.
            if (blockHit == null) return null;
        }

        EntityRayHit entityHit = rayTraceEntities(shooter, start, end,
                entity -> !entity.isSpectator() && entity.isPickable());

        if (entityHit != null && blockHit.getType() != HitResult.Type.MISS) {
            double blockDist = start.distanceToSqr(blockHit.getLocation());
            return entityHit.distanceSqr() < blockDist
                    ? LaserHit.fromEntity(entityHit.result())
                    : LaserHit.fromBlock(blockHit);
        }
        if (entityHit != null) return LaserHit.fromEntity(entityHit.result());
        if (blockHit.getType() != HitResult.Type.MISS) return LaserHit.fromBlock(blockHit);
        return null;
    }

    private static BlockHitResult clipBlocks(Entity shooter, Vec3 start, Vec3 end) {
        // OUTLINE matches visible block geometry, preventing the laser from passing through visible
        // blocks that intentionally have no collision shape, such as signs and decorative blocks.
        return shooter.level().clip(new ClipContext(
                start,
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                shooter
        ));
    }

    private static BlockHitResult resolveInsideBlockHit(Entity shooter, Vec3 start, Vec3 end,
                                                        BlockHitResult insideHit) {
        Vec3 ray = end.subtract(start);
        if (ray.lengthSqr() < 1.0e-12) return null;

        // Minecraft reports an inside hit 0.1% along the entire ray. At a 100-block range that
        // places the result 0.1 blocks into the wall. Reverse-trace to recover the entry surface.
        BlockHitResult reverseHit = clipBlocks(shooter, start.subtract(ray), start);
        if (reverseHit.getType() == HitResult.Type.MISS
                || reverseHit.isInside()
                || !reverseHit.getBlockPos().equals(insideHit.getBlockPos())) {
            return null;
        }
        return reverseHit;
    }

    private static Vec3 renderHit(PoseStack poseStack, MultiBufferSource buffers, Camera camera,
                                  LaserHit hit, Vec3 rayDirection, int color) {
        if (hit.surfaceNormal() != null) {
            Vec3 dotPos = hit.location().add(hit.surfaceNormal().scale(BLOCK_SURFACE_OFFSET));
            LaserDotRenderer.renderSurfaceDot(poseStack, buffers, camera,
                    dotPos.x, dotPos.y, dotPos.z, hit.surfaceNormal(), color, 1.0F, 0.08F);
            return dotPos;
        }

        Vec3 dotPos = hit.location().add(rayDirection.scale(-0.01));
        LaserDotRenderer.renderDot(poseStack, buffers, camera,
                dotPos.x, dotPos.y, dotPos.z, color, 1.0F, 0.08F);
        return dotPos;
    }

    private record LaserHit(Vec3 location, Vec3 surfaceNormal) {
        private static LaserHit fromBlock(BlockHitResult hit) {
            return new LaserHit(hit.getLocation(), Vec3.atLowerCornerOf(hit.getDirection().getNormal()));
        }

        private static LaserHit fromEntity(EntityHitResult hit) {
            return new LaserHit(hit.getLocation(), null);
        }
    }

    private record EntityRayHit(EntityHitResult result, double distanceSqr) {}

    private static EntityRayHit rayTraceEntities(Entity shooter, Vec3 start, Vec3 end,
                                                  Predicate<Entity> filter) {
        Vec3 ray = end.subtract(start);
        AABB searchBox = shooter.getBoundingBox().expandTowards(ray).inflate(1.0);
        EntityRayHit closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : shooter.level().getEntities(shooter, searchBox, filter)) {
            if (entity.getRootVehicle() == shooter.getRootVehicle() && !entity.canRiderInteract()) continue;

            AABB entityBox = entity.getBoundingBox().inflate(entity.getPickRadius());
            Vec3 hitVec;
            double distance;

            if (entityBox.contains(start)) {
                // Treat an origin inside an entity as an immediate hit, but recover the entry
                // boundary so the rendered dot does not sit inside the entity's bounding box.
                Vec3 reverseRay = ray.normalize().scale(Math.max(ray.length(), 16.0));
                hitVec = entityBox.clip(start.subtract(reverseRay), start).orElse(start);
                distance = 0.0;
            } else {
                Optional<Vec3> hitOpt = entityBox.clip(start, end);
                if (hitOpt.isEmpty()) continue;
                hitVec = hitOpt.get();
                distance = start.distanceToSqr(hitVec);
            }

            if (distance < closestDist) {
                closestDist = distance;
                closest = new EntityRayHit(new EntityHitResult(entity, hitVec), distance);
            }
        }

        return closest;
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

    private static boolean isHoldingGunWithLaser(ItemStack gunStack) {
        return com.raiiiden.taczadditions.util.LaserToggleData.hasLaser(gunStack);
    }
}
