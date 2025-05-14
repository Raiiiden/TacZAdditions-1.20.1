package com.raiiiden.taczadditions.mixin;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.event.CameraSetupEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ViewportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CameraSetupEvent.class)
public class CameraSetupEventMixin {

    private static float swayTimer = 0f;

    @Inject(method = "applyLevelCameraAnimation", at = @At("TAIL"), remap = false)
    private static void injectScopeSway(ViewportEvent.ComputeCameraAngles event, CallbackInfo ci) {
        if (!TacZAdditionsConfig.CLIENT.enableScopeSway.get()) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack main = player.getMainHandItem();
        if (!(main.getItem() instanceof IGun iGun)) return;

        float zoom = iGun.getAimingZoom(main);
        float minZoom = TacZAdditionsConfig.CLIENT.scopeSwayMinZoom.get().floatValue();
        if (zoom < minZoom) return;

        IClientPlayerGunOperator op = IClientPlayerGunOperator.fromLocalPlayer(player);
        float aim = op.getClientAimingProgress(Minecraft.getInstance().getFrameTime());
        if (aim < 0.95f) return;

        float strength = TacZAdditionsConfig.CLIENT.scopeSwayStrength.get().floatValue();
        float speed = TacZAdditionsConfig.CLIENT.scopeSwaySpeed.get().floatValue();

        float delta = Minecraft.getInstance().getDeltaFrameTime();
        swayTimer += delta;

        float time = (swayTimer / speed) * (float) Math.PI * 2f;

        float pitch = (float) Math.sin(time * 0.7f) * strength;
        float yaw   = (float) Math.sin(time * 0.45f + 1.3f) * strength;

        player.setXRot(player.getXRot() + pitch);
        player.setYRot(player.getYRot() + yaw);
    }
}