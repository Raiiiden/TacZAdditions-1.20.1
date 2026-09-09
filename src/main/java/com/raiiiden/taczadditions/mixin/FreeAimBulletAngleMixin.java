package com.raiiiden.taczadditions.mixin;

import com.raiiiden.taczadditions.server.ServerFreeAimTracker;
import com.tacz.guns.entity.shooter.LivingEntityShoot;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Supplier;

// Sends the bullet along the barrel the player is looking at rather than along their crosshair,
// using the offset their client reported. Gun tuck adjusts the same two arguments alongside this.
@Mixin(value = LivingEntityShoot.class, remap = false)
public class FreeAimBulletAngleMixin {

    @Shadow
    @Final
    private LivingEntity shooter;

    @ModifyArg(
            method = "shoot(Ljava/util/function/Supplier;Ljava/util/function/Supplier;JFZ)Lcom/tacz/guns/api/entity/ShootResult;",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/tacz/guns/api/item/gun/AbstractGunItem;shoot(Lcom/tacz/guns/entity/shooter/ShooterDataHolder;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Supplier;Ljava/util/function/Supplier;Lnet/minecraft/world/entity/LivingEntity;)V"
            ),
            index = 2,
            remap = false
    )
    private Supplier<Float> taczadditions$applyFreeAimPitch(Supplier<Float> pitch) {
        LivingEntity entity = this.shooter;
        if (entity == null || entity.level().isClientSide()) return pitch;
        if (!ServerFreeAimTracker.isEnforced()) return pitch;

        // Resolved lazily, so the offset read is the one standing when the shot is actually taken.
        return () -> {
            Float supplied = pitch == null ? null : pitch.get();
            float basePitch = supplied == null ? entity.getXRot() : supplied;

            float offset = ServerFreeAimTracker.pitchOffset(entity);
            if (offset == 0f) return basePitch;

            // A positive offset means the muzzle sits high, and negative pitch aims upward.
            return Mth.clamp(basePitch - offset, -90f, 90f);
        };
    }

    @ModifyArg(
            method = "shoot(Ljava/util/function/Supplier;Ljava/util/function/Supplier;JFZ)Lcom/tacz/guns/api/entity/ShootResult;",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/tacz/guns/api/item/gun/AbstractGunItem;shoot(Lcom/tacz/guns/entity/shooter/ShooterDataHolder;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Supplier;Ljava/util/function/Supplier;Lnet/minecraft/world/entity/LivingEntity;)V"
            ),
            index = 3,
            remap = false
    )
    private Supplier<Float> taczadditions$applyFreeAimYaw(Supplier<Float> yaw) {
        LivingEntity entity = this.shooter;
        if (entity == null || entity.level().isClientSide()) return yaw;
        if (!ServerFreeAimTracker.isEnforced()) return yaw;

        return () -> {
            Float supplied = yaw == null ? null : yaw.get();
            float baseYaw = supplied == null ? entity.getYRot() : supplied;

            float offset = ServerFreeAimTracker.yawOffset(entity);
            if (offset == 0f) return baseYaw;

            // A positive offset swings the gun to the player's left, which is the smaller yaw.
            return Mth.wrapDegrees(baseYaw - offset);
        };
    }
}
