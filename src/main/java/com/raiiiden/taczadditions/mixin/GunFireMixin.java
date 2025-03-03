package com.raiiiden.taczadditions.mixin;

import com.raiiiden.taczadditions.client.renderer.MuzzleFlashRenderer;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.item.ModernKineticGunItem;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.BurstData;
import com.tacz.guns.api.TimelessAPI;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Mixin(ModernKineticGunItem.class)
public class GunFireMixin {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Inject(method = "shoot", at = @At("HEAD"), remap = false)
    private void onGunFire(ShooterDataHolder dataHolder, ItemStack gunItem, Supplier<Float> pitch, Supplier<Float> yaw, LivingEntity shooter, CallbackInfo ci) {
        FireMode fireMode = ((ModernKineticGunItem) (Object) this).getFireMode(gunItem);

        System.out.println("[DEBUG] Gun fired in mode: " + fireMode.name());

        if (fireMode.name().equalsIgnoreCase("SINGLE") || fireMode.name().equalsIgnoreCase("SEMI")) {
            MuzzleFlashRenderer.triggerFlash();
            return;
        }
        if (fireMode.name().equalsIgnoreCase("AUTO")) {
            MuzzleFlashRenderer.triggerFlash();
            return;
        }
        if (fireMode.name().equalsIgnoreCase("BURST")) {
            triggerBurstMuzzleFlash(gunItem);
            return;
        }
    }

    private void triggerBurstMuzzleFlash(ItemStack gunItem) {
        int burstCount = 3; // Default burst shots
        int burstDelayMs = 100; // Default time between shots

        var gunId = ((ModernKineticGunItem) (Object) this).getGunId(gunItem);
        var gunIndex = TimelessAPI.getCommonGunIndex(gunId);

        if (gunIndex.isPresent()) {
            GunData gunData = gunIndex.get().getGunData();
            BurstData burstData = gunData.getBurstData();

            if (burstData != null) {
                burstCount = burstData.getCount(); // Get burst shot count
                burstDelayMs = (int) (60000.0 / burstData.getBpm()); // Calculate interval dynamically
                System.out.println("[DEBUG] Burst fire: " + burstCount + " shots, " + burstDelayMs + "ms delay.");
            }
        }

        for (int i = 0; i < burstCount; i++) {
            int delay = i * burstDelayMs;
            scheduler.schedule(() -> {
                System.out.println("[DEBUG] Burst shot muzzle flash!");
                MuzzleFlashRenderer.triggerFlash();
            }, delay, TimeUnit.MILLISECONDS);
        }
    }
}
