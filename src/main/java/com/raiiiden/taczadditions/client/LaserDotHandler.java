package com.raiiiden.taczadditions.client;

import com.raiiiden.taczadditions.mixin.MuzzleDirectionMixin;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import com.tacz.guns.util.LaserColorUtil;
import com.raiiiden.taczadditions.ModParticles;
import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = "taczadditions", value = Dist.CLIENT)
public class LaserDotHandler {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // Check if laser dots are enabled in config
        if (!TacZAdditionsConfig.CLIENT.enableLaserDot.get()) {
            return;
        }

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        ItemStack gunStack = mc.player.getMainHandItem();
        if (isHoldingGunWithLaser(mc.player, gunStack)) {
            // Get the laser color
            int laserColor = getLaserColor(gunStack);

            float partialTick = event.getPartialTick();
            Vec3 eyePos = mc.player.getEyePosition(partialTick);

            // Use the gun's actual barrel direction instead of the crosshair look vector.
            // TaCZ caches the muzzle position in camera space after all bone animations,
            // so this correctly follows inspect, reload, and sway animations.
            Vec3 barrelDir = getBarrelDirection(mc, partialTick);
            Vec3 endPos = eyePos.add(barrelDir.scale(TacZAdditionsConfig.SERVER.laserDotMaxDistance.get()));

            // Check for block hit
            BlockHitResult blockHit = mc.level.clip(new ClipContext(
                    eyePos,
                    endPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    mc.player
            ));

            // Check for entity hit
            EntityHitResult entityHit = rayTraceEntities(mc.player, eyePos, endPos,
                    entity -> !entity.isSpectator() && entity.isPickable());

            // Determine which is closer
            Vec3 hitPos = null;
            if (entityHit != null && blockHit.getType() != HitResult.Type.MISS) {
                double entityDist = eyePos.distanceToSqr(entityHit.getLocation());
                double blockDist = eyePos.distanceToSqr(blockHit.getLocation());
                hitPos = entityDist < blockDist ? entityHit.getLocation() : blockHit.getLocation();
            } else if (entityHit != null) {
                hitPos = entityHit.getLocation();
            } else if (blockHit.getType() != HitResult.Type.MISS) {
                hitPos = blockHit.getLocation();
            }

            // Spawn particle with color
            if (hitPos != null) {
                Vec3 normal = barrelDir.scale(-0.01);
                // Pass color as the vx parameter (we reuse it since we don't need velocity)
                mc.level.addParticle(ModParticles.LASER_DOT.get(),
                        hitPos.x + normal.x,
                        hitPos.y + normal.y,
                        hitPos.z + normal.z,
                        laserColor, 0, 0); // Color passed here
            }
        }
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