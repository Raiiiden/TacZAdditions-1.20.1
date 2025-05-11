package com.raiiiden.taczadditions.mixin;

import com.raiiiden.taczadditions.client.GunRecoilHandler;
import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.raiiiden.taczadditions.network.ModNetworking;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.item.ModernKineticGunItem;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.SilenceModifier;
import com.tacz.guns.resource.pojo.data.gun.BurstData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
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

    @Inject(method = "shoot", at = @At("TAIL"), remap = false)
    private void afterShoot(ShooterDataHolder dataHolder, ItemStack gunItem, Supplier<Float> pitch, Supplier<Float> yaw, LivingEntity shooter, CallbackInfo ci) {
        if (!(shooter instanceof ServerPlayer player)) return;

        ModernKineticGunItem gun = (ModernKineticGunItem) (Object) this;
        FireMode fireMode = gun.getFireMode(gunItem);
        boolean silenced = isGunSilenced(dataHolder);
        int lightLevel = silenced ? 6 : 15;

        Vec3 baseVec = shooter.getEyePosition().add(shooter.getLookAngle().normalize().scale(1.0));
        BlockPos basePos = BlockPos.containing(baseVec);

        float recoilX = TacZAdditionsConfig.CLIENT.recoilVisualX.get().floatValue();
        float recoilY = TacZAdditionsConfig.CLIENT.recoilVisualY.get().floatValue();
        float recoilZ = TacZAdditionsConfig.CLIENT.recoilVisualZ.get().floatValue();

        if (fireMode == FireMode.BURST) {
            GunData data = TimelessAPI.getCommonGunIndex(gun.getGunId(gunItem))
                    .map(index -> index.getGunData())
                    .orElse(null);

            if (data != null && data.getBurstData() != null) {
                BurstData burst = data.getBurstData();
                int count = burst.getCount();
                long delay = 60000L / burst.getBpm();

                for (int i = 0; i < count; i++) {
                    int shotNum = i;
                    scheduler.schedule(() -> {
                        if (TacZAdditionsConfig.SERVER.enableMuzzleFlash.get()) {
                            ModNetworking.sendMuzzleFlash(player, basePos, lightLevel);
                        }
                        GunRecoilHandler.trigger(recoilX, recoilY, recoilZ);
                    }, shotNum * delay, TimeUnit.MILLISECONDS);
                }
                return;
            }
        }

        if (TacZAdditionsConfig.SERVER.enableMuzzleFlash.get()) {
            ModNetworking.sendMuzzleFlash(player, basePos, lightLevel);
        }
        GunRecoilHandler.trigger(recoilX, recoilY, recoilZ);
    }

    private boolean isGunSilenced(ShooterDataHolder dataHolder) {
        if (dataHolder.currentGunItem == null || dataHolder.currentGunItem.get().isEmpty()) return false;

        AttachmentCacheProperty cache = dataHolder.cacheProperty;
        if (cache == null) return false;

        Object silenceData = cache.getCache(SilenceModifier.ID);
        if (silenceData instanceof it.unimi.dsi.fastutil.Pair<?, ?> pair) {
            Object right = pair.right();
            if (right instanceof Boolean b) return b;
        }
        return false;
    }
}
