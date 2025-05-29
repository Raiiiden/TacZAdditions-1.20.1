package com.raiiiden.taczadditions.mixin;

import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.item.ModernKineticGunItem;
import com.tacz.guns.resource.pojo.data.gun.BurstData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.api.TimelessAPI;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ModernKineticGunItem.class)
public class GunFireMixin {

    @Inject(method = "shoot", at = @At("TAIL"), remap = false)
    private void afterShoot(ShooterDataHolder dataHolder, ItemStack gunItem, Supplier<Float> pitch, Supplier<Float> yaw, LivingEntity shooter, CallbackInfo ci) {
        if (!(shooter instanceof ServerPlayer)) return;

        ModernKineticGunItem gun = (ModernKineticGunItem) (Object) this;
        FireMode fireMode = gun.getFireMode(gunItem);

        if (fireMode == FireMode.BURST) {
            GunData data = TimelessAPI.getCommonGunIndex(gun.getGunId(gunItem))
                    .map(index -> index.getGunData())
                    .orElse(null);

            if (data != null && data.getBurstData() != null) {
                BurstData burst = data.getBurstData();
                int count = burst.getCount();
                long delay = 60000L / burst.getBpm();
            }
        }
    }
}
