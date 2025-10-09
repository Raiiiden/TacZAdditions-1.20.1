package com.raiiiden.taczadditions.client;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.util.LaserColorUtil;
import com.raiiiden.taczadditions.ModParticles;
import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
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
            Vec3 lookVec = mc.player.getViewVector(partialTick);
            Vec3 endPos = eyePos.add(lookVec.scale(100.0));

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
                Vec3 normal = lookVec.normalize().scale(-0.01);
                // Pass color as the vx parameter (we reuse it since we don't need velocity)
                mc.level.addParticle(ModParticles.LASER_DOT.get(),
                        hitPos.x + normal.x,
                        hitPos.y + normal.y,
                        hitPos.z + normal.z,
                        laserColor, 0, 0); // Color passed here
            }
        }
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
            AABB entityBox = entity.getBoundingBox().inflate(0.3);
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