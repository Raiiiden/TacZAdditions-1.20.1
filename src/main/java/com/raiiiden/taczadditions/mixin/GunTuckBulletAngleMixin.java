package com.raiiiden.taczadditions.mixin;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.raiiiden.taczadditions.server.ServerGunTuckTracker;
import com.tacz.guns.entity.shooter.LivingEntityShoot;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Supplier;

// Adjusts every server-side shooter's bullet pitch to follow the tucked barrel.
@Mixin(value = LivingEntityShoot.class, remap = false)
public class GunTuckBulletAngleMixin {

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
    private Supplier<Float> taczadditions$applyTuckPitch(Supplier<Float> pitch) {
        LivingEntity entity = this.shooter;
        if (entity == null || entity.level().isClientSide()) return pitch;
        if (!TacZAdditionsConfig.COMMON.tuckAffectsBulletAngle.get()) return pitch;

        // Resolved lazily: TaCZ calls this once per shot, after the tuck value for this tick exists.
        return () -> {
            Float supplied = pitch == null ? null : pitch.get();
            float basePitch = supplied == null ? entity.getXRot() : supplied;

            float progress = ServerGunTuckTracker.getProgress(entity);
            if (progress <= 0f) return basePitch;

            float maxAngle = TacZAdditionsConfig.COMMON.tuckBulletMaxAngle.get().floatValue();
            // Negative pitch aims upward, matching the way the tuck lifts the muzzle.
            return Mth.clamp(basePitch - maxAngle * progress, -90f, 90f);
        };
    }
}
