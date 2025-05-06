package com.raiiiden.taczadditions.mixin;

import com.mojang.math.Axis;
import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.tacz.guns.client.event.FirstPersonRenderGunEvent;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.util.math.PerlinNoise;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = FirstPersonRenderGunEvent.class, remap = false)
public class GunRecoilMultiplierMixin {

    private static final PerlinNoise RECOIL_NOISE_X = new PerlinNoise(-0.2F, 0.2F, 400L);
    private static final PerlinNoise RECOIL_NOISE_Y = new PerlinNoise(-0.0136F, 0.0136F, 100L);

    @Inject(method = "applyShootSwayAndRotation", at = @At("HEAD"), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    private static void modifyRecoilEffect(BedrockGunModel model, float aimingProgress, CallbackInfo ci) {
        BedrockPart rootNode = model.getRootNode();
        if (rootNode == null) return;

        float xMult = TacZAdditionsConfig.CLIENT.recoilVisualX.get().floatValue();
        float yMult = TacZAdditionsConfig.CLIENT.recoilVisualY.get().floatValue();
        float zMult = TacZAdditionsConfig.CLIENT.recoilVisualZ.get().floatValue();

        float progress = 1.0F - (float)(System.currentTimeMillis() - getShootTimestamp()) / 300.0F;
        if (progress < 0.0F) progress = 0.0F;

        progress = (float) com.tacz.guns.util.math.Easing.easeOutCubic(progress);
        float hipFactor = (1.0F - aimingProgress);

        rootNode.offsetX += RECOIL_NOISE_X.getValue() / 16.0F * progress * hipFactor * xMult;
        rootNode.offsetY += 0.00625F * progress * hipFactor * yMult;
        rootNode.offsetZ += 0.0025F * progress * hipFactor * zMult;
        rootNode.additionalQuaternion.mul(Axis.YP.rotation(RECOIL_NOISE_Y.getValue() * progress * xMult));

        ci.cancel();
    }

    private static long getShootTimestamp() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return -1;
        try {
            var field = FirstPersonRenderGunEvent.class.getDeclaredField("shootTimeStamp");
            field.setAccessible(true);
            return (long) field.get(null);
        } catch (Exception e) {
            return -1;
        }
    }
}
