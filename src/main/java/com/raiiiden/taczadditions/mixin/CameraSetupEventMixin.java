package com.raiiiden.taczadditions.mixin;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.raiiiden.taczadditions.util.CurrentGunStack;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.client.event.CameraSetupEvent;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.resource.pojo.data.gun.GunRecoil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ViewportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(CameraSetupEvent.class)
public class CameraSetupEventMixin {

    @Inject(method = "initialCameraRecoil", at = @At("HEAD"), remap = false)
    private static void setCurrentGunStack(GunFireEvent event, CallbackInfo ci) {
        ItemStack stack = event.getShooter().getMainHandItem();
        CurrentGunStack.set(stack);
    }

    @Inject(method = "initialCameraRecoil", at = @At("RETURN"), remap = false)
    private static void clearCurrentGunStack(GunFireEvent event, CallbackInfo ci) {
        CurrentGunStack.clear();
    }
}
