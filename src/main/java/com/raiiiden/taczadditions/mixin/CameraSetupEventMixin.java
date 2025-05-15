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

import java.util.Random;

@Mixin(CameraSetupEvent.class)
public class CameraSetupEventMixin {

    private static float swayTimer = 0f;
    private static long crouchStartTime = 0;
    private static long crouchCooldownEnd = 0;

    private static final Random rand = new Random();

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

        float baseStrength = TacZAdditionsConfig.CLIENT.scopeSwayStrength.get().floatValue();
        float baseSpeed = TacZAdditionsConfig.CLIENT.scopeSwaySpeed.get().floatValue();

        float delta = Minecraft.getInstance().getDeltaFrameTime();
        swayTimer += delta;

        long now = System.currentTimeMillis();
        boolean crouching = player.isCrouching();

        float swayMult = 1f;
        float speedMult = 1f;

        long stabilizeMs = TacZAdditionsConfig.CLIENT.crouchStabilizeTime.get().longValue();
        long sporadicMs = TacZAdditionsConfig.CLIENT.crouchSporadicTime.get().longValue();
        long cooldownMs = TacZAdditionsConfig.CLIENT.crouchCooldownTime.get().longValue();

        double sporadicStrength = TacZAdditionsConfig.CLIENT.sporadicSwayStrength.get();
        double sporadicSpeed = TacZAdditionsConfig.CLIENT.sporadicSwaySpeed.get();

        if (crouching && now >= crouchCooldownEnd) {
            if (crouchStartTime == 0) {
                crouchStartTime = now;
            }

            long crouchDuration = now - crouchStartTime;

            if (crouchDuration <= stabilizeMs) {
                swayMult = 0.25f;
            } else if (crouchDuration <= stabilizeMs + sporadicMs) {
                swayMult = (float) sporadicStrength;
                speedMult = (float) sporadicSpeed;
            } else {
                crouchCooldownEnd = now + cooldownMs;
                crouchStartTime = 0;
            }
        } else {
            crouchStartTime = 0;
        }

        float time = (swayTimer / (baseSpeed * speedMult)) * (float) Math.PI * 2f;

        float pitch = (float) Math.sin(time * 0.7f) * baseStrength * swayMult;
        float yaw   = (float) Math.sin(time * 0.45f + 1.3f) * baseStrength * swayMult;

        player.setXRot(player.getXRot() + pitch);
        player.setYRot(player.getYRot() + yaw);
    }
}