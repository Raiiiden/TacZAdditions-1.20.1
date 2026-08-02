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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
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
    private static final ConfiguredBlockSet PASS_THROUGH_BLOCKS =
            new ConfiguredBlockSet(() -> TacZAdditionsConfig.COMMON.laserPassThroughBlocks.get());
    private static final ConfiguredBlockSet BLOCKING_BLOCKS =
            new ConfiguredBlockSet(() -> TacZAdditionsConfig.COMMON.laserBlockingBlocks.get());

    private static final double BLOCK_SURFACE_OFFSET = 0.002;
    // Entity boxes sit outside the model, so push further out than a block face needs.
    private static final double ENTITY_SURFACE_OFFSET = 0.01;
    private static long lastSyncedGameTime = -1;

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            RemoteLaserDots.clear();
            LaserVisibilityCache.clear();
            MuzzleCache.clear();
            // Tag lookups are level-bound, so the resolved sets must not outlive the level.
            PASS_THROUGH_BLOCKS.clear();
            BLOCKING_BLOCKS.clear();
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
                // Capture shared model state immediately after rendering this entity.
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

            // Use TaCZ's animated barrel direction instead of the crosshair.
            Vec3 eyePos = mc.player.getEyePosition(partialTick);
            boolean correctAnimatedAngle = shouldUseAnimatedGunAngle(mc, partialTick);
            Vec3 barrelDir = getBarrelDirection(mc, partialTick, correctAnimatedAngle);
            if (!isFinite(barrelDir)) {
                barrelDir = mc.player.getViewVector(partialTick);
            }
            double maxDistance = TacZAdditionsConfig.COMMON.laserDotMaxDistance.get();
            Vec3 endPos = eyePos.add(barrelDir.scale(maxDistance));
            LaserHit hit = findHitPosition(mc.player, eyePos, endPos, partialTick);

            // Render the dot directly from the eye-origin ray resolved for this frame.
            if (hit != null) {
                Vec3 dotPos = renderHit(poseStack, buffers, camera, hit, barrelDir, laserColor);

                syncLaserDot(mc, dotPos, laserColor);
            }
        }

        // Derive NPC dots from synchronized rotation because NPCs cannot upload aim data.
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

    private static boolean shouldUseAnimatedGunAngle(Minecraft mc, float partialTick) {
        if (!mc.options.getCameraType().isFirstPerson()) return false;

        // The item-FOV projection applies to every hip-fire first-person pose, not just the
        // animations that hide the crosshair (TaCZ only sets that flag for inspect). While
        // aiming the gun is rendered under the world FOV, so no correction is wanted there.
        float aimingProgress = IClientPlayerGunOperator.fromLocalPlayer(mc.player)
                .getClientAimingProgress(partialTick);
        return aimingProgress <= 1.0e-3F;
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
        double maxDistance = TacZAdditionsConfig.COMMON.laserDotMaxDistance.get();

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

            // Prefer a remote player's uploaded dot over the approximate fallback.
            if (RemoteLaserDots.hasActiveDot(shooter.getId())) continue;

            Vec3 direction = shooter.getViewVector(partialTick);
            if (direction.lengthSqr() < 1.0e-12) continue;
            direction = direction.normalize();

            Vec3 eyePos = shooter.getEyePosition(partialTick);
            Vec3 endPos = eyePos.add(direction.scale(maxDistance));
            LaserHit hit = findHitPosition(shooter, eyePos, endPos, partialTick);
            if (hit == null) continue;

            renderHit(poseStack, buffers, camera, hit, direction, getLaserColor(gunStack));
        }
    }

    private static LaserHit findHitPosition(Entity shooter, Vec3 start, Vec3 end, float partialTick) {
        BlockHitResult blockHit = clipBlocks(shooter, start, end);
        if (blockHit.getType() != HitResult.Type.MISS && blockHit.isInside()) {
            blockHit = resolveInsideBlockHit(shooter, start, end, blockHit);
            // A ray that begins inside solid geometry must not continue through that geometry.
            if (blockHit == null) return null;
        }

        EntityRayHit entityHit = rayTraceEntities(shooter, start, end, partialTick,
                entity -> !entity.isSpectator() && entity.isPickable());

        if (entityHit != null && blockHit.getType() != HitResult.Type.MISS) {
            double blockDist = start.distanceToSqr(blockHit.getLocation());
            return entityHit.distanceSqr() < blockDist
                    ? LaserHit.fromEntity(entityHit)
                    : LaserHit.fromBlock(blockHit);
        }
        if (entityHit != null) return LaserHit.fromEntity(entityHit);
        if (blockHit.getType() != HitResult.Type.MISS) return LaserHit.fromBlock(blockHit);
        return null;
    }

    private static BlockHitResult clipBlocks(Entity shooter, Vec3 start, Vec3 end) {
        Level level = shooter.level();
        // OUTLINE keeps the dot on visible geometry, but Level#clip cannot skip individual blocks,
        // so the traversal is driven here to let configured blocks pass through.
        ClipContext context = new ClipContext(
                start,
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                shooter
        );

        return BlockGetter.traverseBlocks(start, end, context, (ctx, pos) -> {
            BlockState state = level.getBlockState(pos);
            if (isLaserPassThrough(level, pos, state)) return null;

            VoxelShape shape = ctx.getBlockShape(state, level, pos);
            return level.clipWithInteractionOverride(ctx.getFrom(), ctx.getTo(), pos, shape, state);
        }, ctx -> {
            Vec3 delta = ctx.getFrom().subtract(ctx.getTo());
            return BlockHitResult.miss(ctx.getTo(),
                    Direction.getNearest(delta.x, delta.y, delta.z),
                    BlockPos.containing(ctx.getTo()));
        });
    }

    private static boolean isLaserPassThrough(Level level, BlockPos pos, BlockState state) {
        if (state.isAir()) return true;
        // An explicit blocking entry wins over every pass-through rule below.
        if (BLOCKING_BLOCKS.matches(state)) return false;
        if (PASS_THROUGH_BLOCKS.matches(state)) return true;

        // Grass and flowers have a selection box far wider than their cross-shaped model, so the
        // dot would land in the empty air around the plant.
        return TacZAdditionsConfig.COMMON.laserPassThroughNonCollidingBlocks.get()
                && state.getCollisionShape(level, pos).isEmpty();
    }

    private static BlockHitResult resolveInsideBlockHit(Entity shooter, Vec3 start, Vec3 end,
                                                        BlockHitResult insideHit) {
        Vec3 ray = end.subtract(start);
        if (ray.lengthSqr() < 1.0e-12) return null;

        // Reverse-trace inside hits to recover the visible entry surface.
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
        Vec3 normal = hit.surfaceNormal();
        if (normal == null) {
            Vec3 fallbackPos = hit.location().add(rayDirection.scale(-0.01));
            LaserDotRenderer.renderDot(poseStack, buffers, camera,
                    fallbackPos.x, fallbackPos.y, fallbackPos.z, color, 1.0F, 0.08F);
            return fallbackPos;
        }

        double offset = hit.alignToSurface() ? BLOCK_SURFACE_OFFSET : ENTITY_SURFACE_OFFSET;
        Vec3 dotPos = hit.location().add(normal.scale(offset));

        // Entity boxes are a fiction, so their faces only supply the offset: a quad aligned to
        // one would turn edge-on at grazing angles. A real block face is the surface, so align there.
        if (hit.alignToSurface()) {
            LaserDotRenderer.renderSurfaceDot(poseStack, buffers, camera,
                    dotPos.x, dotPos.y, dotPos.z, normal, color, 1.0F, 0.08F);
        } else {
            LaserDotRenderer.renderDot(poseStack, buffers, camera,
                    dotPos.x, dotPos.y, dotPos.z, color, 1.0F, 0.08F);
        }
        return dotPos;
    }

    private record LaserHit(Vec3 location, Vec3 surfaceNormal, boolean alignToSurface) {
        private static LaserHit fromBlock(BlockHitResult hit) {
            return new LaserHit(hit.getLocation(),
                    Vec3.atLowerCornerOf(hit.getDirection().getNormal()), true);
        }

        private static LaserHit fromEntity(EntityRayHit hit) {
            return new LaserHit(hit.result().getLocation(), hit.faceNormal(), false);
        }
    }

    private record EntityRayHit(EntityHitResult result, double distanceSqr, Vec3 faceNormal) {}

    private static EntityRayHit rayTraceEntities(Entity shooter, Vec3 start, Vec3 end,
                                                  float partialTick, Predicate<Entity> filter) {
        Vec3 ray = end.subtract(start);
        AABB searchBox = shooter.getBoundingBox().expandTowards(ray).inflate(1.0);
        EntityRayHit closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : shooter.level().getEntities(shooter, searchBox, filter)) {
            if (entity.getRootVehicle() == shooter.getRootVehicle() && !entity.canRiderInteract()) continue;

            AABB entityBox = renderBoundingBox(entity, partialTick);

            // A box already containing the eye has no visible entry surface; let the ray through.
            if (entityBox.contains(start)) continue;

            Optional<Vec3> hitOpt = entityBox.clip(start, end);
            if (hitOpt.isEmpty()) continue;
            Vec3 hitVec = hitOpt.get();
            double distance = start.distanceToSqr(hitVec);

            if (distance < closestDist) {
                closestDist = distance;
                closest = new EntityRayHit(new EntityHitResult(entity, hitVec), distance,
                        boxFaceNormal(entityBox, hitVec));
            }
        }

        return closest;
    }

    private static AABB renderBoundingBox(Entity entity, float partialTick) {
        // getBoundingBox() tracks the tick position, but the model is drawn interpolated, so an
        // untouched box leads a moving entity by up to a full tick.
        AABB box = entity.getBoundingBox();
        if (partialTick >= 1.0F) return box;

        return box.move(
                Mth.lerp(partialTick, entity.xOld, entity.getX()) - entity.getX(),
                Mth.lerp(partialTick, entity.yOld, entity.getY()) - entity.getY(),
                Mth.lerp(partialTick, entity.zOld, entity.getZ()) - entity.getZ()
        );
    }

    private static Vec3 boxFaceNormal(AABB box, Vec3 point) {
        double best = Math.abs(point.x - box.minX);
        Vec3 normal = new Vec3(-1.0, 0.0, 0.0);

        double candidate = Math.abs(point.x - box.maxX);
        if (candidate < best) {
            best = candidate;
            normal = new Vec3(1.0, 0.0, 0.0);
        }
        candidate = Math.abs(point.y - box.minY);
        if (candidate < best) {
            best = candidate;
            normal = new Vec3(0.0, -1.0, 0.0);
        }
        candidate = Math.abs(point.y - box.maxY);
        if (candidate < best) {
            best = candidate;
            normal = new Vec3(0.0, 1.0, 0.0);
        }
        candidate = Math.abs(point.z - box.minZ);
        if (candidate < best) {
            best = candidate;
            normal = new Vec3(0.0, 0.0, -1.0);
        }
        candidate = Math.abs(point.z - box.maxZ);
        if (candidate < best) {
            normal = new Vec3(0.0, 0.0, 1.0);
        }
        return normal;
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
